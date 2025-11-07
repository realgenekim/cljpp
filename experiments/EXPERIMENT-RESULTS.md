# POP-LINE and POP-ALL Experiment Results

**Date:** 2025-11-06
**Program:** Factorial/Fibonacci (recursive with cond)
**Iterations:** 10 each

## Context: Regular Clojure Performance

**Regular Clojure (fresh instances, same 20 programs):** **80% success** (16/20)
- Source: `test-output-clj-round2/fresh-experiment-results.md`
- Failed on: complex destructuring, core.async, tool-usage attempts (2 programs)

**This is our baseline to beat.**

## Results Summary

| Approach | Success Rate | Transpile Errors | Load Errors |
|----------|--------------|------------------|-------------|
| **Regular Clojure (fresh)** | **80%** (16/20) | N/A | 4 failures |
| Baseline CLJ-PP (explicit POP) | **90%** (9/10) | 1 | 0 |
| Enhanced CLJ-PP (POP-LINE/ALL) | **80%** (8/10) | 1 | 1 |

**Key finding:** Baseline CLJ-PP (90%) beats both regular Clojure (80%) and enhanced CLJ-PP (80%)!

## Analysis

### Baseline Failures (1/10)

**iter7**: Claude included explanatory text before the code
```clojure
I'll write the recursive factorial and fibonacci functions in CLJ-PP format.

PUSH-( ns examples.test7 POP
```
**Error:** "Atom at top-level" - the text before PUSH-( was treated as loose atoms.

**Root cause:** Prompt instruction issue, not syntax issue.

### Enhanced Failures (2/10)

**iter7**: Logical error in fibonacci
```clojure
:else PUSH-( + PUSH-( fibonacci PUSH-( - n 1 POP-LINE PUSH-( fibonacci PUSH-( - n 2 POP-ALL
```

**Transpiled to:**
```clojure
:else (+ (fibonacci (- n 1))) (fibonacci (- n 2))
```

**Error:** POP-LINE closed the `+` too early! The second fibonacci call ended up OUTSIDE the `+`.

**Root cause:** POP-LINE closed `+`, `fibonacci`, and `-` from that line, but the `+` shouldn't have been closed yet because there's more content (the second fibonacci on the NEXT token).

**iter8**: Same as baseline - included text without PUSH-(
```clojure
ns examples.test8

PUSH-( defn factorial...
```

**Error:** "Atom at top-level" - `ns` and `examples.test8` treated as loose atoms.

**Root cause:** Same prompt instruction issue as baseline.

## Key Findings

### 1. POP-LINE Can Create Logic Errors

**The Problem:**
```clojure
:else PUSH-( +
  PUSH-( fibonacci PUSH-( - n 1 POP-LINE    ← Closes -, fibonacci, AND +!
  PUSH-( fibonacci PUSH-( - n 2 POP-ALL     ← Second fibonacci is now orphaned!
```

**What LLM thought:** "I'm done with this line → POP-LINE"

**What actually happened:** POP-LINE closed ALL containers opened on that line, including the `+` that was supposed to stay open for the next line!

**Correct approach would be:**
```clojure
:else PUSH-( +
  PUSH-( fibonacci PUSH-( - n 1 POP-LINE    ← Should be POP POP (only close -, fibonacci)
  PUSH-( fibonacci PUSH-( - n 2 POP-LINE    ← Then POP-LINE here closes -, fibonacci
POP                                          ← Then POP closes +
POP                                          ← Then POP closes cond
POP                                          ← Then POP closes defn
```

OR:
```clojure
:else PUSH-( +
  PUSH-( fibonacci PUSH-( - n 1 POP POP     ← Explicit: close -, fibonacci
  PUSH-( fibonacci PUSH-( - n 2 POP-ALL     ← Then close everything
```

### 2. POP-LINE is Ambiguous for Multi-Line Forms

**The ambiguity:**
```clojure
PUSH-( +                           ← Line 1: opens +
  PUSH-( fibonacci ... POP-LINE    ← Line 2: opens fibonacci, -
```

When you write `POP-LINE` on line 2, it closes ALL containers from line 2 (`fibonacci`, `-`), but what about the `+` from line 1?

**Answer:** POP-LINE only closes containers opened on THE SAME LINE. It doesn't touch the `+`.

**The problem:** Fresh instances don't understand this nuance! They think "I'm done with this line" means "close everything I need to close to finish this line" - which would include the `+`.

### 3. POP-ALL is Safer (But More Extreme)

POP-ALL has no ambiguity - it closes EVERYTHING. The only question is: "Am I completely done?"

**Success pattern:**
```clojure
:else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```
This works because the entire form IS done on this line.

**Failure pattern:**
```clojure
:else PUSH-( +
  PUSH-( fibonacci PUSH-( - n 1 POP-LINE  ← Wrong! + spans multiple lines
```

### 4. Success Rate Comparison

**Surprising result:** Baseline (explicit POP) performed BETTER than enhanced (POP-LINE/ALL)!

**Why?**
1. Explicit POP counting is **unambiguous** - you count PUSHes, emit that many POPs
2. POP-LINE introduces **scope ambiguity** - what counts as "this line"?
3. Fresh instances without context struggled with the decision tree

**Counter-intuitive lesson:** More options → more confusion!

## Recommendations

### For Future Prompts

1. **Emphasize POP-LINE scope:** "POP-LINE ONLY closes containers opened on THIS line - not parent containers from previous lines!"

2. **Warn about multi-line forms:**
```markdown
❌ WRONG:
:else PUSH-( +
  PUSH-( fibonacci ... POP-LINE  ← Doesn't close + (it's from previous line!)

✅ RIGHT:
:else PUSH-( +
  PUSH-( fibonacci ... POP POP   ← Explicit: close fibonacci, then +
```

3. **Keep it simple:** Maybe POP-ALL is enough? POP-LINE adds cognitive load.

### Alternative: POP-ALL Only?

**Proposal:** Just add POP-ALL, skip POP-LINE entirely.

**Rationale:**
- POP-ALL is unambiguous: "close everything"
- POP is precise: "close one thing"
- POP-LINE is ambiguous: "close some things based on line scope"

**Decision tree becomes trivial:**
```
Am I completely done with this entire form?
├─ YES → POP-ALL
└─ NO → POP (one at a time)
```

## Conclusion

**Result:** Baseline (explicit POP) achieved **90% success**, Enhanced (POP-LINE/ALL) achieved **80% success**.

**Hypothesis rejected:** POP-LINE and POP-ALL did NOT improve success rate - they actually hurt it by 10 percentage points.

**Why:** The scope ambiguity of POP-LINE ("which containers count as 'this line'?") introduced new error modes.

**Next steps:**
1. Test POP-ALL only (without POP-LINE)
2. Improve prompts to clarify POP-LINE scope
3. Consider: maybe explicit counting (baseline) is already good enough?

---

**Lesson learned:** More features ≠ better. The explicit POP approach, while requiring counting, is **unambiguous**. POP-LINE added ambiguity that fresh instances couldn't navigate.
