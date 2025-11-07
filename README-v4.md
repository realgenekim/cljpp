# CLJPP Prompt v4: Quick Start

## What We Built

A new CLJPP prompt that applies **the bitter lesson**: leverage the LLM's excellent Clojure training (95% success) instead of fighting it.

## Files Created

- `CLJPP-PROMPT-v4.md` - The new prompt (6.1 KB, 15 examples)
- `plans/variant-hybrid-minimal-dense.md` - Design doc with analysis
- `plans/SUMMARY-v4-design.md` - Executive summary
- `.bd/test-prompt-v4.md` - Testing task
- `build-prompt.sh` - Build script

## Key Changes from v1 (80% success)

1. **Explicitly leverages Clojure knowledge:**
   - Opens with "You're great at Clojure (95%)"
   - Frames CLJPP as "Clojure with explicit delimiters"

2. **Fixes #() confusion (programs 04, 13):**
   - Shows explicit expansion: `#(> % 5)` → `PUSH-( fn PUSH-[ % POP PUSH-( > % 5 POP POP`
   - Multiple examples of anonymous function translation

3. **Dense examples (15 total):**
   - Covers all observed failure modes
   - Trusts in-context learning (bitter lesson)
   - Pattern transformation approach

4. **Minimal rules:**
   - ONE POP PER PUSH
   - No complex decision trees

## How to Test

```bash
# Test v4 on specific failing programs
bb bin/test-variant.clj v4 4 5     # Program 4: Collections with #() (5 iterations)
bb bin/test-variant.clj v4 13 5    # Program 13: Spec with #() (5 iterations)

# Test v4 on all programs (comprehensive)
bb bin/test-variant.clj v4 all 1

# Compare with baseline on specific program
bb bin/test-variant.clj pop 3 10
bb bin/test-variant.clj v4 3 10

# Full parallel test (all variants)
bb bin/run-comprehensive-parallel.clj
```

## Success Criteria

- **Best:** 19/20 (95%) - match pure Clojure
- **Good:** 17-18/20 (85-90%) - fix #() errors + improve
- **Accept:** 16/20 (80%) - no regression
- **Fail:** < 16/20 - revert to v1

## Expected Impact

**Programs likely to improve:**
- 04: Collections with `#()` filters
- 13: Spec with `#()` predicates

**Programs may improve:**
- 17: Complex letfn (better examples)

**Programs unlikely to change:**
- 1-3, 6-12, 14-16, 18-20 (already passing)

## The Big Picture

**Old hypothesis:** LLMs can't count delimiters → need explicit stack ops
**New hypothesis:** LLMs pattern-match, not count → help them match CLJPP to Clojure patterns

**The bitter lesson:**
> "The biggest lesson that can be read from 70 years of AI research is that general methods that leverage computation are ultimately the most effective."
> — Rich Sutton

Applied here: Leverage the LLM's Clojure training (computation/scale) rather than hand-crafted counting rules.

## Next Steps

1. Run the comprehensive test
2. Analyze which programs still fail
3. Update EXPERIMENT-RESULTS.md
4. Decide: iterate, adopt, or revert

See `.bd/test-prompt-v4.md` for detailed testing plan.
