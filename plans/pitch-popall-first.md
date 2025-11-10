# CLJ-PP Pitch Variants: POP-ALL First Approach

## Hypothesis
"Counting POPs is as bad as counting parens. Lead with POP-ALL to reduce counting entirely."

## Challenge
Previous experiments show POP-ALL made things worse (v2: 75%, v3: 60% vs v1: 80%).
Fresh instances used it too aggressively, closing parent forms prematurely.

## Strategy
Introduce POP-ALL EARLY but with CRYSTAL-CLEAR heuristics for when to use it.

---

## Variant POP1: "Two Simple Rules"

```markdown
# CLJ-PP: Two Rules, No Counting

## The Problem You Know

You're generating Clojure. Deep nesting. End of the form. "How many closes do I need?"

```clojure
(* n (factorial (- n 1)))  ← count backwards... 3? 4?
```

You're vibing it.

## The Solution: Two Simple Rules

CLJ-PP has **four operations** but **two rules**:

### Operations
```clojure
PUSH-(  # Open list
PUSH-[  # Open vector
PUSH-{  # Open map
POP-ALL # Done with this entire form
```

### Rule 1: When You're Completely Done
```clojure
PUSH-( defn factorial PUSH-[ n POP-ALL
```
✅ "I'm completely done with the parameter vector."

```clojure
PUSH-( if PUSH-( <= n 1 POP-ALL
```
✅ "I'm completely done with this condition."

```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```
✅ "I'm completely done with this entire recursive branch."

**Use POP-ALL when you're finishing a complete sub-expression.**

### Rule 2: When You're Not Sure
If you're NOT completely done (still have more at this level), use explicit `POP`:

```clojure
PUSH-( defn factorial PUSH-[ n POP    ← More coming (the body)
  PUSH-( if PUSH-( <= n 1 POP          ← More coming (then/else)
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```

## The Heuristic: "Am I Done Here?"

**Ask yourself:** "Am I moving to the next thing at the same level, or am I completely done?"

**Same level → POP:**
```clojure
PUSH-( let PUSH-[ x 10 POP   ← More coming: the let body
  PUSH-( + x 5 POP-ALL        ← Completely done with let
```

**Completely done → POP-ALL:**
```clojure
PUSH-( defn foo PUSH-[ a b POP
  PUSH-( + a b POP-ALL   ← Done with defn body, done with defn
```

## Examples

### Example 1: Factorial
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP-ALL
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```

**Breakdown:**
- `PUSH-[ n POP` - NOT done, body coming
- `PUSH-( <= n 1 POP-ALL` - DONE with condition
- `PUSH-( - n 1 POP-ALL` - DONE with entire else branch (closes everything)

### Example 2: Hiccup
```clojure
PUSH-[ :div.card
  PUSH-[ :h3 name POP-ALL
  PUSH-[ :p email POP-ALL
POP-ALL
```

**Each element is complete → POP-ALL**

### Example 3: Let Binding
```clojure
PUSH-( let PUSH-[
  x 10
  y 20
POP
  PUSH-( + x y POP-ALL
```

**Breakdown:**
- Bindings vector `POP` - NOT done, body coming
- Body `POP-ALL` - DONE with entire let

## When NOT To Use POP-ALL

❌ **WRONG - Inside let bindings:**
```clojure
PUSH-( let PUSH-[
  x PUSH-{ :a 1 POP-ALL   ← BAD! Closes let too!
```

✅ **RIGHT:**
```clojure
PUSH-( let PUSH-[
  x PUSH-{ :a 1 POP       ← Just close the map
POP                       ← Close bindings vector
  body...
POP-ALL                   ← NOW close the entire let
```

❌ **WRONG - Middle of function:**
```clojure
PUSH-( defn foo PUSH-[ x POP
  PUSH-( println x POP-ALL   ← Closes println AND defn!
  PUSH-( inc x POP-ALL       ← ERROR: defn already closed!
```

✅ **RIGHT:**
```clojure
PUSH-( defn foo PUSH-[ x POP
  PUSH-( println x POP       ← Just close println
  PUSH-( inc x POP-ALL       ← DONE with defn body
```

## The Key Insight

**POP-ALL means "I'm completely done with this form AND everything containing it."**

If there's more content coming at the current level, use `POP`.

If you're truly done with the entire structure, use `POP-ALL`.

## Mental Model

Think of code generation as a checklist:

```
Need factorial function?
  - PUSH-( defn factorial
  - Need parameters? PUSH-[ n
  - Done with parameters? POP (body coming!)
  - Need if? PUSH-( if
  - Need condition? PUSH-( <= n 1
  - Done with condition? POP-ALL (then/else coming!)
  - Base case: 1
  - Need recursion? PUSH-( * n PUSH-( factorial PUSH-( - n 1
  - Done with recursion? POP-ALL (done with everything!)
```

## Advanced: The "Completely Done Test"

Before using POP-ALL, ask:
1. Am I done with this form? (Yes)
2. Is there more content at this level? (No)
3. Am I moving to a sibling or finishing the parent? (Finishing)

All three "yes/no/finishing" → POP-ALL

Any other combo → POP

## Summary

**Two rules:**
1. Completely done? → `POP-ALL`
2. More coming? → `POP`

**No counting. Just "am I done?"**

Purpose-built for autoregressive LLMs who want to focus on logic, not delimiter arithmetic.
```

---

## Variant POP2: "The Three Levels"

```markdown
# CLJ-PP: Think In Three Levels, Not Delimiters

## The Problem

```clojure
(defn factorial [n]
  (if (<= n 1)
    1
    (* n (factorial (- n 1)))))
```

At the end: How many `)`? Count backwards through your token stream...

## The CLJ-PP Approach: Three Levels

Instead of counting delimiters, think in three levels:

### Level 1: Opening (You're Good At This)
```clojure
PUSH-(  # Need a list
PUSH-[  # Need a vector
PUSH-{  # Need a map
```

### Level 2: Content (You're Already Great At This)
```
defn factorial   # Symbols, keywords, values
x 10            # Atoms, literals
```

### Level 3: Closing (This Is Where CLJ-PP Helps)

**Two ways to close, based on one question: "Am I completely done?"**

#### Option A: Done With This Specific Thing (More Coming)
```clojure
PUSH-[ n POP    ← Done with params, body coming
```

#### Option B: Completely Done (Nothing More At This Level)
```clojure
PUSH-( + a b POP-ALL   ← Done with this entire expression
```

## The Decision Tree

```
At closing time, ask ONE question:
  "Am I completely done with this form?"

  YES → POP-ALL
  NO (more content coming) → POP
```

That's it. One decision. No counting.

## Examples With Decision Points

### Example 1: Simple Function

```clojure
PUSH-( defn add PUSH-[ a b POP
                              ↑
                              Q: Done with params?
                              A: No, body coming → POP
  PUSH-( + a b POP-ALL
                   ↑
                   Q: Done with body?
                   A: Yes, completely → POP-ALL
```

### Example 2: Factorial

```clojure
PUSH-( defn factorial PUSH-[ n POP
                                  ↑
                                  Q: Done with params?
                                  A: No, body coming → POP

  PUSH-( if PUSH-( <= n 1 POP-ALL
                              ↑
                              Q: Done with condition?
                              A: Yes → POP-ALL (then/else coming, but condition is done)

    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
                                                  ↑
                                                  Q: Done with entire recursion?
                                                  A: Yes, completely → POP-ALL
```

### Example 3: Hiccup With Conditional

```clojure
PUSH-[ :div.card
  PUSH-[ :h3 name POP-ALL
                      ↑
                      Q: Done with h3 element?
                      A: Yes → POP-ALL

  PUSH-( when verified?
    PUSH-[ :span.badge "✓" POP-ALL
                           ↑
                           Q: Done with span?
                           A: Yes → POP-ALL
  POP-ALL
      ↑
      Q: Done with when?
      A: Yes → POP-ALL

  PUSH-[ :p email POP-ALL
POP-ALL
    ↑
    Q: Done with entire div?
    A: Yes → POP-ALL
```

## What "Completely Done" Means

**Completely done = No more content at this level OR any parent level you care about**

Example:
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```

`POP-ALL` closes:
- The `(- n 1)` ✓
- The `(factorial ...)` ✓
- The `(* ...)` ✓

Because you're done with ALL of them.

## Common Mistake: POP-ALL In The Middle

❌ **WRONG:**
```clojure
PUSH-( defn foo PUSH-[ x POP
  PUSH-( println x POP-ALL    ← Closes defn too!
  PUSH-( inc x POP-ALL        ← ERROR: defn already closed!
```

The problem: You weren't "completely done" after `println` - there was more content coming (`inc x`).

✅ **RIGHT:**
```clojure
PUSH-( defn foo PUSH-[ x POP
  PUSH-( println x POP        ← Just close println
  PUSH-( inc x POP-ALL        ← NOW completely done
```

## The Mental Shift

**Regular Clojure:**
- "I need 3 closes here... count backwards... `)))` probably?"

**CLJ-PP (counting approach):**
- "I opened 3, so `POP POP POP`"

**CLJ-PP (POP-ALL approach):**
- "Am I completely done? Yes → `POP-ALL`"
- "Am I completely done? No → `POP`"

**One decision vs arithmetic.**

## Quick Reference

```clojure
# Done with params, body coming
PUSH-[ params POP

# Done with bindings, body coming
PUSH-[ bindings POP

# Done with this complete element
PUSH-[ :h1 title POP-ALL

# Done with entire function call
PUSH-( + a b POP-ALL

# Done with entire function body
PUSH-( defn foo PUSH-[ x POP
  PUSH-( * x 2 POP-ALL
```

## The Core Rule

**Ask ONE question: "Am I completely done?"**

- Completely done → `POP-ALL`
- More coming → `POP`

No counting. Just one binary decision.

Built for autoregressive LLMs who want to think forward, not count backwards.
```

---

## Variant POP3: "Explicit POP-ALL Strategy"

```markdown
# CLJ-PP: Default to POP-ALL, Explicit POP Only When Needed

## Flip The Mental Model

**Old way:** Count delimiters
**v1 CLJ-PP:** Count POPs
**NEW way:** Default to POP-ALL, use POP only for "more coming"

## The Core Strategy

```
Every form ends with POP-ALL...
...UNLESS there's more content coming at the same level.
```

## Examples: The Pattern

### Pattern 1: Function Definition
```clojure
PUSH-( defn add PUSH-[ a b POP    ← Exception: body coming
  PUSH-( + a b POP-ALL             ← Default: done!
```

**Why `POP` after params?** Body is coming.
**Why `POP-ALL` after body?** Completely done.

### Pattern 2: Let Binding
```clojure
PUSH-( let PUSH-[ x 10 y 20 POP   ← Exception: body coming
  PUSH-( + x y POP-ALL             ← Default: done!
```

**Why `POP` after bindings?** Body is coming.
**Why `POP-ALL` after body?** Completely done.

### Pattern 3: If Expression
```clojure
PUSH-( if PUSH-( > x 10 POP-ALL   ← Default: condition done!
  PUSH-( * x 2 POP                ← Exception: else coming
  PUSH-( inc x POP-ALL             ← Default: done!
```

**Why `POP-ALL` after condition?** Condition complete, then/else are separate.
**Why `POP` after then?** Else branch coming.
**Why `POP-ALL` after else?** Completely done.

### Pattern 4: Hiccup Elements
```clojure
PUSH-[ :div.card
  PUSH-[ :h3 name POP-ALL          ← Default: element done!
  PUSH-[ :p email POP-ALL          ← Default: element done!
POP-ALL                            ← Default: container done!
```

**All elements complete → all POP-ALL**

### Pattern 5: Nested Calls
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```

**Deep nesting, all done → POP-ALL closes everything**

## The Exceptions (When To Use POP)

Use `POP` in exactly **three cases**:

### Exception 1: After Parameter Vector
```clojure
PUSH-( defn foo PUSH-[ x y POP   ← Body coming!
  body...
```

### Exception 2: After Let Bindings
```clojure
PUSH-( let PUSH-[ bindings POP   ← Body coming!
  body...
```

### Exception 3: After If/When Then-Branch
```clojure
PUSH-( if condition
  then-branch POP                 ← Else coming!
  else-branch POP-ALL
```

**That's it. Three exceptions. Everything else is POP-ALL.**

## Mental Model: The Checklist

```
Writing a form?
1. Open it: PUSH-(
2. Write contents
3. Closing time:
   - Is there more at this level? → POP (rare!)
   - Otherwise → POP-ALL (common!)
```

## Common Patterns Cheat Sheet

```clojure
# defn with body
PUSH-( defn name PUSH-[ args POP body POP-ALL

# let with body
PUSH-( let PUSH-[ bindings POP body POP-ALL

# if with else
PUSH-( if condition-POP-ALL then-POP else-POP-ALL

# Complete function call
PUSH-( fn args... POP-ALL

# Complete hiccup element
PUSH-[ :tag content POP-ALL

# Nested calls (all done)
PUSH-( outer PUSH-( inner x POP-ALL
```

## Why This Works For Autoregressive Generation

You're generating tokens forward. You naturally know when you're "done" with something.

**The question isn't "how many closes do I need?"**

**The question is "is there more coming after this?"**

- More coming → `POP`
- Nothing more → `POP-ALL`

This matches autoregressive thinking: "What's next?"

## Advanced: The "Next Item" Test

Before closing, ask: "What's my next token?"

- Next token is content at same level (another param, another let binding) → POP
- Next token moves to different level or ends → POP-ALL

Example:
```clojure
PUSH-( defn foo PUSH-[ a b POP
                           ↑
                           Next token: body (different level) → POP

  PUSH-( + a b POP-ALL
                   ↑
                   Next token: nothing (done!) → POP-ALL
```

## Summary: The POP-ALL-First Strategy

1. **Default to POP-ALL** (you're usually done)
2. **Use POP** only for three cases:
   - After params (body coming)
   - After bindings (body coming)
   - After then-branch (else coming)
3. **Everything else**: POP-ALL

Less counting. More "am I done?"

Built for autoregressive LLMs.
```

---

## Analysis: Will POP-ALL-First Work Better?

### Theoretical Advantages

1. **Less counting**: "Am I done?" vs "How many did I open?"
2. **Matches autoregressive thinking**: Forward-oriented, "what's next?"
3. **Simpler mental model**: Default behavior + three exceptions

### Theoretical Risks (Based On v2/v3 Failures)

1. **Context understanding**: Fresh instances might not understand "completely done"
2. **Premature closing**: Might use POP-ALL inside nested forms
3. **Scope confusion**: Might not realize they're inside a parent form

### Key Difference From v2/v3

Previous attempts:
- Added POP-ALL as convenience
- Didn't provide clear heuristics
- Said "use it at end of functions" (too vague)

These variants:
- ✅ Lead with POP-ALL
- ✅ Provide explicit decision rules
- ✅ Show clear examples of when NOT to use it
- ✅ Frame it as "default behavior" not "convenience"

### Hypothesis To Test

**POP-ALL-first with explicit heuristics will outperform v1 (80%) IF:**
1. The heuristics are clear enough
2. The examples cover failure modes
3. Fresh instances can apply "am I done?" test

**It will fail IF:**
1. Fresh instances can't understand "completely done with this form"
2. They close parent forms prematurely (like v3)
3. The decision tree is too complex

## Recommendation

Test **Variant POP1 ("Two Simple Rules")** first:
- Clearest heuristics
- Explicit "when NOT to use POP-ALL" section
- Covers the v3 failure modes

If that works, we've found something better than v1!
If it fails, we know counting is actually easier than decision-making for fresh instances.

Ready to build these into actual test prompts? 🚀
