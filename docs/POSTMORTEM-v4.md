# CLJPP v4 Postmortem: When The Bitter Lesson Bites Back

**Date:** 2025-11-06
**Result:** ❌ **CATASTROPHIC FAILURE** - 0% success rate
**Decision:** REVERT to v1 (explicit POP counting, 80% success)

---

## TL;DR

We tried to "leverage Clojure training data" by telling the LLM "you're great at Clojure" in the prompt. **The LLM took us literally** and wrote pure Clojure code instead of CLJPP, achieving 0% success.

**Key lesson:** **Priming effects are stronger than explicit instructions.** Saying "you're great at X" activates X-generation mode, even if you then say "but write Y instead."

---

## What We Built

**v4's Design Philosophy:**
- "Apply the bitter lesson: leverage computation (Clojure training) over hand-crafted rules"
- Opening: "You're excellent at Clojure (95% success). CLJ-PP is just Clojure with explicit delimiters"
- 15 examples showing Clojure → CLJPP pattern transformations
- Multiple reminders to "start immediately with PUSH-"

**What We Expected:**
- LLM recognizes Clojure patterns
- Maps them to CLJPP syntax using examples
- Success rate: 85-95% (fix #() bugs, leverage training)

---

## What Actually Happened

### Test Results

```bash
bb bin/test-variant.clj v4 3 2    # Program 3 (factorial): 0/2 (0%)
bb bin/test-variant.clj v4 4 3    # Program 4 (collections): 0/3 (0%)
```

**ALL 5 iterations failed.**

### Failure Modes

**Mode 1: Writing Pure Clojure** (most common)
```clojure
(ns examples.program3)
(defn factorial [n]
  (if (<= n 1)
    1
    (* n (factorial (- n 1)))))
```
- Used `(` and `)` instead of `PUSH-(` and `POP`
- Completely ignored CLJPP syntax
- Perfect Clojure code... which was the problem!

**Mode 2: Writing Explanations First**
```
I need to write Program 4 in CLJ-PP format. Let me analyze the structure:
1. Namespace declaration
2. Vector of user maps
...

PUSH-( ns examples.program4 POP
```
- Despite "start immediately with PUSH-" appearing 3+ times
- Explanation text had `(` and `)` which broke the tokenizer
- Even when it tried CLJPP, the preamble broke everything

---

## Root Cause Analysis

### The Fatal Flaw: Prompt Priming

**v4's opening:**
> "You have excellent Clojure training (95% success rate)."

**Effect:** LLM enters **Clojure generation mode**
- Primed to generate Clojure
- Path of least resistance: use heavily-trained Clojure patterns
- Novel CLJPP syntax becomes "too hard" compared to familiar Clojure

**v4's second line:**
> "CLJ-PP is just Clojure where delimiters are explicit..."

**Effect:** Reinforces Clojure framing
- "It's just Clojure" → "So I'll write Clojure"
- "Explicit delimiters" sounds like a minor detail
- LLM ignores the "explicit" part and focuses on "Clojure"

### Why Instructions Didn't Help

Despite saying **"Start code immediately with PUSH-"** in:
1. Error prevention section
2. Common errors examples
3. Final reminder
4. Multiple example headers

**The LLM still ignored it** because:
- **Priming > Instructions**: "You're great at Clojure" primed Clojure mode stronger than "write PUSH-" could override
- **Competing modes**: Clojure generation (heavily trained) vs novel syntax (untrained)
- **Least effort**: Writing Clojure is easier than following new syntax rules

---

## Comparison: v1 vs v4

| Metric | v1 (Explicit POP) | v4 (Hybrid) |
|--------|-------------------|-------------|
| **Success Rate** | **80% (16/20)** ✅ | **0% (0/5)** ❌ |
| **Approach** | "Count PUSHes, emit POPs" | "You're great at Clojure" |
| **Priming** | None (neutral) | Strong (Clojure mode) |
| **Failure Mode** | POP count errors | **Writes Clojure instead** |
| **Instruction Following** | Good | **Failed completely** |
| **Training Leverage** | Implicit (through examples) | Explicit (too strong!) |

**The paradox:** v1 DOESN'T mention Clojure, yet works better! Why?

**Answer:** v1 teaches CLJPP neutrally:
- No competing mode activation
- Examples show patterns without framing
- LLM learns "this is the task" not "this is related to that other thing I know"

---

## Key Insights

### 1. Priming Effects Dominate

**Finding:** "You're great at X" activates X-generation mode so strongly that subsequent instructions to "write Y" are ignored.

**Mechanism:**
1. LLM hears "you're great at Clojure"
2. Activates Clojure generation pathways
3. Sees examples with PUSH-( and POP
4. Thinks "oh, that's just a detail" and writes Clojure anyway
5. Ignores explicit "start with PUSH-" instructions

**Lesson:** If you want novel behavior Y, DON'T prime with "you're great at X" if X competes with Y.

### 2. "Just X" Framing Backfires

**v4 said:** "CLJ-PP is just Clojure with..."

**LLM heard:** "It's Clojure" (ignores "with explicit delimiters")

**Better framing:** "CLJ-PP is a stack-based syntax for..." (no relation to Clojure mentioned)

**Lesson:** Don't anchor novel behavior to familiar patterns - it triggers default mode.

### 3. The Bitter Lesson Applies Differently to Prompting

**Original bitter lesson:** Leverage computation/scale over hand-crafted knowledge

**How we misapplied it:**
- ❌ "Leverage Clojure training" → explicitly invoke it in prompt
- ❌ Tell LLM about the training data
- ❌ Frame new task in terms of old task

**How to apply it correctly:**
- ✅ Use examples that pattern-match implicitly
- ✅ Let LLM find similarities without telling it
- ✅ Trust in-context learning over explicit framing

**v1 does this right:**
- Shows CLJPP examples
- LLM implicitly recognizes Clojure-like patterns
- Learns CLJPP syntax without mode confusion

### 4. Simple Beats Clever (Again)

**v4:** "Clever" approach - leverage training data explicitly
- **Result:** 0% (complete failure)

**v1:** "Simple" approach - just count POPs
- **Result:** 80% (works reliably)

**Lesson:** When working with novel syntax, boring counting > clever framing

---

## Lessons for Future Prompt Design

### ❌ DON'T Do This

1. **Don't prime with competing formats**
   - "You're great at X" when you want Y
   - Activates wrong generation mode

2. **Don't use "just X" framing**
   - "Y is just X with..." triggers X mode
   - LLM focuses on X, ignores the "with..."

3. **Don't assume framing helps**
   - Thought: "Framing helps LLM understand"
   - Reality: "Framing activates competing modes"

4. **Don't rely on repeated instructions**
   - Saying "do Y" 3+ times doesn't override "you're great at X" priming
   - Priming > Instructions

### ✅ DO This Instead

1. **Teach syntax directly**
   - No framing, no comparisons
   - Just: "Here's the syntax, here are examples"

2. **Use examples, not explanations**
   - Let pattern matching work implicitly
   - Don't tell LLM what it should notice

3. **Keep priming neutral**
   - Don't activate any particular mode
   - Let the task itself guide behavior

4. **Test immediately**
   - v4 failed on first 5 tests
   - Could have stopped after test 1!

---

## The Irony

**We set out to "leverage the bitter lesson"** (trust scale/computation over rules)

**We ended up proving a different lesson:** **Explicit framing can backfire when it activates competing modes**

**v1's implicit approach is closer to the real bitter lesson:**
- Trust examples (data) over explanations (framing)
- Let pattern matching work naturally
- Don't overthink it

---

## Decision & Next Steps

### ✅ REVERT to v1

**Why:**
- 80% success rate (proven, reliable)
- No mode confusion
- Follows instructions
- Tedious but works

### ❌ ABANDON v4

**Why:**
- 0% success (catastrophic failure)
- Fundamental design flaw (priming effect)
- Not fixable without removing the core idea
- Would need complete redesign

### 📝 Document & Learn

**Done:**
- ✅ experiments/v4-FAILURE-ANALYSIS.md - detailed analysis
- ✅ POSTMORTEM-v4.md - this document
- ✅ BD issue closed with findings
- ✅ Test results preserved in experiments/

**Future work:**
- Don't repeat this mistake
- v1 (80%) is good enough
- Focus efforts elsewhere

---

## Bottom Line

**Sometimes trying to be clever makes things worse.**

- v1: "Count POPs" → 80%
- v4: "Leverage training!" → 0%

**The simplest approach won by 80 percentage points.**

---

**Files:**
- Full analysis: `experiments/v4-FAILURE-ANALYSIS.md`
- Test results: `experiments/test-variant-v4-*/`
- BD issue: `cljp-tokenizer-1` (closed)
- Prompt design: `plans/variant-hybrid-minimal-dense.md` (failed approach)

**Status:** v1 remains the best CLJPP prompt. No further variants planned.
