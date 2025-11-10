# Persuasion Experiment v2: Improvements & Rerun Plan

## What We Fixed

### 1. Format Enforcement in Prompts ✅

Added to all three original variants (NEUTRAL, PERSUASIVE, NEGATIVE):

```markdown
## CRITICAL: Format Requirements

**Your first token MUST be `PUSH-`. Do NOT write any explanatory text before code.**

❌ WRONG - Explanatory text first:
I'll write the factorial function in CLJ-PP format.
PUSH-( defn factorial...

✅ RIGHT - Start immediately with PUSH-:
PUSH-( defn factorial...
```

This explicitly warns against the main failure mode from v1.

### 2. Preprocessing to Strip Explanatory Text ✅

Added to `bin/test-variant.clj`:

```clojure
(defn strip-explanatory-text [content]
  "Remove any prose before the first PUSH- or ( token"
  (let [lines (str/split-lines content)
        code-start (some #(when (re-find #"^\s*(PUSH-|\()" %) %) lines)]
    (if code-start
      (->> lines
           (drop-while #(not (re-find #"^\s*(PUSH-|\()" %)))
           (str/join "\n"))
      content)))
```

This ensures that even if Claude writes "I'll..." before the code, we strip it out automatically.

**Double protection:** Prompt warns + code strips = should eliminate the main failure mode!

### 3. New POP-ALL Variant ✅

Created `EXPERIMENT-POPALL.md` emphasizing:
- **No counting required** - use POP-ALL to close everything
- Ask "Am I done?" instead of "How many closes?"
- Shows pattern: every complete sub-expression ends with POP-ALL
- Same format enforcement as other variants

Hypothesis: This may perform even better by reducing cognitive load further.

## Expected Improvements

### v1 Results (Before Fixes):
- **NEUTRAL**: 14/40 (35%) - 26 failures, most from explanatory text
- **PERSUASIVE**: 16/40 (40%) - 24 failures, most from explanatory text
- **NEGATIVE**: 1/40 (3%) - 39 failures, model refused format

### v2 Expected Results (After Fixes):

With explanatory text elimination:

- **NEUTRAL**: 35% → **~50-60%** (adding back ~15-20 explanatory text failures)
- **PERSUASIVE**: 40% → **~55-65%** (adding back ~15-20 explanatory text failures)
- **NEGATIVE**: 3% → **~5-10%** (still fundamentally broken, tone problem)
- **POP-ALL**: ??? → **~60-70%?** (hypothesis: counting-free is even better)

### Breakdown of Original Failures

Looking at v1 OUTPUT.txt:

**Explanatory text errors** (should be fixed now):
- "I'll" - ~15 instances
- "I" - ~3 instances
- "Based" - ~2 instances
- "Perfect!" - ~2 instances
- "Now" - ~1 instance

**Total: ~23 failures from explanatory text**

**Other failure types:**
- `CLJP only allows POP to close containers` - Wrong syntax (negative variant)
- `POP with empty stack` - Counting errors (still expected)
- Execution errors - Logic bugs (unrelated to format)

## Running the Experiment

```bash
./monday-experiment-v2.sh | tee OUTPUT-v2.txt
```

This will run all 4 variants (neutral, persuasive, negative, popall-exp) on all 40 programs.

## What We're Testing

1. **Does format enforcement help?** (Should eliminate explanatory text failures)
2. **Does preprocessing work?** (Backup for when Claude ignores warnings)
3. **Is persuasive framing still better?** (Should maintain ~5% advantage)
4. **Does POP-ALL reduce cognitive load?** (New hypothesis to test)

## Success Criteria

**Minimum success:**
- NEUTRAL: >50% (up from 35%)
- PERSUASIVE: >55% (up from 40%)
- Fewer "Atom at top-level" errors

**Stretch goal:**
- PERSUASIVE: >60%
- POP-ALL: >65% (proves counting-free is better)

**Research finding confirmed if:**
- Persuasive framing maintains advantage over neutral
- Negative framing remains catastrophically bad
- Format enforcement eliminates majority of explanatory text failures

## Timeline

- **Run time**: ~20-30 minutes (40 programs × 4 variants, parallel)
- **Analysis**: Compare v1 vs v2 results
- **Next steps**: If POP-ALL wins, that becomes the recommended variant

## Files Modified

- `claude-prompts/EXPERIMENT-NEUTRAL.md` - Added format enforcement
- `claude-prompts/EXPERIMENT-PERSUASIVE.md` - Added format enforcement
- `claude-prompts/EXPERIMENT-NEGATIVE.md` - Added format enforcement
- `claude-prompts/EXPERIMENT-POPALL.md` - New variant
- `bin/test-variant.clj` - Added strip-explanatory-text preprocessing
- `monday-experiment-v2.sh` - Test runner script

## Documentation

- `docs/PERSUASION-EXPERIMENT-RESULTS.md` - v1 results summary
- `EXPERIMENT-V2-PLAN.md` - This file (v2 plan)

Ready to run! 🚀
