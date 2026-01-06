# Bench Mark 3 - mean (integer division bug)

## What it does
`Program.mean(int[] a)` returns the arithmetic mean of an int array as a `double`.

## Bug
In the buggy version, the mean is computed using integer division (`sum / a.length`), so fractional means are truncated (e.g., `3 / 2` becomes `1` instead of `1.5`).

## Expected fix
Perform floating-point division (e.g., `sum / (double) a.length`).

## Tests
Tests are in `tests/ProgramTest.java`.
- `meanCanBeFractional_returnsFraction()` fails on `buggy` and passes on `fixed`.
- The other tests pass on both versions.

## Fault localization
`faultloc.json` provides suspicious line weights for `Program.java` (not perfect localization).

## How to run
From the project root:
- `scripts/run.cmd bm3` (Windows)
- `./scripts/run.sh bm3` *(if available in repo)*
