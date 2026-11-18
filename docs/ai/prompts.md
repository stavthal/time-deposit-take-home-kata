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
Real order: 1.1, 0.1, 0.2, 2.1, 2.2.
The prompt texts are verbatim, so phase numbers inside them use the old numbering
(their "Phase 0" is setup plus analysis, their "phase 1" is now Phase 2).

## Phase 0: setup

### Prompt 0.1: harness first

```text
Yes, let's do that. But first, we need to create the CLAUDE.md, isn't that right?
```

Intent: approve saving the analysis and starting the work, but write the `CLAUDE.md` rules file first.
What I checked: TODO
What I changed or rejected: TODO

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
What I checked: TODO
What I changed or rejected: TODO

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
What I checked: TODO
What I changed or rejected: TODO

Result: the analysis, saved in Prompt 0.2 as `docs/ai/analysis.md`.

## Phase 2: characterization tests

### Prompt 2.1: pin current behavior

```text
No need to print claude code here, let's continue to phase 1.
```

Intent: approve the plan to replace the placeholder test with characterization tests that pin today's `updateBalance` results for every plan and boundary.
What I checked: TODO
What I changed or rejected: TODO

Result: `TimeDepositCalculatorTest` rewritten with tests that pin today's results.
Expected values come from the real output on JDK 17, checked with exact double equality.
The expected values were taken from the current output on purpose, because characterization tests record existing behaviour; I then hand-checked the boundary rows against the README rules.

### Prompt 2.2: commit and close the phase

```text
yes. and tell me if we are done with phase 1
```

Intent: approve committing the characterization tests and ask for a check that the phase is complete.
What I checked: TODO
What I changed or rejected: TODO

Result: a manual mutation check on a scratch copy (operators, rounding mode, scale, rate, case)
found that `HALF_UP` to `HALF_EVEN` survived. Added an exact half cent test (`150.00` basic gives `150.13`).
Every mutant now fails. 23 tests, one commit.
