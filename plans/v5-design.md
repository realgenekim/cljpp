# CLJPP v5 Design: v1 + Better #() Examples

**Date:** 2025-11-06
**Based on:** v1 failures (80% → target 85%+)

## Key Insight: POP-ALL Makes Things Worse!

**The data:**
- v1 (explicit POP, NO POP-ALL): **80%** ✅ WINNER
- v2 (with POP-ALL): 75%
- v3 (with POP-ALL): 60-75%
- v4 (hybrid): 0%

**Conclusion:** Adding POP-ALL reduces success rate! Stick with explicit POP counting.

## Problem Analysis

**v1 (80%) fails on programs: 04, 11, 13, 17**

Common pattern: **Anonymous functions with #() syntax**
- Programs 04, 13: Use `#()` in filters/predicates
- Program 11: core.async (complex, may be inherent difficulty)
- Program 17: letfn with multi-arity (complex nesting)

**Root cause:** v1's #() examples may not be prominent enough

## v5 Strategy: v1 + Prominent #() Examples (NO POP-ALL!)

### Key Changes from v1

1. **NO POP-ALL** - Keep v1's explicit POP counting (it works!)

2. **Prominent #() handling:**
   - Early in prompt (not buried in examples)
   - Clear rule: "Never write #() - always expand to fn"
   - Multiple examples showing the expansion

3. **Better instruction following:**
   - First line: "OUTPUT CODE ONLY - NO EXPLANATIONS"
   - Show what NOT to do (explanation text example)
   - Reinforce at end

## v5 Prompt Structure

```markdown
# CLJ-PP Generation Guide

**OUTPUT CODE ONLY - Start immediately with PUSH-( - NO explanations!**

## Core Syntax

PUSH-(  PUSH-[  PUSH-{  # Open containers
POP                      # Close one container - count them!

## THE GOLDEN RULE: ONE POP PER PUSH

Count opening containers → emit exactly that many POPs.

## CRITICAL: Anonymous Functions

**NEVER write `#(...)` syntax in CLJPP!**

❌ WRONG: `#(> % 5)`
✅ RIGHT: `PUSH-( fn PUSH-[ % POP PUSH-( > % 5 POP POP`

❌ WRONG: `#(> (:age %) 18)`
✅ RIGHT: `PUSH-( fn PUSH-[ x POP PUSH-( > PUSH-( :age x POP 18 POP POP`

**Every `#()` must expand to full `fn` form with PUSH/POP.**

## Examples

### Example 1: Single Function (POP-ALL at end)
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
  POP
POP-ALL  ← Closes defn
```

### Example 2: Multiple Top-Level Forms
```clojure
PUSH-( ns examples.program POP

PUSH-( def users
  PUSH-[
    PUSH-{ :name "Alice" :age 25 POP
    PUSH-{ :name "Bob" :age 30 POP
  POP
POP

PUSH-( defn active-users PUSH-[ users POP
  PUSH-( filter PUSH-( fn PUSH-[ u POP
    PUSH-( > PUSH-( :age u POP 18 POP
  POP POP users POP
POP

PUSH-( defn user-names PUSH-[ users POP
  PUSH-( map :name users POP
POP-ALL  ← ONLY ONE POP-ALL, at the very end!
```

### Example 3: Complex Nested (shows explicit POP counting)
```clojure
PUSH-( ns examples.program POP

PUSH-( defn query PUSH-[ data POP
  PUSH-( let PUSH-[
    result PUSH-( filter PUSH-( fn PUSH-[ x POP
      PUSH-( > x 5 POP
    POP POP data POP
  POP
    PUSH-( map PUSH-( fn PUSH-[ x POP PUSH-( * x 2 POP POP result POP
  POP
POP-ALL
```

## Common Errors

❌ **WRONG - Using POP-ALL after each form:**
```clojure
PUSH-( ns ... POP-ALL  ← Empties stack!
PUSH-( def ... POP-ALL ← Stack underflow!
```

❌ **WRONG - Writing explanation first:**
```
Let me write this in CLJ-PP format...

PUSH-( ns ...
```

✅ **RIGHT - Start immediately, POP-ALL once at end:**
```clojure
PUSH-( ns ... POP
PUSH-( def ... POP
PUSH-( defn ... POP-ALL
```

## Quick Rules

1. **ONE POP PER PUSH** (except the last form)
2. **POP-ALL = End of File** (use once, at the very end)
3. **Start immediately** with PUSH-( - no explanations
4. **Multiple forms** → each gets POP, last gets POP-ALL

**Start your code NOW with PUSH-(**
```

## Expected Improvements

**v3 failures to fix:**
- Program 01: No explanation text → should pass
- Program 04: No POP-ALL after ns → should pass
- Program 19: Only one POP-ALL at end → should pass

**Target:** 85%+ (17/20)

## Why This Should Work

1. **Clearer semantics** - "End of file" is unambiguous
2. **Explicit multi-form pattern** - Shows the right way
3. **Negative examples** - Shows what NOT to do
4. **Stronger enforcement** - First line says "OUTPUT ONLY CODE"

## Risk Analysis

**Potential issues:**
- LLMs might still ignore "no explanation" rule (saw with v3, v4)
- Might forget POP-ALL at the very end
- Might use too many regular POPs

**Mitigation:**
- Make "no explanation" the FIRST LINE (immediate priming)
- Show multiple complete file examples
- Emphasize "POP-ALL = END OF FILE" repeatedly
