# CLJPP Prompt v4: Design Summary

**Date:** 2025-11-06
**Status:** Ready for testing
**Goal:** Improve on 80% success rate by leveraging the bitter lesson

---

## 🎯 The Problem

Current CLJPP prompt (v1) achieves **80% success** (16/20 programs) on comprehensive test.

**Regular Clojure achieves 95%** - better than CLJPP!

**Key question:** How do we leverage the LLM's excellent Clojure knowledge (95%) when using CLJPP?

---

## 🧠 Analysis: Why Does Regular Clojure Win?

**Hypothesis:** LLMs don't "count delimiters" - they do **pattern completion**.

When generating `(* n (factorial (- n 1)))`, the LLM has seen this pattern thousands of times in training. It's reproducing a learned pattern, not calculating delimiter counts.

**CLJPP forces abandonment of this superpower:**
- No training data for `PUSH-( * n PUSH-( factorial...`
- Requires manual counting (working memory)
- Can't leverage pattern recognition

**The bitter lesson:** Methods that leverage computation (scale with data) beat methods that leverage human knowledge (hand-crafted rules).

---

## 🔍 Failure Mode Analysis

### What Failed (4/20 programs)

**Programs 04, 13:**
```clojure
❌ WRONG: #PUSH-( > % 0 POP
```
**Error:** Mixed Clojure `#()` syntax with CLJPP
**Root cause:** LLM's Clojure training made it want to use `#()`, but didn't know how to translate it

**Program 17:**
```clojure
PUSH-( letfn PUSH-[
  PUSH-( PUSH-[ helper PUSH-[ n POP
    [complex nested body]
  POP POP
POP
  PUSH-( helper start POP
POP
```
**Error:** Multi-arity `defn` with `letfn` - complex nesting
**Root cause:** Deep nesting + unfamiliar pattern

### What Worked (16/20 programs)

- Simple `defn`, `let`, `map`, `filter` - all worked
- Direct Clojure patterns (`if`, `cond`, threading macros) - worked
- Hiccup, transducers, protocols - worked

**Pattern:** Common Clojure idioms translated successfully. Edge cases (anonymous functions, complex nesting) failed.

---

## 💡 The Solution: Apply the Bitter Lesson

**Instead of fighting Clojure training, leverage it!**

### Strategy: Hybrid Prompt (v4)

1. **Explicitly invoke Clojure knowledge:**
   - "You're excellent at Clojure (95% success)"
   - "CLJ-PP is just Clojure where delimiters are explicit"

2. **Minimal rules:**
   - ONE POP PER PUSH
   - Count opening delimiters in Clojure → that's how many POPs

3. **Dense examples (15 examples):**
   - Cover ALL observed failure modes
   - Trust in-context learning (bitter lesson)
   - Show pattern transformations:
     ```
     Clojure: (defn f [x] body)
     CLJ-PP:  PUSH-( defn f PUSH-[ x POP body POP
     ```

4. **Critical edge case: `#()` expansion:**
   ```clojure
   ❌ WRONG: #PUSH-( > % 5 POP
   ❌ WRONG: #(> % 5)
   ✅ RIGHT: PUSH-( fn PUSH-[ % POP PUSH-( > % 5 POP POP
   ```

5. **No fluff:**
   - Start code immediately with `PUSH-`
   - No explanatory text before code

---

## 📊 Expected Outcomes

### Best Case: 19/20 (95%)
Match regular Clojure by successfully leveraging training data

### Good Case: 17-18/20 (85-90%)
Fix `#()` errors (programs 04, 13) + improve on complex nesting

### Acceptable: 16/20 (80%)
No regression - validates that explicit examples don't hurt

### Failure: < 16/20 (< 80%)
Revert to v1 (explicit POP counting is already optimal)

---

## 🎓 Key Insights from This Process

### 1. The Bitter Lesson Applied

**Old thinking:** "LLMs can't count delimiters → need explicit stack ops"
**New thinking:** "LLMs don't count - they pattern-match. Help them match CLJPP to Clojure patterns."

### 2. Reader Macros Decision

**Question:** Should CLJPP support `#()`, `#?(:clj)`, etc.?
**Answer:** No.

**Rationale:**
- Defeats the purpose (still has implicit delimiters)
- Inconsistent (some explicit, some implicit)
- Adds parser complexity
- **Fix is in the prompt, not the assembler**

### 3. Hybrid Approaches Don't Work

**Idea:** "Use Clojure normally, switch to CLJPP for hard parts"
**Problem:** Stack context ambiguity - can't know depth mid-stream

**Conclusion:** Pure CLJPP only. But leverage Clojure knowledge in the prompt.

### 4. More Rules ≠ Better Performance

**Evidence:**
- POP-ALL v2: 75% (simple rules)
- POP-ALL v3: 60% (more rules, worse performance!)

**Why:** Fresh instances misinterpret complex rule sets. Simplicity wins.

---

## 📁 Files Created

```
plans/
  variant-hybrid-minimal-dense.md    # Design doc with analysis
  SUMMARY-v4-design.md              # This file

CLJPP-PROMPT-v4.md                  # Generated prompt (6.1 KB)
build-prompt.sh                     # Build script
.bd/test-prompt-v4.md              # Testing task/issue

CLAUDE.md                          # Updated with v4 info
```

---

## 🚀 Next Steps

1. **Run comprehensive test:**
   ```bash
   ./run-comprehensive-experiment.sh v4
   ```

2. **Analyze results:**
   - Which programs still fail?
   - Are they the same failures or new ones?
   - Did `#()` examples help?

3. **Update EXPERIMENT-RESULTS.md** with findings

4. **Decision:**
   - If > 85%: v4 becomes new baseline
   - If 80-85%: Iterate on remaining failures
   - If < 80%: Stick with v1 (explicit counting)

---

## 🤔 Open Questions

1. **Can CLJPP ever beat pure Clojure?**
   - Probably not, given training data advantage
   - But can we get close (within 5%)?

2. **What's the real use case for CLJPP?**
   - Not for LLM generation (Clojure is better)
   - Maybe for human editing? Algorithmic code gen?
   - Or just an interesting experiment in syntax design?

3. **Is the bitter lesson the right framing?**
   - Yes: Leverage scale (training data) over rules
   - But: CLJPP itself is "hand-crafted rules"
   - Maybe the real lesson: Don't fight your training data

---

## 📚 References

- [The Bitter Lesson (Rich Sutton)](http://www.incompleteideas.net/IncIdeas/BitterLesson.html)
- Experiment results: `experiments/EXPERIMENT-RESULTS.md`
- Original prompt: `CLJPP-PROMPT.md` (v1)
- Test suite: `experiments/comprehensive-test-*/`
