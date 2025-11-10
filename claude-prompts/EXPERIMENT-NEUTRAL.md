# CLJ-PP Format Specification

CLJ-PP is an intermediate representation for Clojure code that uses explicit stack operations instead of closing delimiters.

## Syntax

Four operations are available:

```
PUSH-(  # Opens a list
PUSH-[  # Opens a vector
PUSH-{  # Opens a map
POP     # Closes the most recently opened container
```

All other tokens (symbols, keywords, strings, numbers) are written normally.

## Operation Rule

Each opening operation (PUSH-( , PUSH-[, PUSH-{) requires exactly one corresponding POP operation.

## Examples

### Example 1: Simple Function
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP
```

Transpiles to: `(defn add [a b] (+ a b))`

### Example 2: Nested Calls
```clojure
PUSH-( defn double-inc PUSH-[ x POP
  PUSH-( * 2 PUSH-( inc x POP POP
POP
```

Transpiles to: `(defn double-inc [x] (* 2 (inc x)))`

### Example 3: Recursive Function
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
  POP
POP
```

Transpiles to factorial implementation.

### Example 4: Hiccup Vector
```clojure
PUSH-[ :div.header
  PUSH-[ :h2 title POP
  PUSH-( when verified?
    PUSH-[ :span.badge "✓" POP
  POP
POP
```

Transpiles to hiccup component.

### Example 5: Let Binding
```clojure
PUSH-( let PUSH-[
  user PUSH-{ :name "Alice" :age 30 POP
POP
  PUSH-( println user POP
POP
```

Transpiles to let expression with map binding.

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

Each arity is wrapped in `PUSH-( PUSH-[ args POP body POP`.

## Operation Counting

For any expression, count the number of opening operations used, then emit that same number of POP operations.

Example:
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
       ↑        ↑            ↑          ↑   ↑   ↑
       1        2            3          3   2   1
```

Three opening operations require three POP operations.

## Common Patterns

- `defn`: `PUSH-( defn name PUSH-[ args POP body POP`
- `if`: `PUSH-( if test then else POP`
- `let`: `PUSH-( let PUSH-[ bindings POP body POP`
- `map`: `PUSH-{ :key1 val1 :key2 val2 POP`
- `vector`: `PUSH-[ item1 item2 item3 POP`

## Error Messages

Transpiler errors include precise position information:
- "POP with empty stack at position N" - More POP operations than PUSH operations
- "Unclosed containers at EOF" - Insufficient POP operations
- "Map odd arity" - Map contains unpaired key-value pairs

## Code Generation Instructions

When generating CLJ-PP code:
1. Open containers with PUSH operations
2. Write content normally
3. Close containers with POP operations
4. Verify: count of PUSH operations equals count of POP operations
