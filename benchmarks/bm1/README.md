# Bench Mark 1 - Clamp (low high range return bug)

## What it does
`Program.clamp(int x, int low, int high)` clamps `x` into the inclusive range `[low, high]`.

## Bug
In the buggy version, when `x > high` the method incorrectly returns `low` instead of `high`.

## Expected fix
Change the return value in the `x > high` branch to return `high`.

## Tests
Tests are in `tests/ProgramTest.java`.
- The test `aboveUpperBound_returnsHigh()` fails on `buggy` and passes on `fixed`.
- The other tests pass on both versions.

## Fault localization
`faultloc.json` provides suspicious line weights for `Program.java` (not perfect localization).

## How to run
From the project root:
- `scripts/run.cmd bm1` (Windows)
