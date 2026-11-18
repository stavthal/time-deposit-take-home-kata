# Assumptions

Each assumption the solution makes, why, and where it lives in the code.

| # | Assumption | Reason | Where in code |
|---|------------|--------|---------------|
| 1 | A plan type with no policy (unknown, or wrong case like `Basic`) earns zero interest. Its balance is still rewritten as `balance + 0.00`. | The original if/else chain matched no branch for these and left interest at 0. Behavior is frozen. | `TimeDepositCalculator.NO_INTEREST` |
| 2 | Withdrawal `amount` stays `BigDecimal` in the domain. Only `balance` is converted to `Double`. | No interest math touches withdrawals, so the exact DB value is kept and the frozen `TimeDeposit` is not involved. | `domain.Withdrawal` |
| 3 | A balance is stored as `BigDecimal.valueOf(d).setScale(2, HALF_UP)`. | Fits `DECIMAL(19,2)` and drops float noise like `1235595.8100000001`. The calculator's in memory result is unchanged. | `TimeDepositMapper.toDecimal` |
| 4 | The whole update, read, calculate and write, runs in one transaction. A decorator in `config` opens it. | A use case is one unit of work, and the application layer stays free of Spring. Two updates running at once are out of scope for a monthly batch. | `config.TransactionalUpdateAllBalancesUseCase` |
| 5 | Deposit and withdrawal ids are plain `INTEGER` keys, not generated. | No endpoint creates rows. Data comes from migrations. | `TimeDepositEntity.id` |
| 6 | Demo rows are loaded by a Flyway migration: one deposit per rule boundary, plus the README test data. | A reviewer can try both endpoints in Swagger at once. | `V2__demo_seed_data.sql` |
| 7 | The update endpoint is `POST /time-deposits/balance-updates` and returns `204`. | Each call adds one month of interest, so it is not idempotent and POST fits. The new balances are read with GET. | `TimeDepositController.updateAllBalances` |
| 8 | The list endpoint is `GET /time-deposits` and returns `withdrawals` as a list of `{id, amount, date}`. | Standard collection read. A list keeps every withdrawal field; a count or a sum would lose data. | `TimeDepositController.getAllTimeDeposits` |
| 9 | Swagger UI and the OpenAPI spec pages do not count as endpoints. No Actuator. | The README asks for a Swagger contract, so its pages are expected. Actuator would add routes nobody asked for. | `TimeDepositApiIT.openApiContractHasExactlyTheTwoEndpoints` |
| 10 | Updating balances does not advance `days`. | The frozen calculator never changes `days`, and the README does not say what moves a deposit to its next day. | `UpdateAllBalancesService.updateAllBalances` |
| 11 | Two balance updates running at once are not locked against each other. One transaction stops a half saved state, but not a lost update: two POSTs both read `1000.00` and both write `1000.83`, so one month of interest is lost. | A monthly batch runs once, so a second caller at the same moment is out of scope. A lock would make the read wait, for no gain today. | `config.TransactionalUpdateAllBalancesUseCase` |

How to lock the rows if two updates at once must be safe:

- Pessimistic lock (preferred for a batch): put `@Lock(LockModeType.PESSIMISTIC_WRITE)` on `TimeDepositJpaRepository.findAllByOrderByIdAsc`. Hibernate then reads with `SELECT ... FOR UPDATE`. The use case already runs in one transaction, so the rows stay locked from the read until the commit, and the second update waits and then reads the new balances. The lock is short: one read, one calculation in memory, one write.
- Optimistic lock (when waits are not wanted): add a `version` column in a `V3` migration and a `@Version` field on `TimeDepositEntity`. The second writer then fails with `OptimisticLockException` instead of waiting, and the caller retries or gets `409`.
