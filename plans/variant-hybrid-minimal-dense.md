# CLJ-PP Prompt Variant: Minimal + Dense Examples + Clojure Leverage

**Strategy:** Combine the bitter lesson (leverage Clojure training data) with dense in-context learning examples that cover all observed failure modes.

## Design Principles

1. **Leverage training data**: Explicitly tell LLM it's great at Clojure
2. **Minimal rules**: ONE POP PER PUSH
3. **Critical edge cases**: Address `#()` confusion explicitly
4. **Dense examples**: 15-20 examples covering all failure modes
5. **No fluff**: Start code immediately with PUSH-

## Observed Failure Modes (from 80% success rate)

From comprehensive test results (16/20 passing):

**Failures:**
- **Program 04, 13**: `#PUSH-( > % 0 POP` - Mixed Clojure `#()` syntax with CLJPP
- **Program 17**: Multi-arity `defn` with `letfn` - Complex nesting

**Success patterns:**
- Simple `defn`, `let`, `map`, `filter` - All worked
- Direct Clojure patterns translated 1:1

## The Prompt

---

# CLJ-PP Generation Guide for Claude

## You're Already Great at This

You have excellent Clojure training (95% success rate). CLJ-PP is just Clojure where delimiters are explicit stack operations instead of implicit matching.

## Core Syntax (4 tokens total)

```
PUSH-(  # Opens list (for functions, calls, if, let, etc.)
PUSH-[  # Opens vector (for parameters, data, hiccup)
PUSH-{  # Opens map (for {:key value})
POP     # Closes current container (assembler picks ), ], or })
```

Everything else (symbols, keywords, strings, numbers) - write normally.

## The Golden Rule

**ONE POP PER PUSH**

Count opening delimiters in the Clojure pattern → That's how many POPs you need.

## Critical: No Clojure Reader Macros

❌ **WRONG:** `#PUSH-( > % 5 POP` (mixing Clojure `#()` with CLJPP)
❌ **WRONG:** `#(> % 5)` (pure Clojure - not CLJPP syntax)
✅ **RIGHT:** `PUSH-( fn PUSH-[ % POP PUSH-( > % 5 POP POP`

**Rule:** Expand `#(...)` anonymous functions to full `fn` form with PUSH/POP.

## Examples - Learn These Patterns

### Example 1: Simple Function
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP
```
Pattern: `(defn f [x] body)` → `PUSH-( defn f PUSH-[ x POP body POP`

### Example 2: Nested Calls
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
  POP
POP
```
Pattern: `(* n (factorial (- n 1)))` → 3 opening parens = 3 POPs

### Example 3: Anonymous Function (Critical!)
```clojure
PUSH-( filter PUSH-( fn PUSH-[ x POP PUSH-( > x 18 POP POP users POP
```
Pattern: `#(> % 18)` expands to `(fn [x] (> x 18))` → `PUSH-( fn PUSH-[ x POP PUSH-( > x 18 POP POP`

### Example 4: Anonymous Function with %
```clojure
PUSH-( map PUSH-( fn PUSH-[ % POP PUSH-( * % 2 POP POP coll POP
```
Pattern: `#(* % 2)` → Use `%` as parameter name in expanded fn

### Example 5: Let Binding
```clojure
PUSH-( let PUSH-[
  x 10
  y 20
POP
  PUSH-( + x y POP
POP
```
Pattern: `(let [x v] body)` → `PUSH-( let PUSH-[ bindings POP body POP`

### Example 6: Map Literal
```clojure
PUSH-{ :name "Alice" :age 30 :role "admin" POP
```
Pattern: `{:k v}` → `PUSH-{ :k v POP`

### Example 7: Vector of Maps
```clojure
PUSH-[
  PUSH-{ :name "Alice" :age 25 POP
  PUSH-{ :name "Bob" :age 17 POP
  PUSH-{ :name "Charlie" :age 30 POP
POP
```
Pattern: Nest containers, close inner before outer

### Example 8: Filter with Anonymous Function
```clojure
PUSH-( defn active-users PUSH-[ users POP
  PUSH-( filter PUSH-( fn PUSH-[ user POP
    PUSH-( > PUSH-( :age user POP 18 POP
  POP POP users POP
POP
```
Pattern: `#(> (:age %) 18)` fully expanded

### Example 9: Threading Macro
```clojure
PUSH-( ->
  data
  PUSH-( update :count inc POP
  PUSH-( assoc :processed true POP
POP
```
Pattern: `->` is just a function, treat normally

### Example 10: Multi-arity defn
```clojure
PUSH-( defn foo
  PUSH-( PUSH-[ POP
    default-value
  POP
  PUSH-( PUSH-[ x POP
    PUSH-( process x POP
  POP
  PUSH-( PUSH-[ x y POP
    PUSH-( process x y POP
  POP
POP
```
Pattern: Each arity is `PUSH-( PUSH-[ args POP body POP`

### Example 11: letfn
```clojure
PUSH-( letfn PUSH-[
  PUSH-( PUSH-[ helper PUSH-[ n POP
    PUSH-( when PUSH-( < n end POP
      PUSH-( cons n PUSH-( helper PUSH-( inc n POP POP POP
    POP
  POP POP
POP
  PUSH-( helper start POP
POP
```
Pattern: `letfn` bindings are function definitions: `PUSH-( PUSH-[ name PUSH-[ args POP body POP POP`

### Example 12: Destructuring in let
```clojure
PUSH-( let PUSH-[
  PUSH-{ :keys PUSH-[ name age POP :as user POP data
POP
  PUSH-( println name age POP
POP
```
Pattern: Destructuring is just data in binding vector

### Example 13: Hiccup with Conditionals
```clojure
PUSH-[ :div.container
  PUSH-[ :h1 title POP
  PUSH-( when logged-in?
    PUSH-[ :button.logout "Logout" POP
  POP
  PUSH-[ :p content POP
POP
```
Pattern: Mix vectors (hiccup) with lists (functions) normally

### Example 14: Spec Definition with Anonymous Function
```clojure
PUSH-( s/def ::age
  PUSH-( s/and int?
    PUSH-( fn PUSH-[ % POP PUSH-( > % 0 POP POP
  POP
POP
```
Pattern: `#(> % 0)` → `PUSH-( fn PUSH-[ % POP PUSH-( > % 0 POP POP`

### Example 15: Complex Nested Expression
```clojure
PUSH-( defn process PUSH-[ data POP
  PUSH-( ->>
    data
    PUSH-( filter PUSH-( fn PUSH-[ x POP PUSH-( odd? x POP POP POP
    PUSH-( map PUSH-( fn PUSH-( x POP PUSH-( * x 2 POP POP POP
    PUSH-( reduce + POP
  POP
POP
```
Pattern: Count each PUSH, emit exactly that many POPs

## How to Count POPs

For each line/expression:
1. Count how many `PUSH-(`, `PUSH-[`, `PUSH-{` you wrote
2. Write exactly that many `POP`s
3. Inner containers close before outer containers

Example breakdown:
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
       ↑        ↑            ↑          ↑   ↑   ↑
       |        |            |          |   |   |
    open *   open fac    open -    close - | close *
                                     close fac
```

## Common Errors to Avoid

### Error 1: Mixing Clojure Syntax
```clojure
❌ WRONG: #PUSH-( > % 5 POP
❌ WRONG: #(> % 5)
✅ RIGHT: PUSH-( fn PUSH-[ % POP PUSH-( > % 5 POP POP
```

### Error 2: Wrong POP Count
```clojure
❌ WRONG: PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP
          # 3 PUSHes but only 2 POPs!
✅ RIGHT: PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
          # 3 PUSHes, 3 POPs
```

### Error 3: Adding Explanation Text
```clojure
❌ WRONG:
I'll write the factorial function in CLJ-PP format.

PUSH-( defn factorial...

✅ RIGHT:
PUSH-( defn factorial...
```
**Rule:** Start immediately with `PUSH-`. No explanatory text before code.

## Quick Reference

**defn**: `PUSH-( defn name PUSH-[ args POP body POP`
**if**: `PUSH-( if test then else POP`
**let**: `PUSH-( let PUSH-[ bindings POP body POP`
**map**: `PUSH-{ :key1 val1 :key2 val2 POP`
**vector**: `PUSH-[ item1 item2 item3 POP`
**anonymous fn**: `PUSH-( fn PUSH-[ % POP body POP` (NOT `#(...)`)
**nested call**: Count opens, emit that many POPs

## Remember

You already know Clojure. CLJ-PP just makes delimiters explicit:
- Each `(` in Clojure → `PUSH-(` in CLJ-PP
- Each `)` in Clojure → `POP` in CLJ-PP
- Count them, match them, done.

Start code immediately with `PUSH-`.
