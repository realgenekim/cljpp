# POP-LINE + POP-ALL Feature Plan

## Motivation

Current CLJ-PP requires explicit counting:
```clojure
PUSH-( defn fibonacci PUSH-[ n POP
  PUSH-( cond
    PUSH-( = n 0 POP 0
    PUSH-( = n 1 POP 1
    :else PUSH-( +
      PUSH-( fibonacci PUSH-( - n 1 POP POP
      PUSH-( fibonacci PUSH-( - n 2 POP POP
    POP
  POP
POP
```

Mental work: "Count containers at each level, emit exact number of POPs"

With POP-LINE + POP-ALL:
```clojure
PUSH-( defn fibonacci PUSH-[ n POP
  PUSH-( cond
    PUSH-( = n 0 POP-LINE 0
    PUSH-( = n 1 POP-LINE 1
    :else PUSH-( +
      PUSH-( fibonacci PUSH-( - n 1 POP-LINE
      PUSH-( fibonacci PUSH-( - n 2 POP-ALL
```

Mental work:
- Middle of form? "Done with this line? → POP-LINE"
- Last line? "Close everything? → POP-ALL"

**Hypothesis:** Scope-based decisions (precise) are MUCH easier than arithmetic (counting)

**Key insight:** POP-ALL eliminates ALL trailing POPs by closing the entire stack!

## The Three Operations (Precise Scope Indicators)

**POP** = Close exactly one container
**POP-LINE** = Close all containers opened on this line
**POP-ALL** = Close ALL containers in the entire stack

## Semantics

**POP** = Close exactly one container (the most recent PUSH)

**POP-LINE** = Close all containers opened on the current line, in reverse order (LIFO)

**Example:**
```clojure
:else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-LINE
      ↑1     ↑2              ↑3           ↑ closes 3,2,1 (line scope)
```

Containers opened on this line: `*`, `factorial`, `-`
POP-LINE closes them in reverse: `-`, then `factorial`, then `*`

**POP-ALL** = Close ALL containers in the entire stack (terminates the form)

**Example:**
```clojure
PUSH-( defn foo PUSH-[ x POP
  PUSH-( if PUSH-( > x 10 POP-LINE
    PUSH-( * 2 x POP-ALL    # Closes: *, if, defn (everything!)
```

Stack before POP-ALL: `defn`, `if`, `*` (3 containers)
POP-ALL closes all: `*`, then `if`, then `defn`
Stack after: empty ✅

## When to Use Each

### Use POP-LINE

When you've opened multiple containers on a line and want to close them all:

```clojure
# Nested function calls
PUSH-( * 2 PUSH-( inc x POP-LINE
                        ↑
                     closes: inc, *
→ (* 2 (inc x))

# Deeply nested
PUSH-( f PUSH-( g PUSH-( h PUSH-( i x POP-LINE
                                    ↑
                                 closes: i, h, g, f
→ (f (g (h (i x))))

# Condition branches
PUSH-( if PUSH-( > x 10 POP-LINE PUSH-( inc x POP-LINE PUSH-( dec x POP-LINE
                         ↑                      ↑                      ↑
                    closes: >              closes: inc            closes: dec

→ (if (> x 10) (inc x) (dec x))
```

### Use POP-ALL

When you're completely done with the entire form:

```clojure
# Last line of function - close everything!
PUSH-( defn foo PUSH-[ x POP
  PUSH-( * 2 x POP-ALL
                ↑
             closes: *, defn (entire stack!)

→ (defn foo [x]
    (* 2 x))  ← POP-ALL closed * AND defn

# Last branch of conditional
PUSH-( if PUSH-( > x 10 POP-LINE
    PUSH-( inc x POP-LINE
    PUSH-( dec x POP-ALL
                 ↑
              closes: dec, if (everything!)

→ (if (> x 10)
    (inc x)
    (dec x))  ← POP-ALL closed dec AND if
```

### Use POP (explicit)

When you need granular control:

```clojure
# Close vector but continue function
PUSH-( defn add PUSH-[ a b POP      # Just closes vector
  PUSH-( + a b POP-ALL               # Closes the +
POP                                   # Closes defn

# Add more args after closing inner form
PUSH-( foo PUSH-[ 1 2 3 POP :extra-arg POP
```

### Use Individual POPs

For multi-line structures:

```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP-ALL 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
  POP
POP
```

Each closing POP is on its own line, clear and explicit.

## Complete Example

```clojure
# Before (explicit counting)
PUSH-( defn fibonacci PUSH-[ n POP
  PUSH-( cond
    PUSH-( = n 0 POP 0
                 ↑
              closes: =
    PUSH-( = n 1 POP 1
                 ↑
              closes: =
    :else PUSH-( +
      PUSH-( fibonacci PUSH-( - n 1 POP POP
                                     ↑   ↑
                                 close - close fibonacci
      PUSH-( fibonacci PUSH-( - n 2 POP POP
                                     ↑   ↑
                                 close - close fibonacci
    POP  ← close +
  POP  ← close cond
POP  ← close defn

# After (with POP-LINE + POP-ALL)
PUSH-( defn fibonacci PUSH-[ n POP
  PUSH-( cond
    PUSH-( = n 0 POP-LINE 0
                 ↑
              closes: = (line scope)
    PUSH-( = n 1 POP-LINE 1
                 ↑
              closes: = (line scope)
    :else PUSH-( +
      PUSH-( fibonacci PUSH-( - n 1 POP-LINE
                                    ↑
                                 closes: -, fibonacci (line scope)
      PUSH-( fibonacci PUSH-( - n 2 POP-ALL
                                    ↑
                                 closes: -, fibonacci, +, cond, defn (EVERYTHING!)

→ (defn fibonacci [n]
    (cond
      (= n 0) 0
      (= n 1) 1
      :else (+
        (fibonacci (- n 1))
        (fibonacci (- n 2)))))
```

**Reduction:** 12 explicit POPs → 4 POP-LINE + 1 POP-ALL + 1 POP
**Mental load:** No counting! Just scope decisions ("line done?" or "completely done?")

## Implementation Plan

### Phase 1: Parser Support (Don't Tell LLMs)

**Goal:** Implement POP-ALL in parser, but don't document it in prompts yet.

**Changes needed:**

1. **Lexer** (`src/cljp/core.clj`)
   - Add `POP-ALL` as recognized keyword (like PUSH-(, POP)
   - Token type: `:pop-all`

2. **Parser** (`src/cljp/core.clj`)
   - Track "line-local depth" - how many PUSHes on current line haven't been POPed
   - When seeing `POP-ALL`:
     * Pop all containers opened since start of current line
     * Emit corresponding closing delimiters
     * Reset line-local depth counter

3. **Error handling**
   - `POP-ALL with no containers opened on this line` → meaningful error
   - Track line numbers for better error messages

4. **Tests**
   - Test POP-ALL with 1, 2, 3, 4+ containers on a line
   - Test mixing POP and POP-ALL
   - Test error cases

### Phase 2: Baseline Testing (No POP-ALL in Prompt)

**Goal:** Establish baseline with current explicit POP approach.

Run fresh experiments with enhanced CLJPP-PROMPT.md (current version):
```bash
./run-fresh-experiment-cljpp.sh  # 20 programs
```

Expected result: ~80-90% success (based on 100% on single program)

**Metrics to collect:**
- Success rate
- Transpile error rate
- Load error rate
- Types of errors (POP counting vs other)

### Phase 3: Add POP-ALL to Prompt

**Goal:** Test if POP-ALL improves generation.

**Create CLJPP-PROMPT-WITH-POP-ALL.md:**

Add sections:
```markdown
## POP-ALL: Close Multiple Containers at Once

When you've opened multiple containers on a line and want to close them all:

PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL

Instead of counting POPs, ask: "Am I done with this expression? Yes → POP-ALL"

### When to Use

✅ Use POP-ALL: Nested calls at end of line
✅ Use POP: Need to continue outer form
✅ Use both: Mix as needed

### Examples

# Simple nested call
PUSH-( * 2 PUSH-( inc x POP-ALL

# Deep nesting
PUSH-( a PUSH-( b PUSH-( c PUSH-( d x POP-ALL

# But NOT here (defn stays open)
PUSH-( defn add PUSH-[ a b POP  ← Still use POP
  ...
```

### Phase 4: Comparative Testing

**Test both prompts on same 20 programs:**

```bash
# Test 1: Explicit POP only (current prompt)
./run-fresh-experiment-cljpp.sh --prompt CLJPP-PROMPT.md

# Test 2: With POP-ALL available
./run-fresh-experiment-cljpp.sh --prompt CLJPP-PROMPT-WITH-POP-ALL.md

# Run each 3 times for statistical significance
```

**Metrics to compare:**
- Success rate (explicit POP vs POP-ALL)
- Error types (fewer POP counting errors with POP-ALL?)
- Generated code length (fewer POPs = shorter?)
- Subjective: which feels more natural?

### Phase 5: Analysis

**Questions to answer:**

1. **Does POP-ALL improve success rate?**
   - Hypothesis: Should be same or better (100% → 100% or 90% → 100%)

2. **Does POP-ALL reduce cognitive load?**
   - Measure: Fewer POP-counting errors in failure modes
   - Subjective: Does it feel more natural when generating?

3. **What patterns emerge?**
   - Do LLMs consistently use POP-ALL for nested calls?
   - Do they correctly choose POP when needed?

4. **Are there failure modes?**
   - Do LLMs over-use POP-ALL when they should use POP?
   - Do error messages help recovery?

## Expected Outcomes

### Optimistic Case
- POP-ALL reduces cognitive load significantly
- Success rate: 90% → 95% or 100%
- Generated code is cleaner, easier to read
- **Decision:** Make POP-ALL the recommended approach

### Realistic Case
- POP-ALL provides marginal improvement
- Success rate: 90% → 92%
- Some LLMs prefer it, others don't
- **Decision:** Offer both, let users choose

### Pessimistic Case
- POP-ALL confuses LLMs (when to use which?)
- Success rate: 90% → 85%
- More syntax = more confusion
- **Decision:** Keep explicit POP only, document POP-ALL as advanced

## Alternative: POP with Count

Instead of POP-ALL, what about `POP 3`?

```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP 3
```

**Pros:**
- Explicit count (self-documenting)
- No ambiguity about scope

**Cons:**
- Back to counting (defeats purpose)
- More verbose than POP-ALL
- Easy to get count wrong

**Verdict:** POP-ALL is better (semantic vs arithmetic)

## Implementation Checklist

- [ ] Add POP-ALL to lexer
- [ ] Implement POP-ALL in parser with line-tracking
- [ ] Add error messages for POP-ALL misuse
- [ ] Write tests for POP-ALL
- [ ] Run baseline experiments (no POP-ALL in prompt)
- [ ] Create CLJPP-PROMPT-WITH-POP-ALL.md
- [ ] Run comparative experiments
- [ ] Analyze results
- [ ] Decide: recommend POP-ALL or keep explicit POP
- [ ] Update documentation based on findings

## Success Criteria

**Minimum bar:** POP-ALL doesn't make things worse (success rate stays ≥90%)

**Success:** POP-ALL improves success rate by 5+ percentage points OR significantly reduces POP-counting errors

**Home run:** POP-ALL achieves 95%+ success rate AND fresh LLMs consistently use it correctly

## Timeline

- Phase 1 (Implementation): 2-3 hours
- Phase 2 (Baseline): 30 minutes
- Phase 3 (Prompt update): 1 hour
- Phase 4 (Comparative testing): 1 hour
- Phase 5 (Analysis): 1-2 hours

**Total:** ~6-8 hours

## Notes

**The key insight:** This isn't about fixing broken syntax. The explicit POP approach already works (100% with good prompts). This is about reducing cognitive load - making the easy cases easier while keeping full control available.

**The empirical test:** By implementing POP-ALL in the parser but not documenting it initially, we establish a true baseline. Then we can measure the exact impact of adding it to the prompt.

This is rigorous experimentation. 🔬
