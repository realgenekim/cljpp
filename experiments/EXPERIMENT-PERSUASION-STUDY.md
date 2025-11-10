# Persuasion Study: Can LLMs Be "Sold To"?

## Research Question

Does persuasive framing in prompts affect LLM code generation performance, or is only informational content (examples, rules) what matters?

## Hypothesis

**H1 (Persuasion Matters):** Persuasive framing activates better context and increases performance.
- Mechanism: "You struggle with X" → activates problem-recognition weights
- Mechanism: "Built FOR YOU" → links solution to autoregressive constraints
- Mechanism: "KILLER APP" → increases salience of important examples

**H2 (Only Examples Matter):** Rhetorical framing is irrelevant; only examples and rules affect performance.
- LLMs have no emotions/motivation
- Pattern matching is purely statistical
- Neutral technical documentation works equally well

**H3 (Priming Effects):** Strong framing might activate wrong context (like v4's 0% failure).

## Experimental Design

### Control Variables (Identical Across All Variants)

1. **Same 6 core examples:**
   - Example 1: Simple function (`defn add`)
   - Example 2: Nested calls (`double-inc`)
   - Example 3: Recursive function (`factorial with cond`)
   - Example 4: Hiccup vector (with `when`)
   - Example 5: Let binding (with map literal)
   - Example 6: Multi-arity function

2. **Same technical rules:**
   - Four operations: PUSH-(, PUSH-[, PUSH-{, POP
   - ONE POP PER PUSH
   - Operation counting guidance
   - Common patterns reference
   - Error messages description

3. **Same test methodology:**
   - 20-program test suite
   - Fresh Claude instance per test
   - Same test infrastructure
   - Same transpiler

### Independent Variable: Rhetorical Framing

**Variant 1: Neutral (Technical Documentation)**
- File: `claude-prompts/EXPERIMENT-NEUTRAL.md`
- Tone: Objective, specification-style
- Example intro: "Example 1: Simple Function"
- Key phrases: "Syntax", "Operation Rule", "Specification"
- No emotional language, no "you", minimal motivation

**Variant 2: Persuasive ("Selling" to LLMs)**
- File: `claude-prompts/EXPERIMENT-PERSUASIVE.md`
- Tone: Engaging, problem-solving, aspirational
- Example intro: "Example 1: Simple Function - See The Difference"
- Key phrases: "Built FOR YOU", "Stop vibing", "This is where CLJ-PP shines"
- Direct address ("you"), pain points emphasized, benefits highlighted

**Variant 3: Negative (Discouraging)**
- File: `claude-prompts/EXPERIMENT-NEGATIVE.md`
- Tone: Reluctant, emphasizing downsides
- Example intro: "Example 1: Simple Function (Note Increased Length)"
- Key phrases: "Verbose Alternative", "Non-Standard", "adds overhead"
- Emphasizes verbosity, constraints, downsides

### Dependent Variable: Success Rate

Success = Transpiles correctly AND executes without errors

Measured across 20 diverse programs:
- 01-05: Simple functions, let, recursion, collections, threading
- 06-10: Error handling, multimethods, destructuring, state, hiccup
- 11-15: Async, transducers, spec, protocols, graph traversal
- 16-20: Parser combinators, lazy sequences, web handler, datalog, mega hiccup

## Running The Experiment

### Prerequisites

```bash
# Install Clojure and Babashka (already done)
export PATH=~/.local/bin:~/bin:$PATH

# Verify setup
clojure --version  # Should show 1.12.3.1577
bb --version       # Should show v1.12.209
./bin/cljpp /tmp/test.cljpp /tmp/test.clj  # Should work
```

### Test Commands

#### Quick Test (Single Program, 3 Iterations Each)

Test all 3 variants on program 3 (factorial) with 3 iterations each:

```bash
# Neutral variant
bb bin/test-variant.clj neutral 3 3

# Persuasive variant
bb bin/test-variant.clj persuasive 3 3

# Negative variant
bb bin/test-variant.clj negative 3 3
```

Results will be in:
- `experiments/test-variant-neutral-<timestamp>/`
- `experiments/test-variant-persuasive-<timestamp>/`
- `experiments/test-variant-negative-<timestamp>/`

#### Full Experiment (All 20 Programs, 1 Iteration Each)

**IMPORTANT:** Each test uses fresh Claude instances via the `claude` CLI. This requires:
1. Claude CLI installed and configured
2. API access
3. Cost consideration: 20 programs × 3 variants = 60 API calls

```bash
# Test all variants on all programs
bb bin/test-variant.clj neutral all 1
bb bin/test-variant.clj persuasive all 1
bb bin/test-variant.clj negative all 1
```

#### Full Experiment (All 20 Programs, 5 Iterations Each)

For statistical significance (100 tests per variant):

```bash
bb bin/test-variant.clj neutral all 5
bb bin/test-variant.clj persuasive all 5
bb bin/test-variant.clj negative all 5
```

**Cost:** 20 programs × 3 variants × 5 iterations = 300 API calls

### Output Structure

Each test run creates:
```
experiments/test-variant-<variant>-<timestamp>/
  ├── 01-neutral-iter01.raw        # Raw Claude output
  ├── 01-neutral-iter01.cljpp      # Cleaned CLJ-PP code
  ├── 01-neutral-iter01.clj        # Transpiled Clojure
  ├── 02-neutral-iter01.raw
  ...
  └── results-summary.txt           # Success/failure summary
```

### Analyzing Results

After running experiments:

```bash
# Count successes for each variant
grep -r "✓ SUCCESS" experiments/test-variant-neutral-*/
grep -r "✓ SUCCESS" experiments/test-variant-persuasive-*/
grep -r "✓ SUCCESS" experiments/test-variant-negative-*/

# Count failures
grep -r "❌" experiments/test-variant-neutral-*/
grep -r "❌" experiments/test-variant-persuasive-*/
grep -r "❌" experiments/test-variant-negative-*/
```

## Expected Outcomes

### Outcome 1: Persuasive > Neutral > Negative
**Interpretation:** Rhetorical framing matters! Persuasive patterns activate better context.
**Impact:** Prompting is part psychology - communication style affects performance.
**Next steps:** Study which persuasive elements matter most.

### Outcome 2: All Variants ≈ Equal
**Interpretation:** Only examples/rules matter, framing is irrelevant.
**Impact:** Prompts should be minimal and technical.
**Next steps:** Focus on example quality, not communication style.

### Outcome 3: Negative ≈ Persuasive > Neutral
**Interpretation:** Salience matters (any strong framing helps), not persuasion.
**Impact:** Bold/memorable language increases attention, regardless of tone.
**Next steps:** Test extreme framings (very bold neutral).

### Outcome 4: Persuasive = 0% (Like v4)
**Interpretation:** Priming backfired - "stop vibing parens" activated wrong context.
**Impact:** Strong framing can harm performance.
**Next steps:** Test milder persuasive variants.

## Statistical Analysis

### Sample Calculation

For 20 programs × 5 iterations = 100 tests per variant:

```
Neutral:     X/100 successes    (X% success rate)
Persuasive:  Y/100 successes    (Y% success rate)
Negative:    Z/100 successes    (Z% success rate)
```

### Significance Test

Use chi-square test for proportions:
- Null hypothesis: Success rates are equal
- Alternative: Success rates differ
- α = 0.05

If p < 0.05: Reject null, persuasion matters!

### Effect Size

Cohen's h for difference in proportions:
- Small effect: h ≈ 0.2
- Medium effect: h ≈ 0.5
- Large effect: h ≈ 0.8

## Baseline Comparison

Compare experimental variants to known baselines:

| Variant | Expected | Historical Baseline |
|---------|----------|---------------------|
| Neutral | ~80%? | v1 (explicit POP): 80% |
| Persuasive | >80%? | Unknown |
| Negative | <80%? | Unknown |

## Research Contributions

If persuasion matters:
1. **Novel finding**: Rhetorical strategies affect LLM performance
2. **Practical impact**: Prompt engineering should study persuasive writing
3. **Theoretical insight**: LLMs respond to communication patterns, not just information
4. **Future work**: Which persuasive elements matter most?

## Files

- **Prompts:** `claude-prompts/EXPERIMENT-{NEUTRAL,PERSUASIVE,NEGATIVE}.md`
- **Test script:** `bin/test-variant.clj`
- **Test data:** `test-data/test-prompts.txt`
- **Results:** `experiments/test-variant-<variant>-<timestamp>/`
- **This doc:** `experiments/EXPERIMENT-PERSUASION-STUDY.md`

## Timeline

1. **Quick validation** (30 min): Test single program × 3 variants × 3 iterations
2. **Full test** (2-3 hours): 20 programs × 3 variants × 1 iteration
3. **Statistical test** (6-8 hours): 20 programs × 3 variants × 5 iterations
4. **Analysis** (1-2 hours): Aggregate results, run statistics
5. **Write-up** (2-4 hours): Document findings

Total: 1-2 days for complete study

## Notes

- Each `claude` CLI call is a fresh instance (no context carryover)
- Test order doesn't matter (stateless)
- Can parallelize across variants (independent)
- Results are reproducible (deterministic transpiler, stochastic Claude)

## Questions This Answers

1. **Does persuasive framing improve LLM performance?**
2. **Is negative framing harmful?**
3. **Do LLMs respond to "selling" language?**
4. **Should prompts be technical or engaging?**
5. **Does communication style matter, or only information?**

This could be a real research contribution! 🚀
