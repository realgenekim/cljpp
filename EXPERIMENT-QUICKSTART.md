# Persuasion Experiment Quick Start

## What We're Testing

**Can LLMs be "sold to"?** Does persuasive framing in prompts actually improve performance, or do only examples/rules matter?

We created 3 prompts with **identical examples** but different framing:
- **Neutral**: Technical documentation style
- **Persuasive**: "Hey Claude, you know how you struggle with closing parens? This is built FOR YOU!"
- **Negative**: "This is verbose and non-standard. Use when required."

## Prerequisites (Already Done ✓)

- ✅ Clojure installed (~/.local/bin/clojure)
- ✅ Babashka installed (~/.local/bin/bb)
- ✅ cljpp transpiler configured for Babashka
- ✅ 3 experimental prompts created

## Quick Test (5 Minutes)

Test all 3 variants on a single program:

```bash
export PATH=~/.local/bin:~/bin:$PATH

# Test program 3 (factorial) with each variant
bb bin/test-variant.clj neutral 3 1
bb bin/test-variant.clj persuasive 3 1
bb bin/test-variant.clj negative 3 1
```

Look for `✓ SUCCESS` or `❌` in the output!

## Full Experiment (2-3 Hours)

Test all 20 programs with each variant:

```bash
# This will make 60 Claude API calls (20 programs × 3 variants)
bb bin/test-variant.clj neutral all 1
bb bin/test-variant.clj persuasive all 1
bb bin/test-variant.clj negative all 1
```

## Checking Results

After running:

```bash
# Count successes
grep -r "✓ SUCCESS" experiments/test-variant-neutral-* | wc -l
grep -r "✓ SUCCESS" experiments/test-variant-persuasive-* | wc -l
grep -r "✓ SUCCESS" experiments/test-variant-negative-* | wc -l

# See failures
grep -r "❌" experiments/test-variant-neutral-*
grep -r "❌" experiments/test-variant-persuasive-*
grep -r "❌" experiments/test-variant-negative-*
```

## What Success Looks Like

**If Persuasive > Neutral:**
- 🎉 Persuasive framing WORKS!
- 🎉 Communication style matters for LLMs
- 🎉 Real research finding!

**If all variants ≈ equal:**
- Only examples matter, not framing
- Keep prompts minimal/technical

**If Negative harms performance:**
- Negative framing is actively harmful
- Avoid discouraging language

## Files

- **Prompts:** `claude-prompts/EXPERIMENT-{NEUTRAL,PERSUASIVE,NEGATIVE}.md`
- **Test script:** `bin/test-variant.clj` (updated with new variants)
- **Full documentation:** `experiments/EXPERIMENT-PERSUASION-STUDY.md`
- **Results:** Will be in `experiments/test-variant-<variant>-<timestamp>/`

## Next Steps

1. **Run quick test** to verify setup
2. **Review one output** to ensure quality
3. **Run full experiment** (or statistical version with 5 iterations)
4. **Analyze results** and determine if persuasion matters!
5. **Document findings** in experiments/

This could prove whether "selling" to LLMs actually works! 🚀
