# AI Usage

How AI was used to build this solution, and how each part was checked.
Every prompt is logged word for word in `docs/ai/prompts.md`, with what I checked and what I changed or rejected.

## Tools and setup

| Tool | Used for |
|------|----------|
| Claude Code | the coding agent, run in the terminal in this repo |
| `CLAUDE.md` at the repo root | the project rules the agent reads every session |
| Plan mode | the agent proposes a plan before it edits anything |
| Context7 | checking library versions before the pom changes (Prompt 4.1) |
| JUnit 5, AssertJ | characterization and unit tests |
| JaCoCo 0.8.11 | line and branch coverage |
| PIT 1.15.3 (`STRONGER` mutators) | mutation testing on the core |
| Scratch mutation script | mutants PIT cannot make, run on a scratch copy, never committed |
| ArchUnit | hexagonal rules as tests |
| Testcontainers Postgres | integration tests and the local run |

## How I worked

- One phase per session: setup, analysis, characterization tests, refactor, Spring Boot and persistence, contract first API.
- The analysis ran first, read only, and is saved in `docs/ai/analysis.md`.
- Plan mode first. I read each plan before approving it.
- I review every diff and approve each commit.
- The AI writes code. I make the decisions and verify the results.
- Two prompts (2.3 and 3.1) were lost when the context was cleared. The log says so instead of guessing.

## Custom rules (`CLAUDE.md`)

The full file is `CLAUDE.md` at the repo root. In short:

- Start of every session: read `docs/ai/analysis.md`, `git log --oneline` and `ASSUMPTIONS.md`.
- Work only in `java/`. Never touch the other language folders. Never modify the root `README.md`.
- Frozen contract: the `TimeDeposit` public API, and the signature, package and results of
  `TimeDepositCalculator.updateBalance`, to the cent, including the `double` math and the rounding.
- Exactly two HTTP endpoints. No Actuator, no extra controllers or routes. Swagger UI and the spec pages are allowed.
- Hexagonal layout. `domain` and `interest` import no Spring and no JPA. Adapters depend on the domain.
- Table and column names match the README exactly.
- Tests before refactoring. Every change keeps all tests green. No mocks in domain tests.
- Small steps, one logical change per commit, conventional messages.
- Never push or open a PR on `upstream`. Work on the fork.
- No abstraction without a clear reason. No dead code.
- Unclear README: pick an assumption, mark it `// Assumption:` in code, add it to `ASSUMPTIONS.md`.

## Commands

```sh
sdk use java 17.0.0-tem
cd java
mvn test                                        # unit, web slice, architecture
mvn verify                                      # plus Testcontainers integration tests
mvn org.pitest:pitest-maven:mutationCoverage    # mutation score on the core
mvn spring-boot:test-run                        # run the app with Postgres in Docker
```

The JaCoCo report is written to `target/site/jacoco/index.html`.

## What was AI assisted, how I verified it, and why

| Part | AI assisted | How I verified it | Why AI |
|------|-------------|-------------------|--------|
| Analysis (`docs/ai/analysis.md`) | Drafted by the agent. I added the missing traps myself (Prompt 0.2). | Compared the quoted operators `> 30`, `> 45`, `< 366` with the calculator. | Fast first read of a small codebase, with every boundary quoted. |
| `CLAUDE.md` | Drafted from my list of rules (Prompt 0.2). | Read it against the README constraints. | Rules had to exist before the first change. |
| Characterization tests | Written by the agent. I wrote the `Student` case row myself (Prompt 2.7). | Expected values from the real JDK 17 output, boundary rows checked by hand against the README. Mutation runs: 36 of 36, then PIT 33 of 33. | Many table rows, quick to write and easy to check. |
| PIT and JaCoCo setup | Added by the agent on my request (Prompts 2.3, 2.10). | Read both `index.html` reports. | Repeatable proof that the tests catch changes. |
| Refactor to `InterestPolicy` | Written by the agent, one plan per commit (Prompt 3.1). | 32 characterization tests green after each commit. PIT 26 of 26. Scratch script 39 of 39. | Mechanical moves, safe behind pinned tests. |
| `roundToCents` helper | Extracted by the agent. I rejected its comment (Prompt 3.3). | `new BigDecimal(interest)` and `HALF_UP` unchanged, 32 tests green. | Readability fix I asked for. |
| Spring Boot, ports, JPA, Flyway | Written by the agent from my spec (Prompt 4.1). | Plan read first. Flyway applied V1 and V2. The persistence IT checks one prepared statement, so no N+1. | Boilerplate heavy, with a clear spec. |
| Phase 4 fixes: startup class move, transaction decorator | Proposed by the agent. I asked if it was right for hexagonal (Prompts 4.12, 4.13). | Tests green after each fix. A probe class with a Spring import failed the ArchUnit rule, then was deleted. | Second opinion on the architecture. |
| Locking note (`ASSUMPTIONS.md` #11) | Written by the agent on my request (Prompt 4.15). | Read the row and the lock notes. | The risk had to be written down before the push. |
| Local run | The agent diagnosed the failure (Prompt 4.7). I rejected its `compose.yaml` and had it fix my machine instead (Prompt 4.8). | `mvn clean verify` and `mvn spring-boot:test-run` passed with no Docker variables set. Sources: the Spring Boot reference "Development-time Services" and the Spring blog post on Testcontainers in Boot 3.1 (Prompt 4.19). | The project was fine. Only the machine was wrong. |
| Swagger path | The agent proposed the root path and an `/api` prefix. I chose `/docs` (Prompt 4.25). | A test checks that `/docs` opens Swagger UI. | Keep the API paths as they are. |
| Contract first API | Written by the agent from my spec (Prompt 5.1). | Generated sources (one interface, two DTOs), full `mvn verify`, a real run: `POST` gives `204`, Swagger UI loads `/openapi.yaml`. | Generator config and wiring. |
| Stricter tests after Phase 5 | Commits `516f743` (exact GET body, one POST result per plan) and `e5c0e30` (no Hibernate in the core). | `mvn clean verify` green. | Tighter checks on the contract and the layers. |
| These docs | Drafted by the agent from the git log, the prompt log and the code. | I read them against the history before committing. | Sum up work already done and logged. |

## What I decided, not the AI

- The order: analysis, rules, tests, refactor, app, API.
- The full trap list in the analysis.
- Turned down a test for `main`: it would boot a second app only to move coverage (Prompt 4.6).
- Skipped the slow build of every commit: 12 of 15 had built green, the rest were docs or HEAD (Prompt 4.11).
- Fixed my machine, not the project (Prompt 4.8).
- Swagger at `/docs`, no `/api` prefix (Prompt 4.25).
- Write down the lost update risk and how to lock, before the push (Prompt 4.15).
