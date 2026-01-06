# Bench Mark 2 - parseAndSumCSV (split delimiter bug)

## What it does
`Program.parseAndSumCSV(String s)` parses a comma separated list of integers and returns the sum.

## Bug
In the buggy version, the input is split using `", "` (comma + space) instead of `","`.
So inputs like `"1,2,3"` are not split correctly and fail to parse.

## Expected fix
Change the split delimiter from `", "` to `","`.

## Tests
Tests are in `tests/ProgramTest.java`.
- `csvWithoutSpaces_parsesCorrectly()` fails on `buggy` and passes on `fixed`.
- The other tests pass on both versions.

## Fault localization
`faultloc.json` provides suspicious line weights for `Program.java` (not perfect localization).

## How to run
From the project root:
- `scripts/run.cmd bm2` (Windows)
- `./scripts/run.sh bm2` (Linux n IOS)
