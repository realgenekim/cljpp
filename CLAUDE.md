# CLJ-PP Tokenizer Project

This project implements a stack-based intermediate format for generating Clojure code.

## Commands

```bash
# Run tests
make runtests-once

# Build the CLJPP prompt from plans/
./build-prompt.sh

# Test a single variant on specific programs
bb bin/test-variant.clj v4 3 10          # Test v4 on program 3, 10 times
bb bin/test-variant.clj v4 all 1         # Test v4 on all programs, once
bb bin/test-variant.clj pop 4 5          # Test explicit POP on program 4, 5 times

# Run comprehensive test (all variants, all programs, parallel)
bb bin/run-comprehensive-parallel.clj
```

## File Organization

- `src/` - Source code (tokenizer, assembler)
- `test/` - Unit tests
- `experiments/` - All experiment runs (timestamped, never overwritten)
- `plans/` - Prompt design documents and analysis
- `CLJPP-PROMPT-v*.md` - Generated prompts for LLM testing

## Prompt Development

The current prompt is **v4** (Minimal + Dense Examples + Clojure Leverage):
- Built from: `plans/variant-hybrid-minimal-dense.md`
- Key insight: Leverage Clojure training data (95% success) with explicit examples
- Critical fix: Teaches `#()` expansion to avoid syntax mixing errors
- See `experiments/EXPERIMENT-RESULTS.md` for performance data

## Testing Workflow

```bash
# Test v4 on programs that previously failed (#() confusion)
bb bin/test-variant.clj v4 4 5    # Program 4: Collections with #() filters
bb bin/test-variant.clj v4 13 5   # Program 13: Spec with #() predicates

# Test v4 on all 20 programs (comprehensive)
bb bin/test-variant.clj v4 all 1

# Compare variants on a specific program
bb bin/test-variant.clj pop 3 10
bb bin/test-variant.clj v4 3 10
```

## Available Variants

- `clj` - Regular Clojure (baseline, 95% success)
- `pop` - CLJ-PP with explicit POP counting (80% success)
- `popall` - CLJ-PP with POP-ALL v2 (75% success)
- `v3` - CLJ-PP with POP-ALL v3 (60% success)
- `v4` - CLJ-PP v4 Hybrid (untested - this is what we're testing!)
