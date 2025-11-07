# CLJPP v4 Catastrophic Failure Analysis

**Date:** 2025-11-06
**Test Results:** 0/5 (0% success) - ALL iterations failed
**Status:** ❌ COMPLETE FAILURE - REVERT TO v1

## What Happened

v4 prompt **completely failed**. The LLM ignored CLJPP syntax entirely and wrote regular Clojure code.

### Test Results

```bash
bb bin/test-variant.clj v4 3 2    # Factorial: 0/2 (0%)
bb bin/test-variant.clj v4 4 3    # Collections: 0/3 (0%)
```

### Error Patterns

**All iterations showed one of two failure modes:**

1. **Writing pure Clojure** (most common):
   ```clojure
   (ns examples.program3)    ← Used ( and ) instead of PUSH-( and POP
   (defn factorial [n]       ← Completely ignored CLJPP syntax
   ```

2. **Writing explanations before code**:
   ```
   I need to write Program 4 in CLJ-PP format. Let me analyze...
   
   PUSH-( ns examples.program4 POP
   ```
   The explanation text contained `(` and `)` which caused tokenizer errors.

## Root Cause Analysis

### The Fatal Flaw in v4's Design

**v4's opening statement:**
> "You have excellent Clojure training (95% success rate). CLJ-PP is just Clojure where delimiters are explicit stack operations..."

**What we intended:** Leverage Clojure knowledge to help with CLJPP

**What actually happened:** LLM thought "I'm good at Clojure → I'll write Clojure" and completely bypassed CLJPP instructions

### The Bitter Lesson Backfired

**v4's hypothesis:** "Leverage training data (Clojure patterns) instead of fighting it"

**Result:** The LLM leveraged its Clojure training SO WELL that it ignored the novel syntax entirely!

**The paradox:**
- Explicit POP counting (v1): 80% - works because it doesn't invoke Clojure mode
- Leverage Clojure training (v4): 0% - invokes Clojure mode TOO strongly

## Why This Happened

1. **Prompt priming effect:** Starting with "You're great at Clojure" primed the LLM to generate Clojure
2. **Path of least resistance:** Clojure is heavily trained, CLJPP is novel → LLM defaulted to what it knows
3. **Instruction following failure:** Despite multiple "start immediately with PUSH-", it wrote explanations or pure Clojure

## Comparison with v1

| Aspect | v1 (Explicit POP) | v4 (Hybrid) |
|--------|-------------------|-------------|
| Success Rate | 80% (16/20) | **0% (0/5)** ❌ |
| Approach | "Count PUSHes, emit POPs" | "You're great at Clojure" |
| Failure Mode | POP counting errors | **Ignores CLJPP entirely** |
| Follow Instructions | Yes (mostly) | **No** |

## Key Insights

### 1. Don't Fight Training Data, But Don't Invoke It Either

**v1's sweet spot:** Teaches CLJPP without mentioning Clojure
- Doesn't fight Clojure knowledge
- Doesn't activate Clojure generation mode
- Just teaches the new syntax

**v4's mistake:** Explicitly invoked Clojure mode
- "You're excellent at Clojure" → LLM enters Clojure generation mode
- "CLJ-PP is just Clojure" → Reinforces Clojure framing
- Result: LLM writes Clojure, not CLJPP

### 2. Instruction Following is Fragile

Despite saying "start immediately with PUSH-" **3+ times** in the prompt:
- In examples
- In error prevention section  
- In final reminder

**The LLM still:**
- Wrote explanatory text before code
- Wrote pure Clojure
- Ignored the instruction entirely

**Lesson:** Strong priming ("you're great at X") overrides explicit instructions

### 3. The Bitter Lesson Applies Differently Here

**The bitter lesson:** Leverage computation/scale over hand-crafted knowledge

**How it applies to LLM prompting:**
- ✅ Leverage pattern matching (good)
- ❌ Explicitly invoke competing patterns (bad)

**v4's error:** Thought "leverage Clojure patterns" meant "tell it about Clojure"  
**Better approach:** Let pattern matching work implicitly through examples only

## Decision

**❌ REJECT v4**  
**✅ REVERT to v1** (80% baseline with explicit POP counting)

### Why v1 is Better

1. **80% > 0%** - v1 actually works
2. **Tedious but reliable** - explicit counting is predictable
3. **No mode confusion** - LLM knows it's writing CLJPP, not Clojure
4. **Follows instructions** - generates CLJPP syntax consistently

## Lessons for Future Prompt Design

### ❌ Don't Do This

1. **Don't prime with "you're great at X"** if X is a competing format
2. **Don't say "Y is just X"** if you want novel Y behavior
3. **Don't assume explicit framing helps** - it can activate wrong modes

### ✅ Do This Instead

1. **Teach syntax directly** without framing
2. **Use examples only** - let pattern matching work implicitly
3. **Keep priming minimal** - avoid activating competing modes
4. **Test early** - v4 failed immediately on simple programs

## Files

- Test results: `experiments/test-variant-v4-202511061-205*/`
- Failed outputs show pure Clojure or explanatory text
- All 5 iterations across 2 programs failed

## Next Steps

1. ✅ Document this failure (this file)
2. ✅ Update BD issue with findings
3. ✅ Update EXPERIMENT-RESULTS.md
4. ❌ Do NOT test v4 further - it's fundamentally broken
5. ✅ Stick with v1 (80% explicit POP counting)

---

**Bottom line:** Sometimes the "clever" approach (leverage training) is worse than the "simple" approach (just count). v1's tedious counting beats v4's clever framing by 80 percentage points.
