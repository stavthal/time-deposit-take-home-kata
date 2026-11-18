# Prompt log

The prompts given to the AI agent (Claude Code), copied word for word.

## How I worked

- Tool: Claude Code, with `CLAUDE.md` at the repo root as the rules file.
- One phase per session.
- Plan mode first: the agent proposes a plan before it edits anything.
- I review every diff and approve each commit.
- The AI writes code. I make the decisions and verify the results.

Phases follow the runbook: Phase 0 setup, Phase 1 analysis,
Phase 2 characterization tests, Phase 3 refactor, Phase 4 Spring Boot and persistence,
Phase 5 contract first API, and so on.

Prompts are grouped by phase. The analysis prompt (1.1) ran first, before setup.
Real order: 1.1, 0.1, 0.2, 2.1, 2.2, 2.3, 2.4 to 2.11, 3.1 to 3.9, 4.1 to 4.25, 5.1.
The prompt texts are verbatim, so phase numbers inside them use the old numbering
(their "Phase 0" is setup plus analysis, their "phase 1" is now Phase 2).

## Phase 0: setup

### Prompt 0.1: harness first

```text
Yes, let's do that. But first, we need to create the CLAUDE.md, isn't that right?
```

Intent: approve saving the analysis and starting the work, but write the `CLAUDE.md` rules file first.
What I checked: The analysis covered the requirements and constraints before any files were created.
What I changed or rejected: I made CLAUDE.md come before any other work, so the rules applied from the first change.

Result: a first draft of `CLAUDE.md`.

### Prompt 0.2: finish setup

```text
Now finish Phase 0 setup. Do not touch any code under java/src.

1. Save your analysis above to docs/ai/analysis.md (requirements checklist,
   constraints, exists vs missing, issues, ambiguities with proposed
   assumptions). Add any trap you missed from this list: boundaries 30/31,
   45/46, 365/366; new BigDecimal(double) rounding; Double for money;
   unknown plan = zero interest; no withdrawals in TimeDeposit; the
   assertThat(1).isEqualTo(1) placeholder test.

2. Create CLAUDE.md at the repo root with these rules:
   - Work only in java/. Never touch the other language folders.
   - Never change TimeDeposit's public API, or updateBalance's signature,
     package or results.
   - Exactly two HTTP endpoints. No Actuator, no extra controllers.
   - Hexagonal layout: domain and interest packages import no Spring or JPA.
   - Tests before refactoring. Every change keeps all tests green.
   - No mocks in domain tests. Testcontainers Postgres for integration tests.
   - Small steps. One logical change per commit, conventional messages
     (test:, refactor:, feat:, build:, docs:, chore:).
   - No abstractions without a clear reason. No dead code.
   - At the start of each session read docs/ai/analysis.md and git log.

3. Create empty skeletons: ASSUMPTIONS.md, AI_USAGE.md, docs/ai/prompts.md.
   In docs/ai/prompts.md, log the Phase 0 prompts used so far.

4. Run `cd java && mvn test` and confirm it passes.

5. Commit as two atomic commits:
   - "chore: add AI harness config (CLAUDE.md) and docs skeleton"
   - "docs: add analysis of the existing implementation"

Show me the CLAUDE.md content and the git log when done.
```

Intent: lock the project rules and docs skeleton before any code changes, save the analysis with every known trap, and prove the build is green.
What I checked: I read CLAUDE.md against the README constraints, confirmed mvn test passed, confirmed two commits in git log, and confirmed java/src was untouched.
What I changed or rejected: I supplied the list of traps myself so analysis.md would be complete.

Result: `CLAUDE.md`, `ASSUMPTIONS.md`, `AI_USAGE.md`, this file, `docs/ai/analysis.md`, two commits.

## Phase 1: analysis

### Prompt 1.1: analysis

```text
Read README.md and everything under java/. Do not change any file.
Give me: (1) the requirements as a checklist, (2) the constraints,
(3) what exists vs what is missing, (4) every issue in the existing code,
(5) every ambiguity with a proposed assumption and its reason.
Quote exact operators and boundaries from the calculator.
```

Intent: understand the task and the existing code read only, before deciding anything, with exact boundaries quoted so later tests can pin them.
What I checked: I compared the quoted operators (`> 30`, `> 45`, `< 366`) with `TimeDepositCalculator.java`, and saw the baseline `mvn test` gave `Tests run: 1` from the placeholder test.
What I changed or rejected: the analysis missed some traps, so I supplied the full trap list myself in Prompt 0.2.

Result: the analysis, saved in Prompt 0.2 as `docs/ai/analysis.md`.

## Phase 2: characterization tests

### Prompt 2.1: pin current behavior

```text
No need to print claude code here, let's continue to phase 1.
```

Intent: approve the plan to replace the placeholder test with characterization tests that pin today's `updateBalance` results for every plan and boundary.
What I checked: I read the plan before approving it, and checked it touched only the test file and left production code alone.
What I changed or rejected: I turned down the agent's offer to print the Claude Code output here, and went straight to the tests.

Result: `TimeDepositCalculatorTest` rewritten with tests that pin today's results.
Expected values come from the real output on JDK 17, checked with exact double equality.
The expected values were taken from the current output on purpose, because characterization tests record existing behaviour; I then hand-checked the boundary rows against the README rules.

### Prompt 2.2: commit and close the phase

```text
yes. and tell me if we are done with phase 1
```

Intent: approve committing the characterization tests and ask for a check that the phase is complete.
What I checked: I read the mutation check results, including the surviving `HALF_UP` to `HALF_EVEN` mutant, before calling the phase done.
What I changed or rejected: I did not accept the phase as done while a mutant survived. The `150.00` basic test went in first.

Result: a manual mutation check on a scratch copy (operators, rounding mode, scale, rate, case)
found that `HALF_UP` to `HALF_EVEN` survived. Added an exact half cent test (`150.00` basic gives `150.13`).
Every mutant now fails. 23 tests, one commit.

### Prompt 2.3: table driven rewrite and JaCoCo

```text
Not captured. The context was cleared between sessions, so the exact text is lost.
In short: add a JaCoCo coverage report and rewrite the tests as one table.
```

Intent: add a coverage report and rewrite the tests as one `@CsvSource` table.
What I checked: the table kept every earlier case, and `mvn test` ran 31 tests green.
What I changed or rejected: nothing.

Result: commits `cae23c4` (JaCoCo and `junit-jupiter-params`) and `b407cf6` (table driven tests), 31 tests.
The mutation check was not run again after this rewrite. Prompt 2.5 found what that missed.

### Prompt 2.4: check against the analysis

```text
I need one lasat check up to now, against the analysis in @analysis
```

Intent: check the work so far against every trap and issue in `docs/ai/analysis.md` before closing the phase.
What I checked: the trap by trap status, the test run (`Tests run: 31`) and the JaCoCo totals.
What I changed or rejected: I did not let the agent fix the prompt log yet. I asked for the mutation check first.

Result: no code change. Found a stale test count and missing prompt entries in this file,
and that the mutation check had not been run again after `b407cf6`.

### Prompt 2.5: mutation check again

```text
No, run only the mutation check again.
```

Intent: prove the table driven tests still catch every mutant.
What I checked: the list of 36 mutants and which ones survived.
What I changed or rejected: I kept the run read only. The agent worked on a scratch copy and changed nothing in the repo.

Result: 34 of 36 killed. Two survived: `equalsIgnoreCase("student")` on line 13,
and `i += 2` on line 9, which skips every second deposit.

### Prompt 2.6: JaCoCo report

```text
I need you to check JaCaCa index.html report that has been produced, and tell me the line and branch coverage for @java/src/test/java/org/ikigaidigital/TimeDepositCalculatorTest.java and list anything not covered. Do not touch production code and don't change anything.
```

Intent: read the coverage numbers and list any line or branch the tests miss.
What I checked: line and branch totals per class.
What I changed or rejected: nothing changed.

Result: 27 of 27 lines and 14 of 14 branches. JaCoCo measures the production classes the test runs, not the test file.
Full coverage still let the two mutants from Prompt 2.5 survive.

### Prompt 2.7: student case row

```text
I just added a row with a capital "S" for student, and the test passed. Perhaps you can format it better than I already did where I added the case in the end of the CsvSource annotation.
```

Intent: I wrote the `Student` row myself to kill the case mutant, then asked the agent to place it well.
What I checked: the row sits in the case sensitivity group, and the student mutant now fails the tests.
What I changed or rejected: the agent moved my row from the end of the table into that group and removed a blank line holding only spaces.

Result: 32 tests, green. The `equalsIgnoreCase("student")` mutant is killed.

### Prompt 2.8: why it was missed

```text
Why wasn't that added already in the first place though?
```

Intent: find the root cause instead of only fixing the symptom.
What I checked: the case rows in `afbfb02` and the list test in `b407cf6`.
What I changed or rejected: nothing changed.

Result: the case test held only `Basic` and `PREMIUM`, one sample per rule instead of one per branch.
The rewrite in `b407cf6` made the list test weaker, and the mutation check was not run again.
Lesson: one case per branch, and run the mutation check again after any test rewrite.

### Prompt 2.9: fix the loop skip mutant

```text
Let's move to the second mutation, the one in updatesEveryDepositInTheList.
```

Intent: make the list test fail when any deposit is skipped.
What I checked: the full mutation script result after the change.
What I changed or rejected: every deposit in the list now earns interest. Student moved from day 366 to day 365, and `gold` was dropped because the table already covers unknown plans.

Result: 36 of 36 mutants killed. 32 tests, green.

### Prompt 2.10: PIT mutation testing

```text
I also noticed that you never added the maven plugin pitest which is a global claude.md directive to install it in new projects during setup of tests. Then run it again, and tell me the mutation score and any surviving mutant for the test that we have, with its line and mutation type.
```

Intent: replace the hand made mutation check with a standard tool that anyone can run again.
What I checked: the mutation score and the list of mutants by line and type.
What I changed or rejected: the agent found no pitest rule in either `CLAUDE.md`, so the plugin was added on this request.

Result: `pitest-maven` 1.15.3 with the `STRONGER` mutators. 33 of 33 mutants killed, mutation score 100%.
PIT cannot make some mutants that matter here (rounding mode, `BigDecimal.valueOf`, `equalsIgnoreCase`, `i += 2`), so the scratch script still covers them.

### Prompt 2.11: close the phase

```text
Yes, you should go on and commit those. But before you do, I need you to make sure prompts.md is also up to date with the prompts that we have, so that we can sum this up, and move to the next phase.
```

Intent: bring this log up to date, commit the open work, and close Phase 2.
What I checked: this file against the session, and the git log.
What I changed or rejected: Nothing.

Result: three commits: the PIT plugin, the two test fixes, and this log.

## Phase 3: refactor

### Prompt 3.1: interest policies

```text
Not captured. The context was cleared between sessions, so the exact text is lost.
In short: move each plan out of the if/else chain into its own InterestPolicy, one plan per commit.
```

Intent: replace the plan if/else chain with one policy class per plan, without changing any result.
What I checked: the 32 characterization tests stayed green after every commit.
What I changed or rejected: nothing recorded.

Result: commits `98387b7` (basic), `2bdd7db` (premium), `7d84664` (student), `e930dd0` (simpler loop),
and `3104164` (unknown plan earns zero interest, recorded as an assumption).

### Prompt 3.2: readable rounding line

```text
I believe this line of code is not so readable. Perhaps, we should narrow it down a little bit? I understand the code and it works, it's just that it doesn't look so clean.
```

Intent: make the balance update line in `TimeDepositCalculator` easier to read.
What I checked: the rounding still uses `new BigDecimal(interest)` with `setScale(2, RoundingMode.HALF_UP)`, and `mvn test` ran 32 tests green.
What I changed or rejected: nothing yet. The agent extracted the rounding into a private `roundToCents` helper.

Result: the line became `double newBalance = deposit.getBalance() + roundToCents(interest);`.

### Prompt 3.3: drop the helper comment

```text
I don't really like the comment that you have added to this helper. Perhaps it could not have a comment at all, as the code it self is pretty much self-explanatory.
```

Intent: keep the helper free of comments, because its name already says what it does.
What I checked: the diff and the test run, 32 tests green.
What I changed or rejected: I rejected the agent's comment about `new BigDecimal(double)` versus `BigDecimal.valueOf`.
The trap is already recorded in `docs/ai/analysis.md`.

Result: `roundToCents` with no comment.

### Prompt 3.4: commit

```text
Yes.
```

Intent: approve the commit.
What I checked: only `TimeDepositCalculator.java` was staged.
What I changed or rejected: nothing.

Result: commit `9733e87 refactor: extract cent rounding into roundToCents`.

### Prompt 3.5: update this log

```text
WE need to also update prompts.md with the prompts we have used.
```

Intent: log the Phase 3 prompts in this file.
What I checked: this file against the session and the git log.
What I changed or rejected: nothing.

Result: this section.

### Prompt 3.6: commit the log

```text
Yes, as long as there are no todos.
```

Intent: commit the Phase 3 log only if it holds no open TODO.
What I checked: a search for `todo`, `tbd` and `fixme` found no matches.
What I changed or rejected: nothing.

Result: commit `b05e01f docs: log phase 3 prompts 3.1 to 3.5`.

### Prompt 3.7: coverage and PIT again

```text
Run the tests with full coverage, once more, using mutation testing with PIT and show any uncovered branches from the index.html that gets produced.
```

Intent: prove the refactored code is still fully covered and every PIT mutant is killed.
What I checked: both `index.html` reports, and the JaCoCo source pages for partly or not covered lines.
What I changed or rejected: nothing.

Result: 32 tests green. JaCoCo: `0 of 8` branches and `0 of 147` instructions missed, 35 of 35 lines.
PIT: 26 of 26 mutants killed, down from 33 because the code moved into the policy classes.

### Prompt 3.8: scratch mutation script

```text
Yes, run it, so we can wrap up and move to the next phase.
```

Intent: run the mutants PIT cannot make, such as `BigDecimal.valueOf` and other rounding modes.
What I checked: the per mutant result list, and a clean `git status` afterwards.
What I changed or rejected: the old script targeted the single file from before the refactor.
The agent ported it to the policy classes on a scratch copy and added 4 mutants new to this layout.

Result: 39 of 39 mutants killed, none survived.

### Prompt 3.9: close the phase

```text
Yes.
```

Intent: log the last prompts and close Phase 3.
What I checked: this file against the session and the git log.
What I changed or rejected: nothing.

Result: this entry and one commit. Phase 3 is closed.

## Phase 4: Spring Boot, hexagonal layout and persistence

Two sessions. Prompts 4.1 to 4.6 built the app. Prompts 4.7 to 4.25 checked the phase, fixed the machine and tidied the run.
The texts were recovered from the saved Claude Code session files for this project.

### Prompt 4.1: Spring Boot with a hexagonal layout

```text
Turn java/ into a Spring Boot 3 app with hexagonal layout.
Before writing code, show me: the pom changes (check versions with Context7),
the package tree, and every new class with a one-line responsibility.
Ports: TimeDepositRepository (outbound), two use cases (inbound).
Persistence: separate JPA entities, Flyway V1 for the two tables exactly as in
the README (DECIMAL(19,2) for money), V2 with demo seed data, a mapper that
converts BigDecimal <-> Double. Load withdrawals without N+1.
The domain packages must not import Spring or JPA.
```

Intent: add the Spring Boot app, the ports and the Postgres persistence, with the core kept free of Spring.
What I checked: the plan (pom changes, package tree, one line per class) before any code was written.
What I changed or rejected: nothing recorded.

Result: commits `af3a375` (Spring Boot 3.5.16 parent), `589ea63`, `fc4e5b7`, `627eabd`, `21a26aa`.

### Prompt 4.2: resume after a pause

```text
Continue, the session has been paused.
```

Intent: let the agent finish the approved plan.
What I checked: the report. 43 tests green, PIT `Killed 29 (100%)`, Flyway applied V1 and V2.
The persistence IT checks `getPrepareStatementCount() == 1`, so withdrawals load without N+1.
What I changed or rejected: nothing. The agent added one commit outside the plan, `df61c53`,
because the PIT JUnit launcher clashed with the one Spring Boot manages.

Result: commits `806268a` (V2 seed data), `24cf62c` (ArchUnit rule), `bc51375` (assumptions), `df61c53`.

### Prompt 4.3: the deleted .iml files

```text
No, I did not delete them
```

Intent: answer the agent's question about two `.iml` files that showed as deleted.
What I checked: that I had not deleted them.
What I changed or rejected: the agent restored both files from git. Its best guess was an IntelliJ Maven reimport.

Result: clean working tree, no commit.

### Prompt 4.4: REST adapter

```text
Yes
```

Intent: approve the next step, the REST adapter with springdoc `2.9.1`.
What I checked: the report. 48 tests green, and `TimeDepositApiIT` checks that `/v3/api-docs` lists exactly the two endpoints.
What I changed or rejected: the agent found that its first commit, `08f66d2`, had swept in the `.iml` deletions.
It was never pushed. The agent redid it as `0dd0a4e`.

Result: commits `0dd0a4e`, `8902618`, `fbcc3dc`, `d169de9`, `e33b9aa`, `8de012f`.

### Prompt 4.5: full run before the push

```text
Yes. Also, I want one full test run and coverage through JaCaCa reporting, plus PIT, to make sure no tests have broken. I am aware of the changes happened. No need to change any code.
```

Intent: prove nothing broke, then push.
What I checked: 41 unit and 7 integration tests green. JaCoCo lines `98.1% (105/107)`, branches `100.0% (8/8)`.
The 2 missed lines are in `TimeDepositApplication.main`. PIT `Killed 29 (100%)`.
What I changed or rejected: nothing.

Result: pushed `c4ea358..8de012f main -> main` to my fork.

### Prompt 4.6: a test for main

```text
Why don't we add a test for that too? Would that be overkill?
```

Intent: decide whether to cover the 2 missed lines in `main`.
What I checked: the agent's answer. A test would boot a second full app only to move coverage to 100%.
What I changed or rejected: rejected the test. Coverage stays at 98.1%, with the gap explained.

Result: no change.

### Prompt 4.7: the app does not run

```text
Read CLAUDE.md, docs/ai/analysis.md and git log --oneline.
Plan mode. The app does not run. Diagnose first, fix second.

STEP 1: Diagnose (change nothing):
- Run: java -version, docker ps, cd java && mvn clean verify
- Then try to start the app the way a reviewer would.
- Report the exact error and its root cause, with the log lines as evidence.
- Check the usual suspects: Docker not running; no Postgres for the app
  to connect to; Flyway 10+ missing flyway-database-postgresql; table
  names (Postgres lowercases unquoted timeDeposits); JPA ddl-auto
  validate vs the Flyway schema; datasource properties; Java version.
- Propose the smallest fix, then STOP and wait for my approval.

STEP 2: Fix (after I approve), one commit per fix, mvn clean verify after each.
- A reviewer must be able to run it with Docker and one command.
  Prefer a docker-compose.yml with Postgres plus `mvn spring-boot:run`,
  unless you find a simpler option already in the project.

STEP 3: Verify Phase 4, PASS/FAIL with evidence:
1. mvn clean verify passes; characterization test count unchanged.
2. The app starts against Postgres; Flyway applies V1 (both tables as in
   README.md) and V2 (seed data). Show the Flyway log lines.
3. Package layout: org.ikigaidigital (TimeDeposit, calculator),
   .interest, .application (use cases + ports),
   .adapter.out.persistence (entities, repos, adapter, mapper).
4. No Spring or JPA imports in org.ikigaidigital or .interest (grep).
5. TimeDeposit.java and the updateBalance signature unchanged.
6. Outbound port TimeDepositRepository; JPA entities separate from TimeDeposit.
7. Mapper converts BigDecimal <-> Double, with an assumption comment.
8. UpdateAllBalancesUseCase loads all, calls updateBalance, saves all in
   one transaction, does not change days (assumption comment present).
9. Withdrawals load without N+1 (fetch join or entity graph).
10. No controller and no endpoints yet (those are Phase 5). No Actuator.
11. README.md untouched. No dead code.
12. Every commit atomic, green, conventional message.

End with a table. Say "Ready for Phase 5" only if all pass.
```

Intent: find out why the app did not start, fix it, and gate the phase.
What I checked: the diagnosis. The code was fine. The machine used JDK 11 by default,
`/var/run/docker.sock` pointed at Docker Desktop instead of Colima, and nothing started Postgres for `mvn spring-boot:run`.
What I changed or rejected: rejected the proposed `compose.yaml` (see 4.8).

Result: no change yet.

### Prompt 4.8: fix the machine, not the project

```text
You need to fix my machine, not the project. The specs of the project are fine as they are, but my docker setup is relying on some older versions.
```

Intent: keep the project as it is and repair the local Docker setup.
What I checked: `mvn clean verify` and `mvn spring-boot:test-run` both passed with no Docker variables set.
What I changed or rejected: my own `sudo ln -sf $HOME/.colima/default/docker.sock /var/run/docker.sock` failed,
because the `!` prompt cannot read a password. The agent ran the same command through the macOS password dialog.

Result: the socket points at Colima. No project files changed.

### Prompt 4.9: JDK 17 by default

```text
Yes, do that.
```

Intent: make JDK 17 the default for every new shell.
What I checked: a fresh login shell gives `openjdk version "17"`.
What I changed or rejected: nothing.

Result: `sdk default java 17.0.0-tem`. No project files changed.

### Prompt 4.10: run the Phase 4 checklist

```text
Go
```

Intent: run the 12 checks from 4.7.
What I checked: the static checks. Check 12 started a fresh build of each of the 15 commits since Phase 3.
What I changed or rejected: nothing yet (see 4.11).

Result: no change.

### Prompt 4.11: skip the slow check

```text
I believe that this check is overkill, and we are wasting time. Better skip that specific one that will take several minutes.
```

Intent: stop the build of every single commit.
What I checked: 12 of 15 commits had already built green. Of the rest, 2 change docs only, and HEAD passed `mvn clean verify`.
What I changed or rejected: rejected the full per commit run.

Result: 9 of 12 checks pass. Checks 4, 8 and 10 fail.

### Prompt 4.12: the right fix

```text
Which is the most correct one architectural wise?
```

Intent: choose how to fix checks 4, 8 and 10.
What I checked: the proposal. Move the startup class out of the core package. Wrap the use case in one transaction
with a decorator in `config`. Keep the controller and accept check 10, since only the phase order differs.
What I changed or rejected: nothing yet.

Result: no change.

### Prompt 4.13: is it hexagonal

```text
is this correct to do for hexagonal architecture? Sounds right to me, but not sure.
```

Intent: confirm the proposal fits the hexagonal rules.
What I checked: the reasons. The startup class is wiring code and sits outside the core.
The transaction is a framework detail, so a decorator in `config` keeps `application` free of Spring.
What I changed or rejected: nothing.

Result: no change.

### Prompt 4.14: apply the fixes

```text
Yes
```

Intent: approve the two fixes.
What I checked: 43 unit and 7 integration tests green after each fix. `TimeDepositCalculatorTest` still has 32 tests.
A probe class with a Spring import failed the ArchUnit rule for the root package, then was deleted.
What I changed or rejected: nothing. The move also needed a new `PersistenceConfig` and explicit test config.

Result: commits `7d030b5`, `c0f0aa3`, `5bc2ead`.

### Prompt 4.15: record the locking risk, then push

```text
Push them, but first write that down to the assumptions, and how you would lock the row briefly, in the assumptions.
```

Intent: write down that one transaction does not stop a lost update, and how to lock the rows.
What I checked: `ASSUMPTIONS.md` row 11 and the lock notes under the table.
What I changed or rejected: nothing. The last commit changes docs and a comment only, so only `mvn compile` ran for it.

Result: commit `e001356`, pushed `8de012f..e001356 main -> main`.

### Prompt 4.16: does the app boot

```text
so all tests run? The spring boot app still needs the DB and doesn't boot though?
```

Intent: check that the app runs, not only the tests.
What I checked: the answer. `TimeDepositApplication` needs a Postgres that is already running.
`TestTimeDepositApplication` starts Postgres in Docker first.
What I changed or rejected: nothing.

Result: no change.

### Prompt 4.17: connection refused

```text
This is where we are right now. It boots, but we get the connection refused.

[pasted startup log, trimmed here; its last line:]
Unable to obtain connection from database: Connection to localhost:5432 refused.
```

Intent: report the error I saw in IntelliJ.
What I checked: the log showed I had run `TimeDepositApplication`, not `TestTimeDepositApplication`.
What I changed or rejected: nothing.

Result: no change.

### Prompt 4.18: is that the goal

```text
Yes, TestTime seems to be working. Is t hat the goal though?
```

Intent: confirm the dev runner meets "Docker and one command".
What I checked: `mvn spring-boot:test-run` starts Postgres, runs Flyway and serves on `8080`.
What I changed or rejected: nothing.

Result: no change.

### Prompt 4.19: the source

```text
how do you know that this is the way indeed? Where is that source? I have missed it perhaps, and my sleep deprivation doesn't help
```

Intent: get the source for the Testcontainers dev runner.
What I checked: the Spring Boot reference page "Development-time Services", section "Using Testcontainers at Development Time",
and the Spring blog post "Improved Testcontainers Support in Spring Boot 3.1".
What I changed or rejected: the agent corrected itself. It is one of two official ways, the other is Docker Compose support.

Result: no change.

### Prompt 4.20: where is Swagger

```text
Where is the UI for swagger?
```

Intent: find the Swagger page.
What I checked: http://localhost:8080/swagger-ui.html, which works only while the app runs.
What I changed or rejected: nothing.

Result: no change.

### Prompt 4.21: a shorter Swagger URL

```text
shouldn't we set another url for swagger, to make it easier t han having to type the swagger-ui.html? Is there a way?
```

Intent: make Swagger easier to open.
What I checked: the proposal, `springdoc.swagger-ui.use-root-path: true`.
What I changed or rejected: nothing yet.

Result: no change.

### Prompt 4.22: Swagger at the root

```text
Yes. Do that
```

Intent: approve Swagger at `/`.
What I checked: the test `rootPathOpensSwaggerUi` failed first with `404`, then passed. 43 unit and 8 integration tests green.
What I changed or rejected: the auto mode safety check blocked the commit and push. The agent stopped there.

Result: no commit. The change was replaced in 4.25.

### Prompt 4.23: an /api prefix

```text
That way though, we need different prefix for the api, something like /api .
```

Intent: keep the API apart from the docs pages.
What I checked: the answer. Nothing clashes, but `/api` is a sensible convention.
What I changed or rejected: nothing yet.

Result: no change.

### Prompt 4.24: prefix in the controller or the yml

```text
Well, change the controller, and add the api prefix, or even better, do it from the .yml file of the application
```

Intent: add the `/api` prefix.
What I checked: nothing. I stopped the agent while it was changing the tests.
What I changed or rejected: rejected the `/api` prefix (see 4.25).

Result: no change.

### Prompt 4.25: Swagger at /docs

```text
You know what, just change the uri from where we serve swagger, to something like "/docs"
```

Intent: keep the API paths, and move only the Swagger page.
What I checked: `/docs` redirects to `/swagger-ui/index.html`, and a test checks it.
What I changed or rejected: dropped the root path and the `/api` prefix.

Result: commit `57fb190`. I pushed it myself: `e001356..57fb190 main -> main`.

## Phase 5: contract first API

### Prompt 5.1: openapi.yaml first

```text
Write src/main/resources/openapi.yaml first with exactly two
operations: GET /time-deposits (id, planType, balance, days, withdrawals[id,
amount, date]) and POST /time-deposits/balance-updates returning 204.
Generate the server interface with openapi-generator-maven-plugin
(interfaceOnly, Spring Boot 3). The controller implements it and only calls
use cases. Point Swagger UI at this file. No Actuator, no other endpoints.
```

Intent: make the hand written spec the single contract, and generate the server code from it.
What I checked: the generated sources (one interface, two DTOs, no extras), the full `mvn verify`,
and a real run with `mvn spring-boot:test-run` (`POST` gives `204`, Swagger UI loads `/openapi.yaml`).
What I changed or rejected: the plan turned off `/v3/api-docs`. springdoc 2.9.1 then turned Swagger UI off too,
so it stays on, and a test checks that it lists the same two endpoints.
A `WebMvcConfigurer` serving `classpath:/` failed to start. Maven now copies the file into `static/` instead.
A new ArchUnit rule keeps the web adapter on the use case ports. A probe with a bad import made it fail, as expected.

Result: commits `d1bb2db`, `bcfa025`, `2ed6ea4`, `103f950`, `8adb4e0`. 43 unit tests and 10 integration tests green.
