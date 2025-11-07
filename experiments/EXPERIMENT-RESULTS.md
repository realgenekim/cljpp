# CLJ-PP Experiment Results

**Date:** 2025-11-06
**Last updated:** 2025-11-06 21:04 (v5 test complete - CATASTROPHIC FAILURE)

---

## 🏆 FINAL RESULTS: All Variants Ranked (20 Diverse Programs)

**Test:** 20 different programs, 1 fresh Claude instance each → Tests generalization

| Rank | Approach | Success Rate | Prompt File | Notes |
|------|----------|--------------|-------------|-------|
| **🥇** | **Regular Clojure** | **19/20 (95%)** ✅ | *(standard Clojure)* | **Winner!** Best on diverse programs |
| **🥈** | **CLJ-PP (explicit POP)** | **16/20 (80%)** ✅ | `CLJPP-PROMPT.md` | **Best CLJ-PP variant** - tedious but unambiguous |
| 🥉 | CLJ-PP (POP-ALL v2) | 15/20 (75%) | `CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md` | Overfitted to factorial/fibonacci |
| 4 | CLJ-PP (POP-ALL v3) | **12/20 (60%)** ❌ | `CLJPP-PROMPT-WITH-POP-ALL-ONLY-v3.md` | **WORSE than v2** - more rules backfired |
| 5 | CLJ-PP v5 (v1 + #() fix) | **11/20 (55%)** 🚨 | `CLJPP-PROMPT-v5.md` | **CATASTROPHIC** - Broke basic POP counting! |
| 6 | CLJ-PP v4 (Hybrid) | **0/5 (0%)** 💀 | `CLJPP-PROMPT-v4.md` | **TOTAL FAILURE** - Wrote Clojure instead of CLJPP |

**🚨 CRITICAL FINDINGS:**

1. **Regular Clojure wins** at 95% - better delimiter balancing than CLJ-PP overall
2. **Explicit POP counting** (80%) is the best CLJ-PP approach - **v1 REMAINS UNBEATEN**
3. **POP-ALL doesn't generalize:** 100% on factorial/fibonacci → 75% on diverse programs
4. **More rules made things worse:** v3 (60%) < v2 (75%) - fresh instances lack context understanding
5. **⚠️ v5 DISASTER:** Trying to "improve" v1 with prominent #() examples BROKE basic POP counting (55%)
6. **💀 v4 TOTAL FAILURE:** "Bitter lesson" misapplication - priming with Clojure knowledge backfired (0%)

**RECOMMENDATION:** **STOP trying to improve v1!** Use baseline CLJ-PP with explicit POP counting (`CLJPP-PROMPT.md`). Every attempt to "fix" it makes it worse.

---

## Test Methodology

**Two types of tests:**

1. **Single program, multiple iterations** (factorial/fibonacci with cond)
   - Tests consistency: Same program, N fresh Claude instances
   - Used for: Initial prompt testing and refinement

2. **Multiple programs, single iteration** (20 diverse programs)
   - Tests generalization: Different programs, 1 fresh instance each
   - Used for: Final comparison against regular Clojure baseline

---

## Single Program Test Results (Factorial/Fibonacci)

**Test:** 1 program (recursive factorial/fibonacci), N fresh Claude instances → Tests consistency

| Approach | Success Rate | Test Type | Notes |
|----------|--------------|-----------|-------|
| **CLJ-PP POP-ALL v2** | **100%** (20/20) ✅ | 1 program, 20 iterations | Perfect on simple recursion! (But doesn't generalize) |
| **CLJ-PP Baseline (explicit POP)** | **90%** (9/10) | 1 program, 10 iterations | Solid consistency |
| CLJ-PP (POP-LINE + POP-ALL) | **80%** (8/10) | 1 program, 10 iterations | POP-LINE scope ambiguity |
| CLJ-PP (POP-ALL v1) | **80%** (8/10) | 1 program, 10 iterations | Early version |
| CLJ-PP (no examples in prompt) | **50%** (10/20) | 20 programs, 1 iteration | Examples matter!

---

## Multi-Program Test Results (20 Diverse Programs)

✅ **Tests complete:** Sequential (`run-comprehensive-experiment.sh`) and Parallel (`bb bin/run-comprehensive-parallel.clj`) (2025-11-06)

| Approach | Success Rate | Notes |
|----------|--------------|-------|
| **Regular Clojure** | **19/20 (95%)** ✅ | Best performer on diverse programs! |
| CLJ-PP (explicit POP) | **16/20 (80%)** | Solid performance |
| CLJ-PP (POP-ALL v2) | **15/20 (75%)** | Overfitted to factorial/fibonacci |
| CLJ-PP (POP-ALL v3) | **12/20 (60%)** ❌ | WORSE than v2! More aggressive rules backfired |

**Key findings:**
- **POP-ALL v2 achieved PERFECT 100% on simple recursion** but dropped to 75% on diverse programs
- **v3 attempted to add stricter rules** but fresh instances misinterpreted them
- **v3 made things worse:** 60% (12/20) - fresh instances started using POP-ALL inappropriately
- **Explicit POP counting (80%) remains the winner** for diverse programs

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

## Second Experiment: POP-ALL Only (No POP-LINE)

**Hypothesis:** Remove POP-LINE (ambiguous) but keep POP-ALL (unambiguous).

**Result:** **80% success** (8/10)

**Failures:**
- iter3: Included explanatory text before code (same as before)
- iter8: Used POP-ALL inside cond, then added trailing POP POP → underflow!

**Example of confusion (iter8):**
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL  ← Closes everything!
  POP   ← But then tries to close cond
POP     ← And tries to close defn
```

**Error:** POP-ALL already closed cond and defn, so the trailing POPs cause stack underflow.

**The confusion:** Fresh instances don't consistently understand when to use POP-ALL vs trailing POPs.

## Final Results Summary

| Approach | Success Rate | Key Issue |
|----------|--------------|-----------|
| Regular Clojure (fresh) | **80%** | Delimiter counting |
| CLJ-PP Fresh (no examples) | **50%** | Syntax rules alone insufficient |
| **Baseline CLJ-PP (explicit POP, with examples)** | **90%** ✅ | Unambiguous - winner! |
| CLJ-PP + POP-LINE + POP-ALL | **80%** | POP-LINE scope ambiguity |
| CLJ-PP + POP-ALL only | **80%** | When to use POP-ALL vs trailing POPs |

## Conclusion

**Winner:** Baseline CLJ-PP with explicit POP counting (**90% success**)

**Why it wins:**
1. **Unambiguous:** Count PUSHes → emit that many POPs
2. **No scope questions:** Each POP closes exactly one container
3. **No decision fatigue:** Just count - no "am I done?" decisions

**Why POP-ALL didn't help:**
1. Introduced new decision point: "Should I use POP-ALL here?"
2. Fresh instances got confused about trailing POPs after POP-ALL
3. Sometimes used POP-ALL too early (inside cond instead of after)

**Hypothesis rejected (again):** Adding convenience operations (POP-LINE, POP-ALL) did NOT improve success rate.

**Key insight:** **The counting approach is already optimal for fresh instances.**

Adding semantic shortcuts (POP-LINE, POP-ALL) requires understanding WHEN to use them, which is harder than just counting. The arithmetic is tedious but unambiguous.

**Recommendation:** Stick with baseline CLJ-PP (explicit POP counting). It achieves:
- **90% success** (better than regular Clojure's 80%)
- **Zero ambiguity** (every PUSH needs exactly one POP)
- **Simple mental model** (count and match)

---

## POP-ALL v3 Analysis: Why More Rules Made Things Worse

**Test date:** 2025-11-06 (parallel test using `bb bin/run-comprehensive-parallel.clj`)

**Result:** **60% (12/20)** - WORSE than v2's 75%!

### What v3 Changed

v3 added three "critical rules" attempting to address v2's failures:

1. **Rule 1:** NEVER write POPs after POP-ALL (carried over from v2)
2. **Rule 2:** NEVER use POP-ALL in the middle of a function ← NEW
3. **Rule 3:** Start code immediately - no explanations ← NEW

### What Actually Happened

**Fresh instances interpreted the rules as "use POP-ALL MORE aggressively"** instead of "use it more carefully."

### Critical Failure Examples

**Program 04 (v2 vs v3):**
```clojure
v2: PUSH-( ns examples.program4 POP              ✅ Correct
v3: PUSH-( ns examples.program4 POP-ALL          ❌ Closes entire program at line 2!
```

**Program 12 - Inside let binding (v2 vs v3):**
```clojure
v2: PUSH-[ seen PUSH-( atom PUSH-{ POP POP POP   ✅ Explicit counting
v3: PUSH-[ seen PUSH-( atom PUSH-{ POP-ALL       ❌ Closes let, fn, AND defn!
```

**Program 12 - Inside comp (v2 vs v3):**
```clojure
v2: PUSH-( map PUSH-( fn [...] POP POP POP       ✅ Closes fn, map, then waits
v3: PUSH-( map PUSH-( fn [...] POP-ALL           ❌ Closes entire comp + defn!
```

### Why v3 Failed

**The fundamental problem:** Fresh instances don't understand **nesting context**.

When they see:
```clojure
PUSH-( atom PUSH-{ POP-ALL
```

They think: "I'm done with this atom expression → POP-ALL"

They don't realize they're inside a `let` binding, which is inside a `fn`, which is inside a `defn`. POP-ALL closes ALL of them!

### The Paradox

**More explicit rules → More misinterpretation**

- v2 had fewer rules → Fresh instances used POP-ALL conservatively (only at end of complete functions)
- v3 added "don't use POP-ALL in the middle" → Fresh instances thought "okay, so I CAN use it for complete sub-expressions" → Used it EVERYWHERE they thought a sub-expression ended
- Result: v3 used POP-ALL in MORE places, closing parent forms prematurely

### v2 vs v3 Failure Comparison

**v2 failures (5 programs):**
- Programs 04, 05, 11, 13, 14

**v3 failures (8 programs):**
- Programs 04, 08, 11, 12, 13, 14, 16, 19
- **New failures:** Programs 08, 12, 16, 19 (4 regressions!)
- **Only fixed:** Program 05 (1 improvement)

**Net result:** v3 fixed 1 but broke 4 more → 3 steps backward!

### The Actual Solution

**Don't add more rules. The counting approach is already optimal.**

| Approach | Success Rate | Characteristic |
|----------|--------------|----------------|
| Explicit POP counting | **80%** ✅ | Tedious but unambiguous |
| POP-ALL v2 | **75%** | Overfitted to simple recursion |
| POP-ALL v3 | **60%** ❌ | More rules = more confusion |

**Winner:** Explicit POP counting (baseline CLJ-PP)

---

## v4 and v5: The Failed "Improvement" Attempts

**Test date:** 2025-11-06

After establishing v1 (explicit POP) as the 80% baseline, we attempted two "improvements" based on different theories. Both failed catastrophically.

### v4: "Bitter Lesson" Misapplication (0% - TOTAL FAILURE)

**Theory:** LLMs are trained on vast Clojure code. Explicitly prime them with "you're great at Clojure (95% success)" then ask them to translate to CLJPP mid-stride when nesting gets complex.

**Result:** **0/5 (0%)** - Complete failure!

**What happened:** The prompt opened with "You have excellent Clojure training (95% success rate)..." This priming was SO strong that Claude wrote **pure Clojure** with `(` and `)` instead of CLJPP with `PUSH-(` and `POP`.

**Example output:**
```clojure
(ns examples.program3)  ← Should be: PUSH-( ns examples.program3 POP
(defn factorial [n]     ← Should be: PUSH-( defn factorial PUSH-[ n POP ...
```

**Lesson:** Priming effects dominate explicit instructions. Telling an LLM "you're great at X" activates X-generation mode, even when you then ask for Y.

**Postmortem:** See `experiments/v4-FAILURE-ANALYSIS.md` and `POSTMORTEM-v4.md`

---

### v5: "Prominent #() Examples" Backfire (55% - CATASTROPHIC REGRESSION)

**Theory:** v1 fails on programs 04, 11, 13, 17 - all involving anonymous functions `#()`. Make #() expansion more prominent in the prompt to fix these failures.

**Design changes from v1:**
1. **Removed** motivation section ("why CLJ-PP exists")
2. **Shortened** to concise "reference card" style vs v1's "teaching" style
3. **Added** prominent CRITICAL section on #() expansion (early in prompt)
4. **Removed** mental models ("write contents then pop" pattern)
5. **Made it** more rules-based vs explanation-based

**Expected:** Fix #() issues → 85%+ (17/20)

**Actual result:** **11/20 (55%)** - WORSE than v1, v2, AND v3!

**What went wrong:** v5 BROKE basic POP counting!

**Common failure pattern (7/9 failures):**
```clojure
PUSH-( defn active-users PUSH-[ users POP
  PUSH-( filter PUSH-( fn PUSH-[ user POP
    PUSH-( > PUSH-( :age user POP 18 POP
  POP POP users POP  ← ERROR: 3 POPs here (should be 2)
POP                   ← Tries to close defn, but stack already empty!
```

**Manual trace:**
- Line 1: Open defn (depth 1), open [ (depth 2), close [ (depth 1)
- Line 2: Open filter (depth 2), open fn (depth 3), open [ (depth 4), close [ (depth 3)
- Line 3: Open > (depth 4), open :age (depth 5), close :age (depth 4), close > (depth 3)
- Line 4: POP closes fn (depth 2), POP closes filter (depth 1), POP closes defn (depth 0)
- Line 5: **POP with empty stack → ERROR**

**The LLM added an EXTRA POP on line 4!** Should be 2 POPs (close fn, close filter), not 3.

**Why v5 failed:**
1. **Lost teaching context** - Concise "reference" style removed explanation of WHY counting is hard
2. **Lost mental models** - v1's "write contents then pop" pattern helped build intuition
3. **Prominent #() section disrupted flow** - May have distracted from basic POP counting
4. **Too terse** - Short prompts fail for complex tasks like delimiter balancing

**Failure breakdown:**
- **7 transpile errors:** All "POP with empty stack" (miscounting)
- **2 execution errors:** Semantic errors (letfn syntax, transducer logic)

**Comparisons:**
- v1 failures: Programs 04, 11, 13, 17 (4 failures, all complex semantic issues)
- v5 failures: Programs 04, 10, 11, 12, 13, 15, 16, 17, 19 (9 failures, 7 from basic POP miscounting!)

**v5 didn't fix #() issues - it broke POP counting instead.**

**Files:**
- Prompt: `CLJPP-PROMPT-v5.md`
- Results: `experiments/test-variant-v5-20251106-210424/`
- Analysis: `experiments/test-variant-v5-20251106-210424/FAILURE-ANALYSIS.md`
- Design doc: `plans/v5-design.md`

---

## Key Lessons from v4 and v5 Failures

1. **v1 (80%) is a local maximum** - Every "improvement" attempt made things worse
2. **Don't prime with competing formats** - v4 primed with Clojure → wrote Clojure
3. **Don't remove teaching context** - v5 made it concise → broke basic counting
4. **More prominent ≠ better** - v5's prominent #() section harmed overall performance
5. **LLMs need context, not just rules** - Concise reference cards fail for complex tasks
6. **Priming > Explicit instructions** - "You're great at X" overrides "now do Y"

**FINAL VERDICT:** v1 (explicit POP counting with teaching-style prompt) remains the best CLJPP variant at 80%. Stop trying to improve it.

---

**Lesson learned:** **Simple and tedious beats clever and ambiguous.** For fresh LLM instances without context, explicit counting is easier than semantic decision-making. Adding convenience operations requires contextual understanding that fresh instances don't have. **And removing teaching context to add prominent examples breaks the foundational skills.**
