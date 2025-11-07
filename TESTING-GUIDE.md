# CLJPP Testing Guide

## Quick Reference

### Test a Single Variant

```bash
# Test v4 on program 3, 10 times
bb bin/test-variant.clj v4 3 10

# Test v4 on all programs, once each
bb bin/test-variant.clj v4 all 1

# Test on programs that previously failed
bb bin/test-variant.clj v4 4 5    # #() confusion in filters
bb bin/test-variant.clj v4 13 5   # #() confusion in spec
```

### Compare Variants

```bash
# Compare pop vs v4 on program 3
bb bin/test-variant.clj pop 3 10
bb bin/test-variant.clj v4 3 10

# Then compare results in experiments/
```

### Run Comprehensive Tests

```bash
# All variants, all programs, parallel
bb bin/run-comprehensive-parallel.clj

# Results in: experiments/comprehensive-test-parallel-<timestamp>/
```

## Available Variants

| Variant | Description | Success Rate | Prompt File |
|---------|-------------|--------------|-------------|
| `clj` | Regular Clojure (baseline) | 95% (19/20) | None |
| `pop` | Explicit POP counting | 80% (16/20) | `CLJPP-PROMPT.md` |
| `popall` | POP-ALL v2 | 75% (15/20) | `CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md` |
| `v3` | POP-ALL v3 (worse!) | 60% (12/20) | `CLJPP-PROMPT-WITH-POP-ALL-ONLY-v3.md` |
| `v4` | **Hybrid (NEW!)** | **untested** | `CLJPP-PROMPT-v4.md` |

## Understanding Results

### Output Files

For each test run, you'll get:
```
experiments/test-variant-<variant>-<timestamp>/
  01-v4-iter01.raw       # Raw LLM output
  01-v4-iter01.cljpp     # Cleaned CLJPP code
  01-v4-iter01.clj       # Transpiled Clojure (if CLJPP)
```

### Success/Failure

- ✓ SUCCESS - Code generated, transpiled (if needed), and executed
- ❌ TRANSPILE ERROR - CLJPP syntax error
- ❌ EXECUTION ERROR - Code runs but fails
- ❌ ERROR - Other error (LLM call failed, etc.)

## Example Workflow: Testing v4

```bash
# 1. Quick sanity check on program 3 (factorial)
bb bin/test-variant.clj v4 3 5

# 2. Test on known failure cases
bb bin/test-variant.clj v4 4 5    # #() in filters
bb bin/test-variant.clj v4 13 5   # #() in spec

# 3. If those look good, test all programs
bb bin/test-variant.clj v4 all 1

# 4. Update EXPERIMENT-RESULTS.md with findings

# 5. Decide: adopt v4, iterate, or revert
```

## Expected v4 Results

**Hypothesis:**
- Programs 04, 13 should improve (explicit #() examples)
- Other programs should maintain 80%+ baseline
- Best case: 19/20 (95%) - match pure Clojure!

**Decision criteria:**
- > 85% → Adopt v4 as new baseline
- 80-85% → Iterate on remaining failures
- < 80% → Revert to v1 (explicit POP counting)

## Troubleshooting

**"Command not found: claude"**
- Need Claude CLI installed

**"File does not exist: bin/cljpp.clj"**
- Wrong directory - run from project root

**"Unknown variant: xxx"**
- Check available variants with `--help`
- Valid: clj, pop, popall, v3, v4

## See Also

- `README-v4.md` - Full v4 design rationale
- `plans/SUMMARY-v4-design.md` - Executive summary
- `experiments/EXPERIMENT-RESULTS.md` - Historical results
- BD issue: `cljp-tokenizer-1`
