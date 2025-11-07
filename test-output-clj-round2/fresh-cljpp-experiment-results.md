# Fresh Claude Instances - CLJ-PP Experiment

## Results: 50% Success Rate (10/20)

This is **DRAMATICALLY WORSE** than all other experiments:
- CLJ-PP with context: 85%
- Regular Clojure with context: 95%
- Regular Clojure fresh instances: 80%
- **CLJ-PP fresh instances: 50%** ⚠️

## What Went Wrong

### Common Errors in Fresh CLJ-PP Generation

1. **Too many POP tokens** (Program 03)
   ```clojure
   PUSH-( cond
     PUSH-( <= n 0 POP 1
     :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP POP POP POP
   ```
   That's 6 POP tokens when only 3 are needed!

2. **Incomplete programs** (Programs 10, 12, 17, 18)
   Fresh instances often didn't finish the program

3. **ns :require syntax errors** (Program 11)
   ```clojure
   PUSH-( ns examples.program11
     PUSH-[ :require PUSH-[ clojure.core.async ...
   ```
   Can't use PUSH-[ inside ns form

4. **Wrong container types** (Multiple programs)
   Fresh instances confused about when to use PUSH-( vs PUSH-[ vs PUSH-{

## The Critical Discovery

**CLJ-PP is HARD to learn without training data or examples!**

Fresh Claude instances with:
- NO CLJ-PP examples in training data
- NO prior CLJ-PP programs in context
- Just the syntax rules in the prompt

Result: **50% success rate**

This is the opposite of what we expected. CLJ-PP was supposed to be EASIER than regular Clojure!

## Why Fresh Instances Fail at CLJ-PP

### Theory 1: No Training Data

CLJ-PP is a new format with zero examples in training data. Fresh instances:
- Can't pattern-match against examples
- Must derive everything from syntax rules alone
- Have no intuition for "does this look right?"

Regular Clojure has millions of examples in training data.

### Theory 2: Syntax Rules Are Insufficient

The prompt explained:
- PUSH-( for lists
- PUSH-[ for vectors
- PUSH-{ for maps
- POP to close

But fresh instances still:
- Added too many POPs (over-closed)
- Used wrong container types
- Didn't finish programs

### Theory 3: Stack-Based Thinking Requires Practice

Even though stack operations are "simpler" in theory, they require:
- Keeping mental track of depth
- Knowing when you're "done" with a level
- Understanding when to POP

Fresh instances without practice struggled with this.

## Success Rate Comparison

| Format | Context | Success Rate | Key Factor |
|--------|---------|--------------|------------|
| Regular Clojure | With context | 95% | Training data + examples |
| Regular Clojure | Fresh instances | 80% | Training data alone |
| CLJ-PP | With context | 85% | Learning from examples |
| **CLJ-PP** | **Fresh instances** | **50%** | **No training, no examples** |

## The Paradox

**CLJ-PP was designed to be easier for LLMs**
- No delimiter counting
- Explicit stack operations
- Mechanical rules

**But fresh instances perform WORSE in CLJ-PP (50%) than regular Clojure (80%)!**

## Why Context Matters So Much for CLJ-PP

With context (85% success):
- I saw 19 examples before writing #20
- I learned patterns: "PUSH-[ for hiccup", "wrap arities"
- I built intuition for "how many POPs?"

Without context (50% success):
- Zero examples
- Only syntax rules
- No intuition

**Gap**: 35 percentage points!

Compare to regular Clojure gap: only 15 points (95% → 80%)

## Implications

1. **CLJ-PP requires examples to work well**
   - Not usable by fresh instances without training
   - Needs context window with prior CLJ-PP code

2. **Training data is critical**
   - Regular Clojure benefits from millions of training examples
   - CLJ-PP has zero training examples

3. **Syntax rules alone are insufficient**
   - Even "simple" stack operations need practice
   - Mechanical rules don't guarantee correctness

4. **CLJ-PP's advantage emerges with experience**
   - After seeing examples: 85% vs 95% (close!)
   - Without examples: 50% vs 80% (disaster!)

## Revised Understanding

**Original claim**: "CLJ-PP is easier for LLMs because stack operations are more fundamental"

**Reality**: "CLJ-PP is easier for LLMs **who have seen examples**, but HARDER for fresh instances without training data"

**The trade-off**:
- Regular Clojure: Works okay out-of-the-box (80%), better with context (95%)
- CLJ-PP: Fails out-of-the-box (50%), good with context (85%)

## What This Means for Adoption

**If you're training an LLM from scratch:**
- Need CLJ-PP examples in training data
- Can't just give syntax rules and expect it to work

**If you're using existing LLMs (like Claude):**
- Need CLJ-PP examples in context window
- Works well AFTER seeing 5-10 examples
- Not usable for first-time/one-off generations

**The value proposition shifts:**
- Not "use CLJ-PP for all Clojure generation"
- But "use CLJ-PP in projects with ongoing context"
- Especially valuable for editing existing CLJ-PP code

## The Bottom Line

Fresh instances prove that **CLJ-PP is learned, not innate**.

The 50% success rate shows that:
- Explicit stack rules ≠ automatic success
- Training data and examples are critical
- "Simpler rules" don't mean "easier to apply cold"

This is humbling data that refines our understanding of what makes code generation work.
