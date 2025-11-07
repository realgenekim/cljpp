# POP-ALL v2 Hypothesis

**Date:** 2025-11-06
**Experiment:** Testing improved POP-ALL prompt

## The Problem

Original POP-ALL experiment achieved only **80% success** (8/10), same as POP-LINE+POP-ALL.

### Root Cause Analysis

**Failure pattern (iter8 from original POP-ALL test):**
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
  POP   ← ERROR! Stack already empty
POP     ← ERROR! Stack already empty
```

**The confusion:** Fresh instances used POP-ALL but then added trailing POPs anyway.

**Why?** The original prompt said "No trailing POPs needed!" in a comment, but didn't emphasize it strongly enough.

## The Solution: POP-ALL v2 Prompt

### Key Improvements

1. **THE CRITICAL RULE FOR POP-ALL section**
   - Makes it a top-level section, not buried in examples
   - Shows WRONG pattern with ❌ explicitly

2. **Error Example #1: POPs After POP-ALL**
   - Dedicated error section showing exact failure pattern
   - Shows why it fails (stack is empty)

3. **Repeated emphasis throughout**
   - "POP-ALL means STOP" appears 5+ times
   - "Never write POPs after POP-ALL" stated explicitly
   - Every POP-ALL example includes "Done! Nothing after!"

4. **Mental model reinforcement**
   - "POP-ALL = I'm completely finished!"
   - Clear decision tree: "Am I done? → YES → POP-ALL (then STOP!)"

### Changes from Original Prompt

**Original prompt (CLJPP-PROMPT-WITH-POP-ALL-ONLY.md):**
- Mentioned "No trailing POPs needed!" once in a comment
- Showed correct pattern but didn't show WRONG pattern
- Didn't have dedicated error section

**v2 prompt (CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md):**
- Dedicated "CRITICAL RULE" section at top
- Shows WRONG pattern with ❌ before showing right pattern
- Error section specifically for "POPs After POP-ALL"
- Repeats "POP-ALL means STOP" throughout
- Adds "Final Reminders" section at end reinforcing the rule

## Hypothesis

**If the confusion was due to insufficient emphasis**, then the improved prompt should achieve **90%+ success** (matching baseline).

**If the confusion is fundamental to the POP-ALL concept**, then success rate will remain ~80%.

## Expected Results

| Scenario | Outcome | Success Rate | Interpretation |
|----------|---------|--------------|----------------|
| Hypothesis CONFIRMED | v2 fixes the problem | **90%+** (18+/20) | Prompting matters! POP-ALL is viable |
| Hypothesis REJECTED | v2 doesn't help | **~80%** (16/20) | POP-ALL adds cognitive load regardless |

## Additional Experiment Design Changes

**Contamination removal:**
- Backed up original CLAUDE.md (full of CLJ-PP experiment details)
- Created minimal CLAUDE.md with no hints about experiments
- This prevents fresh instances from seeing "oh, this is an experiment about POP-ALL"

**Test scale:**
- 20 iterations (instead of 10) for better statistical confidence
- Same factorial/fibonacci program as baseline for direct comparison

## Success Criteria

**Minimum viable improvement:** 18/20 (90%) - matches baseline
**Strong success:** 19/20 (95%) - better than baseline!

## What We'll Learn

**If v2 succeeds:**
- POP-ALL is viable with proper prompting
- Clear error examples matter more than abstract rules
- Fresh instances need to see WRONG patterns, not just right ones

**If v2 fails:**
- Decision fatigue is fundamental (POP-ALL vs trailing POPs)
- Baseline (explicit POP) is optimal - no shortcuts work
- Keep it simple: one PUSH → one POP

## Previous Results (for comparison)

| Approach | Success | Why |
|----------|---------|-----|
| Regular Clojure | 80% (16/20) | Delimiter counting hard |
| CLJ-PP Baseline (POP) | **90%** (9/10) | Unambiguous counting |
| CLJ-PP + POP-ALL v1 | 80% (8/10) | POPs after POP-ALL confusion |
| CLJ-PP + POP-ALL v2 | **100%** (20/20) ✅ | **HYPOTHESIS CONFIRMED!** |

## RESULT: ✅ HYPOTHESIS CONFIRMED!

**Test run:** 2025-11-06 19:14:50
**Success rate:** 20/20 (100%) - PERFECT SCORE!
**Location:** `experiments/popall-v2-clean/run-20251106-191450/`

### What We Learned

**✅ POP-ALL is viable with proper prompting**
- Clear error examples matter more than abstract rules
- Fresh instances need to see WRONG patterns (❌), not just right ones
- Repetition works: "POP-ALL means STOP" stated 5+ times

**Key insight:**
> "Show what NOT to do, not just what TO do."

**Result:** The improved v2 prompt not only matched baseline (90%) but achieved **perfect 100% success**!
