# POP-ALL v2 Test Results

**Date:** Thu Nov  6 19:07:47 PST 2025
**Iterations:** 20

## Results

- Success: 0/20 (0%)
- Transpile errors: 20
- Load errors: 0

## Comparison

| Approach | Success Rate |
|----------|--------------|
| CLJ-PP Baseline (explicit POP) | 90% (9/10) |
| CLJ-PP + POP-ALL v1 | 80% (8/10) |
| CLJ-PP + POP-ALL v2 | 0% (0/20) |

## Hypothesis

POP-ALL v2 improved the prompt with:
- Explicit "THE CRITICAL RULE" section
- WRONG pattern examples with ❌
- "POP-ALL means STOP" repeated throughout
- Dedicated error section for "POPs After POP-ALL"

Goal: Achieve 90%+ success rate (matching baseline).
