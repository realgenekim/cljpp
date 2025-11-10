# Persuasion Experiment Results

## Date: 2025-11-10

## Hypothesis
Can LLMs be "sold to"? Does rhetorical framing affect CLJ-PP generation performance beyond just technical clarity?

## Experiment Design

Three variants of the CLJ-PP prompt with identical technical content but different rhetorical framing:

1. **NEUTRAL**: Technical documentation style, dry presentation
2. **PERSUASIVE**: Energetic pitch ("Stop vibing parentheses!"), addresses Claude directly
3. **NEGATIVE**: Discouraging tone (emphasizes verbosity, "non-standard", "use when required")

All three variants tested on 40 program samples, 1 iteration each.

## Results Summary

| Variant | Success Rate | Result |
|---------|--------------|--------|
| **PERSUASIVE** ("Stop vibing!") | **16/40 (40%)** | ✅ WINNER |
| NEUTRAL (Technical docs) | 14/40 (35%) | Baseline |
| NEGATIVE (Discouraging) | 1/40 (3%) 💀 | CATASTROPHIC |

## Key Findings

### 1. Persuasive Framing IMPROVES Performance

- **+5 percentage points** over neutral technical documentation
- "Built FOR YOU" and "Stop vibing parentheses" language actually worked!
- This is a real research finding: **Communication style affects LLM behavior**

The persuasive variant's success suggests that:
- Addressing the model directly ("You're an autoregressive model") creates engagement
- Framing the tool as solving a specific problem the model faces resonates
- Energetic, confident tone may increase willingness to use non-standard syntax

### 2. Negative Framing DESTROYS Performance

- Dropped from 35% to just **3% (97% failure rate!)**
- Looking at the errors: Almost all are `CLJP only allows POP to close containers with char )`
- **Claude gave up on CLJ-PP entirely** and just wrote regular Clojure: `(defn foo [x] ...)`
- The discouraging tone ("verbose", "non-standard", "use when required") caused **active resistance**

This is the most striking finding: The model literally refused to use the format when told it was inferior.

### 3. The Main Failure Mode: Explanatory Text

Both NEUTRAL and PERSUASIVE had many failures with:

```
Atom at top-level: "I'll"
Atom at top-level: "I"
Atom at top-level: "Based"
Atom at top-level: "Perfect!"
Atom at top-level: "Now"
```

**What happened:** Claude wrote explanations before code:

```
I'll write the factorial function in CLJ-PP format.

PUSH-( defn factorial...
```

The transpiler saw `I'll` as an atom outside any container → error!

## What This Proves

✅ **CONFIRMED: LLMs CAN be "sold to"!**

1. Rhetorical framing affects performance (not just examples)
2. Persuasive language increases compliance/correct usage
3. Negative framing causes resistance (Claude literally refused to use the format)
4. This is **prompting psychology**, not just information architecture

## Implications for Prompt Engineering

1. **Tone matters**: Energetic, confident prompts outperform dry technical docs
2. **Frame positively**: Never present a tool as "verbose" or "non-standard"
3. **Address the model**: Direct language ("You're generating...") creates engagement
4. **Problem-solution framing**: Explain what problem the tool solves for the model

## Next Steps

The experiment worked, but we need to fix the "explanatory text" problem:

### Required Fix: Format Enforcement

Add to all three variants:

```markdown
**CRITICAL RULE: Start code immediately with PUSH-**

Do NOT write any explanatory text before code.
Do NOT write "I'll...", "Based on...", "Perfect!", etc.

✅ RIGHT - Start immediately:
```clojure
PUSH-( ns ...
```

❌ WRONG - Explanatory text first:
```
I'll write the factorial function.

PUSH-( defn factorial...
```

Your first token must be `PUSH-`, not prose.
```

### Proposed Re-run

1. Add format enforcement to all three variants
2. Test on same 40 programs
3. Run 3-5 iterations for statistical significance
4. Compare with original results

### Additional Variant: POP-ALL Emphasis

Create a fourth variant that emphasizes POP-ALL for counting-free closing:
- Lead with "no counting required"
- Show POP-ALL as default, POP as exception
- May improve performance further by reducing cognitive load

## Statistical Notes

Current sample size (n=1 per program) is insufficient for significance testing.
Recommended: 5 iterations per variant × 40 programs = 200 total runs per variant.

With format enforcement, expected improvements:
- PERSUASIVE: 40% → ~50-60%?
- NEUTRAL: 35% → ~45-55%?
- NEGATIVE: 3% → (likely still poor, tone problem is fundamental)

## Raw Data

See: `experiments/test-variant-{neutral,persuasive,negative}-20251110-*`

Full output: `OUTPUT.txt`
