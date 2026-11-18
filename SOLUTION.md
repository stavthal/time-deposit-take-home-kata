# Solution

## What this is

A Spring Boot 3.5 service in `java/` with a hexagonal layout and exactly two endpoints: update all balances, and list all time deposits.
The original calculator was pinned by tests first, then refactored to one interest policy per plan, with identical results to the cent.

The task text is the root `README.md`, left untouched.

## How to run

Prerequisites:

- Java 17 or newer (the build targets 17, verified on 17)
- Docker running (Testcontainers starts Postgres)
- Maven

```sh
sdk use java 17.0.0-tem   # only if your default JDK is older than 17
cd java
mvn spring-boot:test-run
```

This starts Postgres in Docker, applies the Flyway migrations (schema plus demo data) and serves the app on http://localhost:8080.
To use your own Postgres instead, set `DB_URL`, `DB_USER`, `DB_PASSWORD` and run `mvn spring-boot:run`.
Colima users: see the two `export` lines in [`java/README.md`](java/README.md#run-the-app).

## Trigger the endpoints with Swagger UI

Open http://localhost:8080/docs. It redirects to Swagger UI, which shows the hand written contract `openapi.yaml`.

1. **GET /time-deposits**: click it, then **Try it out**, then **Execute**.
   The response is `200` with every deposit (`id`, `planType`, `balance`, `days`, `withdrawals`).
2. **POST /time-deposits/balance-updates**: click it, then **Try it out**, then **Execute**.
   The response is `204` with no body. Every deposit got one month of interest.
3. **GET /time-deposits** again: **Execute** shows the new balances.

Worked example, demo deposit id 6 (`premium`, day 46, `1000.00`):
`1000.00 * 0.05 / 12 = 4.1666...`, rounded `HALF_UP` to `4.17`, so the balance becomes `1004.17`.

Result of one POST on the demo data (checked on a real run):

| id | plan | days | before | after |
|----|------|------|--------|-------|
| 1 | basic | 30 | 1000.00 | 1000.00 |
| 2 | basic | 31 | 1000.00 | 1000.83 |
| 3 | student | 365 | 1000.00 | 1002.50 |
| 4 | student | 366 | 1000.00 | 1000.00 |
| 5 | premium | 45 | 1000.00 | 1000.00 |
| 6 | premium | 46 | 1000.00 | 1004.17 |
| 7 | basic | 45 | 1234567.00 | 1235595.81 |

Each POST adds one more month. Restart the app to reset the data.

Same calls with curl:

```sh
curl http://localhost:8080/time-deposits
curl -X POST http://localhost:8080/time-deposits/balance-updates
```

## How to run the tests

All from `java/`.

| What | Command | Report |
|------|---------|--------|
| Unit, web slice, architecture (no Docker) | `mvn test` | `target/surefire-reports/` |
| Plus Testcontainers integration tests | `mvn verify` | `target/failsafe-reports/` |
| JaCoCo coverage (runs with `mvn test`) | `mvn test` | `target/site/jacoco/index.html` |
| PIT mutation testing on the core | `mvn org.pitest:pitest-maven:mutationCoverage` | `target/pit-reports/index.html` |

Last run on JDK 17: `mvn clean verify` gave 44 unit and 11 integration tests green.
PIT killed 29 of 29 mutants, with 100% line coverage of the mutated classes.

## Architecture

Hexagonal, under `org.ikigaidigital`. Dependencies point inward.

```
adapter.in.web  ->  application.port.in  ->  application.service  ->  TimeDepositCalculator + interest
                                                    |
adapter.out.persistence  ->  implements  application.port.out
```

- Core: root package (frozen `TimeDeposit`, `TimeDepositCalculator`), `interest`, `domain`, `application`. No Spring, JPA or Hibernate.
- Adapters: `adapter.in.web` (controller implements the generated `TimeDepositsApi`), `adapter.out.persistence` (JPA, Flyway, Postgres).
- `config` wires the use cases and opens the transaction. `app` holds the startup class.
- `ArchitectureTest` (ArchUnit) fails the build if these rules break.

Diagrams and the package table: [`java/README.md`](java/README.md#architecture).

## Analysis: issues in the original code

Full read only analysis: [`docs/ai/analysis.md`](docs/ai/analysis.md).

| Issue | How it was handled |
|-------|--------------------|
| The only test was `assertThat(1).isEqualTo(1);` | 32 characterization tests pin real JDK 17 output, then PIT proves they catch changes |
| Boundaries `> 30`, `> 45`, `< 366` easy to break | One test row per boundary day (30/31, 45/46, 365/366) |
| `new BigDecimal(double)` rounding and `double` money | Kept exactly, because results are frozen. Extracted into `roundToCents` with tests |
| All plan rules in one if/else chain | One `InterestPolicy` per plan, looked up by plan name |
| Unknown or wrong case plan gets zero interest silently | Kept as is and documented ([#1](ASSUMPTIONS.md)) |
| `TimeDeposit` has no withdrawals, GET needs them | `TimeDeposit` untouched. Separate read model `TimeDepositDetails` carries them |
| `Double` in the model, decimal in the DB | `TimeDepositMapper` converts at the adapter edge, scale 2 |

## Decisions and trade-offs

All assumptions, with reasons and code locations, are in [`ASSUMPTIONS.md`](ASSUMPTIONS.md).

- `POST /time-deposits/balance-updates` returns `204`, because each call adds a month and is not idempotent ([#7](ASSUMPTIONS.md)).
- `GET /time-deposits` returns withdrawals as a list of `{id, amount, date}` ([#8](ASSUMPTIONS.md)).
- Contract first: `openapi.yaml` is hand written, the server interface and DTOs are generated at build time ([#12](ASSUMPTIONS.md)).
- Swagger UI and the spec pages do not count as endpoints. No Actuator ([#9](ASSUMPTIONS.md)).
- Table and column names match the README exactly, quoted for Postgres ([#18](ASSUMPTIONS.md)).
- The update runs in one transaction ([#4](ASSUMPTIONS.md)). The read locks the rows (`PESSIMISTIC_WRITE`), so two updates at once cannot lose a month. `ConcurrentBalanceUpdateIT` checks it.
- Updating balances does not advance `days` ([#10](ASSUMPTIONS.md)).
- Trade-off: `/v3/api-docs` stays on next to the hand written spec, because springdoc 2.9.1 turns Swagger UI off with it. A test checks both list only two endpoints.
- Trade-off: the local run uses the Testcontainers dev runner, not a `docker-compose.yml`.

## What I would do with more time

- Money as `BigDecimal` in the domain end to end. Today the frozen `TimeDeposit` and calculator keep it in `double`.
- Input validation, for example a null `planType` or `balance`, which the original code turns into a `NullPointerException`.
- Error handling with a consistent error body, instead of default Spring errors.
- Pagination: page the deposits behind the same `TimeDepositRepository` port instead of loading all of them.
- Idempotency on the update: a period key (for example the month) so a repeated POST cannot add interest twice.
- Agree with the business what advances `days` and who calls the update each month.

## AI usage

Claude Code was the coding agent, driven by the rules in [`CLAUDE.md`](CLAUDE.md) and plan mode.
I set the order (analysis, rules, tests, refactor, app, API), read each plan, reviewed every diff and approved each commit.
Behavior was verified with characterization tests, PIT and a scratch mutation script, ArchUnit rules and real runs.
Details per part: [`AI_USAGE.md`](AI_USAGE.md). Every prompt, word for word, with what I checked and changed: [`docs/ai/prompts.md`](docs/ai/prompts.md).
