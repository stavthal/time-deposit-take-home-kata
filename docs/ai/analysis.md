# Time Deposit Kata: Read Only Analysis

Sources read: `README.md`, `java/pom.xml`, `java/time-deposit-kata.iml`,
`java/src/main/java/org/ikigaidigital/TimeDeposit.java`,
`java/src/main/java/org/ikigaidigital/TimeDepositCalculator.java`,
`java/src/test/java/org/ikigaidigital/TimeDepositCalculatorTest.java`.
Baseline: `cd java && mvn test` on JDK 17 gives `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
That single test checks nothing (see trap 8).

## 0. Traps to never forget

1. Boundary 30/31: `getDays() > 30`. Day 30 gets no interest, day 31 does.
2. Boundary 45/46: premium `getDays() > 45`. Day 45 gets none, day 46 does.
3. Boundary 365/366: student `getDays() < 366`. Day 365 gets interest, day 366 does not.
4. `new BigDecimal(double)` rounding: it takes the exact binary value of the `double`.
   A value that looks like `x.xx5` may be stored as `x.xx4999...` and round down with `HALF_UP`.
   `BigDecimal.valueOf(double)` can round differently. Swapping them changes results.
5. `Double` for money: `balance` is a boxed `Double` and all math is `double`.
   The result after the rounded interest is added back is still a `double`, with float noise.
6. Unknown plan gives zero interest: no branch matches, interest stays `0`,
   and the balance is still rewritten as `balance + 0.00`. No error.
7. No withdrawals in `TimeDeposit`: the GET response needs `withdrawals`,
   but the frozen class has no such field.
8. Placeholder test: `assertThat(1).isEqualTo(1);` asserts nothing.
   Green today means nothing about the calculator.

## 1. Requirements checklist

API
- [ ] Endpoint A: update the balances of all time deposits in the database.
- [ ] Endpoint B: get all time deposits, each with `id`, `planType`, `balance`, `days`, `withdrawals`.
- [ ] Exactly two endpoints. No more.

Database
- [ ] Table `timeDeposits`: `id` Integer PK, `planType` String required, `days` Integer required, `balance` Decimal required.
- [ ] Table `withdrawals`: `id` Integer PK, `timeDepositId` Integer FK required, `amount` Decimal required, `date` Date required.
- [ ] All time deposit plans stored in the database.

Interest
- [ ] Basic: 1%.
- [ ] Student: 3%, no interest after 1 year.
- [ ] Premium: 5%, interest starts after 45 days.
- [ ] No interest in the first 30 days for any plan.
- [ ] Interest is monthly.

Refactoring
- [ ] Keep `TimeDepositCalculator.updateBalance` behavior exactly the same.
- [ ] Do not change the `updateBalance` signature.
- [ ] No breaking change to `TimeDeposit`.
- [ ] Design open to new interest rules (new plans, new conditions).

Quality and AI
- [ ] SOLID, design patterns, clean code.
- [ ] AI harness set up and used, documented, reproducible, with rules, prompts, agent configs.
- [ ] Summary of which parts were AI assisted and why.

Submission
- [ ] Fork into own GitHub repo. No PR or branch in `ikigai-digital`.
- [ ] Instructions to trigger endpoints through Swagger.
- [ ] Ambiguities justified in code comments.

Preferred (not mandatory)
- [ ] OpenAPI Swagger contract.
- [ ] Hexagonal architecture.
- [ ] Atomic commits.
- [ ] Testcontainers.

## 2. Constraints

- Signature frozen: `public void updateBalance(List<TimeDeposit> xs)` (`TimeDepositCalculator.java:8`).
- Behavior frozen: same inputs must give the same balances, to the cent, including double rounding quirks.
- `TimeDeposit` public API frozen: constructor `TimeDeposit(int id, String planType, Double balance, int days)`, `getId`, `getPlanType`, `getBalance`, `getDays`, `setBalance`. Adding members is non breaking. Removing or retyping is breaking.
- Exactly two endpoints. Swagger UI and the OpenAPI JSON are served docs, not business endpoints (assumption below).
- Invalid input and exception handling not required.
- Java 17 (`pom.xml:12-13`). Any framework allowed.

## 3. Exists vs missing

Exists
- `TimeDeposit`: plain mutable class, 4 fields, no `withdrawals`.
- `TimeDepositCalculator.updateBalance`: all rules in one if/else chain.
- One test that asserts nothing real.
- Maven pom with JUnit 5.9.0 and AssertJ 3.23.1 only.

Missing
- Web framework (Spring Boot or similar), both endpoints, controllers.
- OpenAPI contract and Swagger UI.
- Persistence: DB, schema, migrations (Flyway or Liquibase), seed data.
- `Withdrawal` model and its link to a deposit.
- Hexagonal layout: domain, ports (in and out), adapters (REST, JPA or JDBC).
- Extensible interest design (strategy per plan, registry).
- Real tests: characterization tests for the calculator, unit tests per plan, REST tests, Testcontainers integration tests.
- `src/main/resources`, app config, Dockerfile or compose (optional).
- AI harness docs (`CLAUDE.md`, agent configs, prompts) and README run instructions.

## 4. Issues in the existing code

Calculator (`TimeDepositCalculator.java`)
1. Money in `double`. Line 10 `double interest = 0;`, line 15 `getBalance() * 0.03 / 12`. Binary floating point is not exact for cents.
2. Mixed rounding. Line 26: `new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)` then `.doubleValue()` and added to a `double`. `new BigDecimal(double)` takes the exact binary value, so a value like `x.xx5` stored as `x.xx4999...` rounds down. Adding back to `double` reintroduces error.
3. Only interest is rounded, not the final balance. The balance can carry float noise over months.
4. String compare on magic literals: `"student"`, `"premium"`, `"basic"` (lines 13, 17, 21). Case sensitive. `"Basic"` gets no interest.
5. Unknown plan type silently gets 0 interest. Balance is still rewritten (`balance + 0.00`).
6. Null `planType` gives NPE at `getPlanType().equals(...)`. Null `balance` gives NPE on unboxing.
7. All rules in one method. Adding a plan means editing it. Breaks Open/Closed.
8. Rule order hides intent. The global `> 30` gate wraps plan rules, so premium `> 45` and student `< 366` live inside it.
9. Index loop with repeated `xs.get(i)` (5 calls per item). Bad on `LinkedList`. Hard to read.
10. Poor names: `xs`, `a2d`, `interest +=` on a value set once.
11. Rates are annual divided by 12, hard coded: `0.03 / 12`, `0.05 / 12`, `0.01 / 12`.
12. Does not advance `days`. Whatever moves `days` forward is undefined.

Exact operators and boundaries
- Global gate: `getDays() > 30` (line 12). Day 30 gets nothing. Day 31 is the first day with interest.
- Student: `getDays() < 366` (line 14). Interest on days 31 to 365. Day 366 and later get none.
- Premium: `getDays() > 45` (line 18). Interest from day 46. Days 31 to 45 get none.
- Basic: no extra condition (line 21). Interest from day 31, forever.
- Monthly amount: `balance * rate / 12`, rounded `HALF_UP` to scale `2`, added to balance.

Worked example: `basic`, `1234567.00`, `45` days (the test data).
`1234567.00 * 0.01 / 12 = 1028.80583...` then `setScale(2, HALF_UP)` gives `1028.81`. New balance `1235595.81`.
The test never checks this.

Model (`TimeDeposit.java`)
13. `Double balance` (boxed, floating). DB column is Decimal. Mapping needs care.
14. No `withdrawals` field, but the GET response needs it.
15. Mutable via `setBalance`. Fields not `final` even where never changed (`id`, `planType`, `days`).
16. Inconsistent style: `getId` on one line, others multi line.
17. No `equals`, `hashCode`, `toString`.

Test (`TimeDepositCalculatorTest.java`)
18. `assertThat(1).isEqualTo(1);` asserts nothing. Zero real coverage.
19. One case only. No boundaries (30, 31, 45, 46, 365, 366), no student, no premium, no unknown plan.
20. Name `updateBalance_Test` says nothing. Wildcard import `java.util.*`.

Build
21. `time-deposit-kata.iml` has `LANGUAGE_LEVEL="JDK_1_8"`, pom says `17`. IDE and Maven disagree.
22. Old plugins: surefire `3.0.0-M3` (milestone), compiler `3.8.1`. Compiler config repeats `source/target` already in properties.
23. Only `junit-jupiter-engine` declared. Works, but `junit-jupiter` aggregate is the usual choice.
24. `.iml` committed. IDE file in the repo.

## 5. Ambiguities, assumptions, reasons

1. What do 1%, 3%, 5% mean?
   Assume annual rates paid monthly as rate / 12.
   Reason: the frozen code does `* 0.01 / 12`. Behavior must not change.

2. "No interest after 1 year" for student: is day 365 or 366 the cut?
   Assume interest on days 31 to 365, none from 366.
   Reason: code uses `< 366`. Keep it.

3. "Interest starts after 45 days" for premium.
   Assume first interest at day 46.
   Reason: code uses `> 45`.

4. "First 30 days" boundary.
   Assume day 31 is the first with interest.
   Reason: code uses `> 30`.

5. What does `days` mean, and who increments it?
   Assume `days` is the deposit age in days, kept by another process. The update endpoint does not change it.
   Reason: calculator never changes `days`. Changing it adds behavior not asked for.

6. How often is the update endpoint called?
   Assume once per month by an outside scheduler. Each call applies one month of interest. Not idempotent.
   Reason: requirement says "monthly interest" and asks only for an endpoint, no scheduler.

7. Do withdrawals affect interest or balance?
   Assume no. They are history only; `balance` is already net of them.
   Reason: calculator ignores them and its behavior is frozen.

8. Shape of `withdrawals` in the GET response.
   Assume a list of objects `{id, amount, date}` per deposit.
   Reason: the table holds those fields. A list loses nothing. A count or sum would.

9. How to add `withdrawals` without breaking `TimeDeposit`?
   Assume keep `TimeDeposit` untouched. Build the response from a separate read model that joins withdrawals.
   Reason: zero risk to the shared class. Adding a field is also legal, but is not needed.

10. HTTP verb and path for the update.
    Assume `POST /time-deposits/balance-updates` (or `POST /time-deposits/update-balances`), return `204` or the updated list.
    Reason: the call is not idempotent, so POST fits. PUT and PATCH imply idempotent or partial resource edits.

11. GET path.
    Assume `GET /time-deposits`, return `200` with a JSON array.
    Reason: standard REST collection read.

12. "Exactly two endpoints" vs Swagger UI, `/v3/api-docs`, actuator.
    Assume Swagger UI and the spec are allowed. No actuator, no health endpoint.
    Reason: README asks for Swagger, so its docs pages are expected. Actuator adds endpoints nobody asked for.

13. Table and column names `timeDeposits`, `planType`, `timeDepositId` are camelCase.
    Assume keep the exact names, quoted in SQL (Postgres folds unquoted names to lower case).
    Reason: the README gives exact names. A reviewer may check them.

14. Decimal precision.
    Assume `NUMERIC(19,2)` for `balance` and `amount`.
    Reason: two decimals matches the `setScale(2, ...)` in the calculator.

15. Decimal in DB vs `Double` in `TimeDeposit`.
    Assume convert `BigDecimal` to `Double` at the adapter edge, feed the frozen calculator, convert back with scale 2 `HALF_UP`.
    Reason: the calculator must keep its `double` math to stay byte for byte the same.

16. Plan type values and case.
    Assume lowercase strings `basic`, `student`, `premium`. Unknown types get 0 interest.
    Reason: matches the code literals and current behavior.

17. Should the new extensible design replace the calculator body?
    Assume yes: move rules into a strategy per plan behind `updateBalance`, keep signature and results identical, prove it with characterization tests written first.
    Reason: README asks for refactor plus extensibility. Tests lock behavior before the change.

18. Transaction scope for the bulk update.
    Assume one transaction for all deposits.
    Reason: all or nothing avoids half updated months.

19. Data volume.
    Assume load all deposits in memory.
    Reason: kata scale. Paging can come later behind the same port.

20. Seed data.
    Assume a migration inserts sample deposits and withdrawals for each plan and boundary.
    Reason: lets a reviewer try both endpoints in Swagger right away.

21. Database choice.
    Assume PostgreSQL, Testcontainers for integration tests.
    Reason: README prefers Testcontainers. Postgres has real `NUMERIC` and FK support.
