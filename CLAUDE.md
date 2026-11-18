# CLAUDE.md

Project rules for AI agents working on this repo. Humans can read it too.
The task itself is in `README.md`.

## Start of every session

1. Read `docs/ai/analysis.md`.
2. Read `git log --oneline` to see where the work stands.
3. Read `ASSUMPTIONS.md` before making a new assumption.

## Scope

- Work only in `java/`.
- Never touch `c#/`, `kotlin/`, `python/`, `typescript/`.
- Never modify `README.md`. It is the original task text.

## Frozen contract (never change)

- `TimeDeposit` public API: the constructor
  `TimeDeposit(int id, String planType, Double balance, int days)`,
  `getId`, `getPlanType`, `getBalance`, `getDays`, `setBalance`.
  Adding members is allowed. Removing, renaming or retyping is not.
- `TimeDepositCalculator.updateBalance`: its signature
  `public void updateBalance(List<TimeDeposit> xs)`, its package `org.ikigaidigital`,
  and its results, to the cent, for every input.
  That includes the `double` math and the
  `new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)` rounding.

Current rules the results depend on:

| Plan      | Monthly rate | Condition in code  | Days with interest |
|-----------|--------------|--------------------|--------------------|
| all       |              | `getDays() > 30`   | from day 31        |
| `basic`   | `0.01 / 12`  | none extra         | 31 and later       |
| `student` | `0.03 / 12`  | `getDays() < 366`  | 31 to 365          |
| `premium` | `0.05 / 12`  | `getDays() > 45`   | 46 and later       |
| other     | 0            | no branch matches  | never              |

Plan names are lowercase and case sensitive.

## API

- Exactly two HTTP endpoints: update all balances, and get all time deposits.
- No Actuator. No extra controllers. No extra routes.
- Swagger UI and the OpenAPI spec pages are allowed.

## Architecture

- Hexagonal layout under `org.ikigaidigital`.
- The `domain` and `interest` packages import no Spring and no JPA.
- Adapters (REST, persistence) depend on the domain. Never the other way.
- Table and column names match the README exactly:
  `timeDeposits`, `withdrawals`, `planType`, `timeDepositId`.

## Tests

- Tests come before refactoring. Pin current behavior first.
- Every change keeps all tests green.
- No mocks in domain tests. Use real objects.
- Integration tests use Testcontainers Postgres.

## Commits

- Small steps. One logical change per commit.
- Conventional messages: `test:`, `refactor:`, `feat:`, `build:`, `docs:`, `chore:`.
- Never push a branch or open a PR on `upstream` (ikigai-digital). Work on `origin` (the fork).

## Code

- No abstraction without a clear reason. No dead code.
- When the README is unclear, pick a logical assumption.
  Write it as a code comment starting with `// Assumption:` and add it to `ASSUMPTIONS.md`.

## Commands

The build needs JDK 17. The default shell JDK on this machine may be 11.

```sh
sdk use java 17.0.0-tem
cd java && mvn test
```
