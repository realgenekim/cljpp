# v5 Failure Analysis

**Result:** 11/20 (55%) - WORSE than v1 (80%), v2 (75%), v3 (75%)

**Date:** 2025-11-06

## The Catastrophic Failure

v5 was designed to be "v1 + prominent #() examples" but achieved **only 55%** - significantly worse than all previous variants!

## What v5 Changed from v1

1. **Removed motivation section** - v1 explained "why CLJ-PP exists" (delimiter problem)
2. **Shortened to "concise guide"** - v1 had more context and explanation
3. **Added prominent #() section** - This was the goal
4. **Removed mental models** - v1 had "write contents then pop" pattern
5. **Made it more "reference card"** style vs v1's "teaching" style

## Common Failure Pattern: POP Miscounting

**Example: Program 04, lines 12-16**
```clojure
PUSH-( defn active-users PUSH-[ users POP
  PUSH-( filter PUSH-( fn PUSH-[ user POP
    PUSH-( > PUSH-( :age user POP 18 POP
  POP POP users POP  ← ERROR: 3 POPs here
POP                   ← Tries to close defn, but stack already empty!
```

**Manual trace:**
- Line 12: Open `defn` (→ stack depth 1), open `[` (→ depth 2), close `[` with POP (→ depth 1)
- Line 13: Open `filter` (→ depth 2), open `fn` (→ depth 3), open `[` (→ depth 4), close `[` (→ depth 3)
- Line 14: Open `>` (→ depth 4), open `:age` call (→ depth 5), close `:age` (→ depth 4), close `>` (→ depth 3)
- Line 15: Close `fn` with first POP (→ depth 2), close `filter` with second POP (→ depth 1), **then third POP closes `defn` (→ depth 0)**
- Line 16: **POP with empty stack → ERROR**

**The issue:** Line 15 should have **2 POPs** (close fn, close filter), line 16 should close defn. Instead, line 15 has 3 POPs and empties the stack.

## Transpile Errors (7/9 failures)

**"POP with empty stack" failures:**
- Program 04 (line 16): Extra POP in filter expression
- Program 10 (line 37): Extra POP
- Program 11 (line 21): Extra POP
- Program 13 (line 16): Extra POP
- Program 15 (line 44): Extra POP in letfn
- Program 16 (line 10): Extra POP
- Program 19 (line 44): Extra POP

**Pattern:** LLM is adding too many POPs on the "closing line" of a form.

## Execution Errors (2/9 failures)

**Program 12:** Invalid syntax - stateful transducer generated incorrectly
```
Could not resolve symbol: result
```

**Program 17:** Invalid letfn syntax - extra parens around binding form
```
Unsupported binding form: (helper [n] (when (< n end) ...))
```

Both are semantic errors in the generated Clojure, not POP-counting issues.

## Why v5 Failed

### Hypothesis 1: Lost Context
v1's longer "teaching" style with motivation ("why delimiter counting is hard") may have helped the LLM understand the task better. v5's concise "reference card" style may have been too terse.

### Hypothesis 2: Prominent #() Examples Created Confusion
Adding the early, prominent #() section may have disrupted the prompt flow. The LLM may have focused on #() expansion at the expense of basic POP counting.

### Hypothesis 3: Removed Mental Models
v1 had explicit sections like:
- "Mental Model: Write Contents THEN Pop"
- "Think: what container am I opening?"
- Detailed nesting examples

v5 removed these in favor of concise rules. This may have hurt.

### Hypothesis 4: Wrong Examples
v5's examples may have inadvertently shown patterns that confused the LLM. For instance, showing complex nested examples early might have set wrong expectations.

## Key Data Point

**What stayed the same:**
- "ONE POP PER PUSH" rule
- Basic syntax (PUSH-( PUSH-[ PUSH-{ POP)
- #() expansion examples (now more prominent)

**What got worse:**
- Basic POP counting (the core skill!)
- 7 programs failed on transpile (all POP miscounting)
- Only 2 failed on execution (semantic errors)

This suggests v5's changes **harmed the LLM's ability to count POPs correctly**.

## Comparison to Other Variants

| Variant | Success Rate | Notes |
|---------|-------------|-------|
| **clj** (baseline) | 95% (19/20) | Pure Clojure, no CLJPP |
| **v1** (explicit POP) | **80%** (16/20) | Original CLJPP-PROMPT.md |
| v2 (POP-ALL) | 75% (15/20) | Added POP-ALL feature |
| v3 (POP-ALL v3) | 60-75% | POP-ALL confusion |
| v4 (hybrid) | **0%** (0/20) | Priming disaster |
| **v5** (v1 + #()) | **55%** (11/20) | **WORST CLJPP variant** |

## Conclusion

**v5 made things worse, not better.**

The prominent #() examples and concise "reference" style harmed basic POP counting ability. The LLM needs more context, mental models, and teaching - not just rules and examples.

## Next Steps

**Don't pursue v5 further.** The data is clear:
- v1 (80%) remains the best CLJPP variant
- v5's changes backfired dramatically
- Adding more examples/rules doesn't help if it disrupts the teaching flow

**Recommendation:** Stick with v1, focus on understanding why programs 04, 11, 13, 17 fail with v1, rather than trying new prompt variants.
