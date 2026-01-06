# Bench Mark 4 - isPalindrome (index off by one bug)

## What it does
`Program.isPalindrome(String s)` checks whether a string is a palindrome, case-insensitively.
Spaces/punctuation are treated as normal characters.

## Bug
The buggy version sets `j = s.length()` and then calls `s.charAt(j)`, which throws
`StringIndexOutOfBoundsException` for any non-empty string.

## Expected fix
Initialize `j` as `s.length() - 1` and compare characters while `i < j`.

## Tests
Tests are in `tests/ProgramTest.java`.
- `evenLengthPalindrome_isTrue()` fails on `buggy` (throws) and passes on `fixed`.
- Other tests pass on both versions (depending on which tests are enabled).

## Fault localization
`faultloc.json` provides suspicious line weights for `Program.java` (not perfect localization).

## How to run
From the project root:
- `scripts/run.cmd bm4` (Windows)
- `./scripts/run.sh bm4` *(if available in repo)*
