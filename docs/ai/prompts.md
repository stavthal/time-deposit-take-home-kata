# Prompt log

The prompts given to the AI agent (Claude Code), copied word for word.

## How I worked

- Tool: Claude Code, with `CLAUDE.md` at the repo root as the rules file.
- One phase per session.
- Plan mode first: the agent proposes a plan before it edits anything.
- I review every diff and approve each commit.
- The AI writes code. I make the decisions and verify the results.

Phases follow the runbook: Phase 0 setup, Phase 1 analysis,
Phase 2 characterization tests, Phase 3 refactor, and so on.

Prompts are grouped by phase. The analysis prompt (1.1) ran first, before setup.
Real order: 1.1, 0.1, 0.2, 2.1, 2.2, 2.3, 2.4 to 2.11.
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
TODO: prompt text not captured. Paste it here.
```

Intent: add a coverage report and rewrite the tests as one `@CsvSource` table.
What I checked: TODO.
What I changed or rejected: TODO.

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
What I changed or rejected: TODO.

Result: three commits: the PIT plugin, the two test fixes, and this log.
