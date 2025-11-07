# POP-ALL v2 Test Results (with markdown fence removal)

**Date:** Thu Nov  6 19:09:30 PST 2025
**Iterations:** 20

## Results

- Success: 0/20 (0%)
- Transpile errors: 12
- Load errors: 8

## Comparison

| Approach | Success Rate |
|----------|--------------|
| CLJ-PP Baseline (explicit POP) | 90% (9/10) |
| CLJ-PP + POP-ALL v1 | 80% (8/10) |
| CLJ-PP + POP-ALL v2 | 0% (0/20) |

## Test Setup

- Removed CLAUDE.md contamination (minimal version)
- Used improved POP-ALL v2 prompt with explicit warnings
- Post-processed output to remove markdown code fences
- Fresh instances via `claude --print` (20 iterations)

## Hypothesis

POP-ALL v2 improved the prompt with:
- "THE CRITICAL RULE" section warning about POPs after POP-ALL
- WRONG pattern examples with ❌
- "POP-ALL means STOP" repeated throughout
- Dedicated error section

Goal: Achieve 90%+ success rate (matching baseline).

## Post-Processing

Used `sed` to remove markdown code fences:
```bash
sed -e '/^$/d' input.cljpp > output.cljpp
```

This handles Claude Code's tendency to wrap output in markdown formatting.
