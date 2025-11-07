# Comprehensive CLJ-PP Experiment Results

**Date:** Thu Nov  6 19:44:01 PST 2025
**Test Set:** All 20 programs from test-output-clj-round2/

## Results Summary

| Approach | Success Rate | Percentage |
|----------|--------------|------------|
| Regular Clojure | 19/20 | 95% |
| CLJ-PP (explicit POP) | 16/20 | 80% |
| CLJ-PP (POP-ALL v2) | 15/20 | 75% |

## Changes in This Experiment

1. **Removed CLAUDE.md contamination** - Minimal CLAUDE.md with no CLJ-PP details
2. **Improved POP-ALL prompt (v2)** - Added explicit warnings about POPs after POP-ALL
3. **Tested all 20 programs** - Full test suite instead of just factorial/fibonacci

## Key Improvements in POP-ALL v2 Prompt

- Added "THE CRITICAL RULE FOR POP-ALL" section
- Showed WRONG pattern explicitly (POPs after POP-ALL)
- Emphasized "POP-ALL means STOP"
- Added Error Example 1: "POPs After POP-ALL"
- Made it crystal clear that nothing comes after POP-ALL

## Detailed Logs

See log.txt for full output of all tests.

## Files Generated

- `20-clj.clj` - Regular Clojure output
- `20-cljpp-pop.cljpp` - CLJ-PP with explicit POP
- `20-cljpp-pop.clj` - Transpiled from POP variant
- `20-cljpp-popall.cljpp` - CLJ-PP with POP-ALL v2
- `20-cljpp-popall.clj` - Transpiled from POP-ALL variant
