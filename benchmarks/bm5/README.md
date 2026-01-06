# Bench Mark 5 - maxOfThree (wrong comparison bug)

## What it does
`Program.maxOfThree(int a, int b, int c)` returns the maximum of three integers.

## Bug
In the buggy version, the comparison for `c` is wrong (`c < max` instead of `c > max`).
This can incorrectly overwrite the current maximum when `c` is smaller.

## Expected fix
Change the comparison in the `c` update check to `c > max`.

## Tests
Tests are in `tests/ProgramTest.java`.
- `whenCIsSmallest_shouldNotOverwriteMax()` fails on `buggy` and passes on `fixed`.
- The other tests pass on both versions.

## Fault localization
`faultloc.json` provides suspicious line weights for `Program.java` (not perfect localization).

## How to run
From the project root:
- `scripts/run.cmd bm6` (Windows)
- `./scripts/run.sh bm6` *(if available in repo)*
