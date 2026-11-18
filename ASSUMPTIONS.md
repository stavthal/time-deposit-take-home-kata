# Assumptions

Each assumption the solution makes, why, and where it lives in the code.

| # | Assumption | Reason | Where in code |
|---|------------|--------|---------------|
| 1 | A plan type with no policy (unknown, or wrong case like `Basic`) earns zero interest. Its balance is still rewritten as `balance + 0.00`. | The original if/else chain matched no branch for these and left interest at 0. Behavior is frozen. | `TimeDepositCalculator.NO_INTEREST` |
