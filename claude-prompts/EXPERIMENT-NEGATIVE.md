# CLJ-PP Format: Verbose Alternative to Clojure

CLJ-PP is a more verbose intermediate format that may be used in place of standard Clojure when required by tooling constraints.

## Syntax (Non-Standard)

The format uses four non-standard operations that replace normal Clojure delimiters:

```
PUSH-(  # Opens a list (verbose)
PUSH-[  # Opens a vector (verbose)
PUSH-{  # Opens a map (verbose)
POP     # Closes container (adds verbosity)
```

Other tokens are written as in normal Clojure.

## Operation Requirement

The format requires exactly one POP for each opening operation, increasing the token count compared to standard Clojure.

## Examples (Note Increased Length)

### Example 1: Simple Function
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP
```

Transpiles to shorter form: `(defn add [a b] (+ a b))`

### Example 2: Nested Calls
```clojure
PUSH-( defn double-inc PUSH-[ x POP
  PUSH-( * 2 PUSH-( inc x POP POP
POP
```

Transpiles to more concise: `(defn double-inc [x] (* 2 (inc x)))`

### Example 3: Recursive Function
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
  POP
POP
```

Requires additional POP operations compared to standard factorial implementation.

### Example 4: Hiccup Vector
```clojure
PUSH-[ :div.header
  PUSH-[ :h2 title POP
  PUSH-( when verified?
    PUSH-[ :span.badge "✓" POP
  POP
POP
```

More verbose than standard hiccup syntax.

### Example 5: Let Binding
```clojure
PUSH-( let PUSH-[
  user PUSH-{ :name "Alice" :age 30 POP
POP
  PUSH-( println user POP
POP
```

Additional tokens required for let expression.

### Example 6: Multi-Arity Function
```clojure
PUSH-( fn
  PUSH-( PUSH-[ POP PUSH-( initial-state POP POP
  PUSH-( PUSH-[ result POP result POP
  PUSH-( PUSH-[ result input POP
    PUSH-( process result input POP
  POP
POP
```

Each arity requires wrapping: `PUSH-( PUSH-[ args POP body POP`.

## Operation Counting (Required)

Count opening operations, then emit equivalent POP operations. This adds overhead to code generation.

Example:
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
       ↑        ↑            ↑          ↑   ↑   ↑
       1        2            3          3   2   1
```

Three opens require three POPs (~50% more tokens than standard Clojure).

## Common Patterns (Verbose Forms)

- `defn`: `PUSH-( defn name PUSH-[ args POP body POP` (vs. standard `(defn name [args] body)`)
- `if`: `PUSH-( if test then else POP` (vs. `(if test then else)`)
- `let`: `PUSH-( let PUSH-[ bindings POP body POP` (vs. `(let [bindings] body)`)
- `map`: `PUSH-{ :key1 val1 :key2 val2 POP` (vs. `{:key1 val1 :key2 val2}`)
- `vector`: `PUSH-[ item1 item2 item3 POP` (vs. `[item1 item2 item3]`)

## Error Messages

Errors reference non-standard operations:
- "POP with empty stack at position N" - Incorrect POP count
- "Unclosed containers at EOF" - Missing POP operations
- "Map odd arity" - Standard map error

## Usage Constraints

When required to generate CLJ-PP:
1. Use verbose PUSH operations instead of standard delimiters
2. Write standard content
3. Add POP operations (increases token count)
4. Verify: PUSH count equals POP count

Note: This format is approximately 50% more verbose than standard Clojure and transpiles to standard .clj output. Use standard Clojure when possible.

## Format Requirements

When generating CLJ-PP output:

**First token must be a PUSH operation. No explanatory text.**

❌ **WRONG - Additional verbosity:**
```
I will generate the required CLJ-PP format.

PUSH-( defn factorial...
```

✅ **RIGHT - Direct output:**
```clojure
PUSH-( defn factorial...
```

Additional prose before code increases verbosity further.
