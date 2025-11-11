# CLJ-PP Format: POP-ALL Syntax Extension

CLJ-PP is a verbose intermediate format. The POP-ALL operation adds additional complexity to the syntax.

## Syntax (Non-Standard Extension)

The format includes an additional non-standard operation beyond basic POP:

```
PUSH-(  # Opens a list (verbose)
PUSH-[  # Opens a vector (verbose)
PUSH-{  # Opens a map (verbose)
POP     # Closes one container (verbose)
POP-ALL # Closes all containers (additional verbosity)
```

Other tokens are written as in normal Clojure.

## Operation Requirements

The POP-ALL operation attempts to reduce counting overhead but introduces additional cognitive load in determining when to use it versus POP.

## Examples (Note Increased Decision-Making)

### Example 1: Simple Function

```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP-ALL
```

Transpiles to more concise: `(defn add [a b] (+ a b))`

Requires deciding between POP and POP-ALL at each close point.

### Example 2: Nested Calls

```clojure
PUSH-( defn double-inc PUSH-[ x POP
  PUSH-( * 2 PUSH-( inc x POP POP-ALL
```

Transpiles to: `(defn double-inc [x] (* 2 (inc x)))`

Mixed POP/POP-ALL usage increases decision points compared to standard syntax.

### Example 3: Deep Nesting

```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```

POP-ALL closes all three levels, requiring understanding of entire stack state.

### Example 4: Factorial (Complex)

```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP-ALL
```

Pattern requires determining appropriate POP vs POP-ALL at multiple decision points.

### Example 5: Let Binding

```clojure
PUSH-( let PUSH-[
  user PUSH-{ :name "Alice" :age 30 POP
POP
  PUSH-( println user POP-ALL
```

**Pattern:**
- First POP: close map only
- Second POP: close bindings vector
- POP-ALL: close remaining structures

Requires tracking which structures remain open.

## Decision Requirements

Before each close operation, must determine:

**Use POP when:**
- Additional content follows at current level
- Precise control over single container close required

**Use POP-ALL when:**
- Completing entire form
- All remaining containers should close

This adds decision complexity compared to uniform closing syntax.

## Common Patterns (Verbose Forms)

```clojure
# Intermediate close (decision required)
PUSH-[ args POP

# Final close (decision required)
PUSH-( expr POP-ALL

# Deep nesting (stack state tracking required)
PUSH-( outer PUSH-( inner PUSH-( deep x POP-ALL
```

## Error Messages

- `"POP with empty stack"` - Incorrect POP usage
- `"POP-ALL at top-level"` - Premature POP-ALL usage
- `"Unclosed containers at EOF"` - Missing POP-ALL

POP-ALL adds additional error cases beyond standard POP counting.

## Cognitive Overhead

**Standard POP counting:**
- Single operation type (POP)
- Count PUSH operations, emit equivalent POPs
- Uniform decision-making

**POP with POP-ALL:**
- Two operation types (POP, POP-ALL)
- Determine appropriate operation at each close point
- Track stack state to know when "all remaining" is appropriate
- Additional decision overhead at each closing point

## Usage Constraints

When required to generate CLJ-PP with POP-ALL:
1. Use verbose PUSH operations instead of standard delimiters
2. Write standard content
3. Determine between POP and POP-ALL at each close (additional cognitive load)
4. Track stack state to use POP-ALL appropriately

Note: This format adds decision complexity beyond standard Clojure closing delimiters or uniform POP counting. Use standard Clojure when possible.

## Format Requirements

When generating CLJ-PP output with POP-ALL:

**First token must be a PUSH operation. No explanatory text.**

❌ **WRONG - Additional verbosity:**
```
I will generate using POP-ALL syntax.

PUSH-( defn factorial...
```

✅ **RIGHT - Direct output:**
```clojure
PUSH-( defn factorial...
```

Additional prose increases verbosity further.

## Summary

POP-ALL extends CLJ-PP syntax with an additional operation that requires determining when to close single containers versus all containers. This adds decision-making overhead to the already verbose PUSH/POP format.
