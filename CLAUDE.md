# CLJ-PP Tokenizer Project Notes

## Project Context

This is an experiment in creating a stack-based intermediate format (CLJ-PP) for generating Clojure code. The hypothesis: explicitly tracking PUSH/POP operations is easier for LLMs than counting closing delimiters.

## Success Rates Summary

| Approach | Success Rate | Notes |
|----------|--------------|-------|
| Regular Clojure (with context) | 95% | I wrote 20 programs with full conversation context |
| Regular Clojure (fresh instances) | **80%** | 20 independent `claude --print` calls, no context |
| CLJ-PP (with context) | 85% | I wrote 20 programs in CLJ-PP format |
| CLJ-PP (fresh instances, original) | 50% | No examples in prompt - failed dramatically |
| CLJ-PP (fresh instances, enhanced prompt) | **100%** | Added examples to prompt - perfect! |
| CLJ-PP with POP-LINE + POP-ALL | **80%** | ⚠️ POP-LINE introduced scope ambiguity |

## Latest Experiment: POP-LINE and POP-ALL (2025-11-06)

### Implementation

Added two new closing operations:
- **POP-LINE**: Closes all containers opened on current line
- **POP-ALL**: Closes all containers in entire stack

### Hypothesis

Making closing operations semantic ("am I done?") instead of arithmetic ("how many?") would improve success rate.

### Results

**HYPOTHESIS REJECTED!**

- Baseline (explicit POP): **90% success** (9/10)
- Enhanced (POP-LINE + POP-ALL): **80% success** (8/10)

POP-LINE actually HURT performance by introducing scope ambiguity.

### The Problem with POP-LINE

```clojure
:else PUSH-( +                         ← Line 1: opens +
  PUSH-( fibonacci PUSH-( - n 1 POP-LINE   ← Line 2: opens fibonacci, -
```

**Fresh instance thinks:** "I'm done with this line → POP-LINE"

**What POP-LINE actually does:** Closes containers from line 2 only (`fibonacci`, `-`)

**What fresh instance expected:** Close everything needed to finish this expression (including `+`)

**Result:** Logic error - second fibonacci call ends up outside the `+`:
```clojure
:else (+ (fibonacci (- n 1))) (fibonacci (- n 2))  ← WRONG!
```

### Key Learning

**More options ≠ better!** 

The explicit POP counting approach is unambiguous:
- Count PUSHes on a line
- Emit that many POPs
- No scope questions

POP-LINE added cognitive complexity that fresh instances couldn't navigate.

## Second Experiment: POP-ALL Only (2025-11-06)

### Hypothesis

Remove POP-LINE (scope ambiguity) but keep POP-ALL (unambiguous "I'm done").

### Result

**80% success** (8/10) - same as POP-LINE+POP-ALL!

### The Problem

Fresh instances got confused about **when to use POP-ALL vs trailing POPs**:

```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL  ← Closes everything
  POP   ← ERROR! Stack already empty
POP     ← ERROR! Stack already empty
```

Introduced **new decision fatigue**: "Should I use POP-ALL here or save it for later?"

## Final Verdict

**Winner: Baseline CLJ-PP (explicit POP) - 90% success** ✅

| Approach | Success | Why |
|----------|---------|-----|
| Regular Clojure | 80% | Delimiter counting is hard |
| **CLJ-PP (explicit POP)** | **90%** ✅ | **Unambiguous counting** |
| CLJ-PP + POP-LINE + POP-ALL | 80% | Scope ambiguity (when does POP-LINE apply?) |
| CLJ-PP + POP-ALL only | 80% | Decision ambiguity (when to use POP-ALL?) |

**Key insight:** **Simple and tedious beats clever and ambiguous.**

For fresh LLM instances:
- **Counting is easier than deciding**
- **One rule (POP per PUSH) is clearer than multiple options**
- **Arithmetic is unambiguous, semantics are subjective**

### Recommendation

**Keep CLJ-PP as-is** (PUSH/POP only):
- 90% success beats regular Clojure's 80%
- Zero ambiguity - count PUSHes, emit POPs
- Simple mental model
- Proven to work with enhanced prompt + examples

## File Organization

- `experiments/` - All experiment runs (timestamped, never overwritten)
- `experiments/EXPERIMENT-RESULTS.md` - Detailed analysis of POP-LINE/POP-ALL experiment
- `CLJPP-PROMPT.md` - Current prompt (explicit POP, 100% success on factorial)
- `CLJPP-PROMPT-WITH-POP-LINE-ALL.md` - Enhanced prompt with both (80% success)
- `CLJPP-PROMPT-WITH-POP-ALL-ONLY.md` - (TO BE CREATED) Just POP-ALL, no POP-LINE
- `test-tuning/` - Test runs for prompt tuning (may be overwritten)

## Commands

```bash
# Test baseline (explicit POP)
./test-one-program.sh 3 10

# Test with POP-LINE and POP-ALL
./test-one-program-with-pop-line-all.sh 3 10

# Test with POP-ALL only (to be created)
./test-one-program-with-pop-all-only.sh 3 10

# Run full test suite
make runtests-once
```

## Key Insights

1. **Examples matter**: Fresh instances need examples, not just syntax rules (50% → 100%)
2. **Simplicity wins**: Explicit counting (90%) beat semantic operations (80%)
3. **Ambiguity kills**: POP-LINE's scope ambiguity caused logic errors
4. **POP-ALL is promising**: Unambiguous "I'm done" marker might help

## References

- Original results: `test-output-clj-round2/fresh-experiment-results.md`
- CLJ-PP results: `test-output-clj-round2/fresh-cljpp-experiment-results.md`
- Prompt tuning notes: `PROMPT-TUNING-NOTES.md`
- Implementation plan: `plans/implementation-plan.md`
- POP-ALL feature plan: `plans/pop-all-feature.md`
