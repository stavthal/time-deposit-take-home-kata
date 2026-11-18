# Assumptions

Each assumption the solution makes, why, and where it lives in the code.

| # | Assumption | Reason | Where in code |
|---|------------|--------|---------------|
| 1 | A plan type with no policy (unknown, or wrong case like `Basic`) earns zero interest. Its balance is still rewritten as `balance + 0.00`. | The original if/else chain matched no branch for these and left interest at 0. Behavior is frozen. | `TimeDepositCalculator.NO_INTEREST` |
| 2 | Withdrawal `amount` stays `BigDecimal` in the domain. Only `balance` is converted to `Double`. | No interest math touches withdrawals, so the exact DB value is kept and the frozen `TimeDeposit` is not involved. | `domain.Withdrawal` |
| 3 | A balance is stored as `BigDecimal.valueOf(d).setScale(2, HALF_UP)`. | Fits `DECIMAL(19,2)` and drops float noise like `1235595.8100000001`. The calculator's in memory result is unchanged. | `TimeDepositMapper.toDecimal` |
| 4 | The whole update, read, calculate and write, runs in one transaction. A decorator in `config` opens it. | A use case is one unit of work, and the application layer stays free of Spring. The same transaction holds the row locks from the read until the commit (#11). | `config.TransactionalUpdateAllBalancesUseCase` |
| 5 | Deposit and withdrawal ids are plain `INTEGER` keys, not generated. | No endpoint creates rows. Data comes from migrations. | `TimeDepositEntity.id` |
| 6 | Demo rows are loaded by a Flyway migration: one deposit per rule boundary, plus the README test data. | A reviewer can try both endpoints in Swagger at once. | `V2__demo_seed_data.sql` |
| 7 | The update endpoint is `POST /time-deposits/balance-updates` and returns `204`. | Each call adds one month of interest, so it is not idempotent and POST fits. The new balances are read with GET. How I would make it idempotent is below the table. | `TimeDepositController.updateAllBalances` |
| 8 | The list endpoint is `GET /time-deposits` and returns `withdrawals` as a list of `{id, amount, date}`. | Standard collection read. A list keeps every withdrawal field; a count or a sum would lose data. | `TimeDepositController.getAllTimeDeposits` |
| 9 | Swagger UI and the OpenAPI spec pages do not count as endpoints. No Actuator. | The README asks for a Swagger contract, so its pages are expected. Actuator would add routes nobody asked for. | `TimeDepositApiIT.handWrittenContractHasExactlyTheTwoEndpoints` |
| 10 | Updating balances does not advance `days`. | The frozen calculator never changes `days`, and the README does not say what moves a deposit to its next day. | `UpdateAllBalancesService.updateAllBalances` |
| 11 | Two balance updates running at once are serialized by a pessimistic row lock. The read takes `PESSIMISTIC_WRITE` (Postgres `FOR NO KEY UPDATE`), so the second update waits for the first to commit and then reads the new balances. Without it, two POSTs both read `1000.00` and both write `1000.83`, and one month of interest is lost. | A lost month is a money error. The lock is short (one read, one calculation, one write), rows lock in id order so two updates cannot deadlock, and a plain GET never waits. `ConcurrentBalanceUpdateIT` runs 8 updates at once and checks 8 months were applied. That tests the lock, not the business rule: paying a month once is the idempotency below. | `TimeDepositJpaRepository.findAllByOrderByIdAsc`, `config.TransactionalUpdateAllBalancesUseCase` |
| 12 | The API is contract first. `src/main/resources/openapi.yaml` is written by hand, and the server interface `TimeDepositsApi` and the `TimeDepositDto` and `WithdrawalDto` classes are generated from it at build time. Swagger UI loads that file at `/openapi.yaml`. | One source of truth for paths, status codes and JSON. The `Dto` suffix keeps the generated classes apart from the frozen `TimeDeposit`. `/v3/api-docs` stays on only because springdoc 2.9.1 turns Swagger UI off with it; a test checks it lists the same two endpoints. | `TimeDepositController`, `application.yml` (`springdoc.swagger-ui.url`) |

Locking options for the balance update:

- Pessimistic lock (chosen, commit `818638c`): `@Lock(LockModeType.PESSIMISTIC_WRITE)` on `TimeDepositJpaRepository.findAllByOrderByIdAsc`. The use case already runs in one transaction, so the rows stay locked from the read until the commit, and the second update waits and then reads the new balances.
- Optimistic lock (the alternative, when waits are not wanted): add a `version` column in a `V3` migration and a `@Version` field on `TimeDepositEntity`. The second writer then fails with `OptimisticLockException` instead of waiting, and the caller retries or gets `409`.

## Idempotency: the real fix for repeated runs (not built)

The lock and the idempotency answer different questions:

- The lock keeps the app correct when two updates run at the same moment. Each one still applies its month, so 8 POSTs give 8 months. `ConcurrentBalanceUpdateIT` tests exactly that: it proves the app does not lose or corrupt a month under load. It does not say 8 months for 8 calls is the wanted business result.
- In production, interest for a month must be paid once, however often the endpoint is called. A retry after a timeout, a scheduler firing twice, or two operators clicking must not pay a second month.

What I would build:

- An `interestRuns` table with the period (for example `2026-11`) as primary key, inserted in the same transaction as the balance update.
- The update takes the period, from the request or the scheduler. If the period is already recorded, it returns without touching any balance (`204` again, or `409`).
- The primary key makes the check safe under concurrency: the second insert for the same period fails, and its transaction rolls back.
- Optional: an `Idempotency-Key` header for client retries, stored with the response.

Why it is not built: the README asks for exactly two endpoints and does not define the period, and `TimeDeposit` has no "last paid" field. The demo also needs repeated POSTs to show interest growing.

## Rules read from the frozen code

These come from `docs/ai/analysis.md` §5. The frozen calculator already decides them,
so they have no `// Assumption:` comment. They are listed so a reader sees each choice.

| # | Assumption | Reason | Where in code |
|---|------------|--------|---------------|
| 13 | 1%, 3% and 5% are annual rates, paid monthly as rate / 12. | The code does `* 0.01 / 12`. Behavior is frozen. | `interest.*InterestPolicy` |
| 14 | No interest up to day 30. Day 31 is the first day with interest. | The code uses `getDays() > 30`. | `TimeDepositCalculator.updateBalance` |
| 15 | Premium earns from day 46. | The code uses `getDays() > 45`. | `PremiumInterestPolicy` |
| 16 | Student earns on days 31 to 365, none from day 366. | The code uses `getDays() < 366`. | `StudentInterestPolicy` |
| 17 | Withdrawals do not change interest or balance. `balance` is already net of them. | The calculator ignores them, and its behavior is frozen. | `UpdateAllBalancesService` |
| 18 | Table and column names stay camelCase, quoted in SQL. | The README gives exact names. Postgres folds unquoted names to lower case. | `V1__create_time_deposits_and_withdrawals.sql`, `application.yml` |
