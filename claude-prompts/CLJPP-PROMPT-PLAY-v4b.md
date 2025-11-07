# CLJ-PP Generation Guide for Claude

## You're Already Great at This

You have excellent Clojure training (95% success rate). CLJ-PP is just Clojure where delimiters are explicit stack operations instead of implicit matching.

## Core Syntax (5 tokens total)

```
PUSH-(   # Opens list (for functions, calls, if, let, etc.)
PUSH-[   # Opens vector (for parameters, data, hiccup)
PUSH-{   # Opens map (for {:key value})
POP      # Closes current container (assembler picks ), ], or })
POP-ALL  # Closes ALL remaining containers (use at end of forms)
```

Everything else (symbols, keywords, strings, numbers) - write normally.

## The Two Rules

1. **ONE POP PER PUSH** - Count opening delimiters → That's how many POPs you need
2. **POP-ALL MEANS STOP** - Never write POPs after POP-ALL (it closes everything!)

## Critical: No Clojure Reader Macros

❌ **WRONG:** `#(> % 5)` (Clojure shorthand - not CLJPP)
✅ **RIGHT:** `PUSH-( fn PUSH-[ x POP PUSH-( > x 5 POP POP`

**Rule:** Expand `#(...)` anonymous functions to full `fn` form with PUSH/POP.

## Examples - Learn These Patterns

### Example 1: Simple Function with POP-ALL
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP-ALL
```
POP-ALL closes `+` then closes `defn`. Done!

### Example 2: Simple Function with Explicit POPs
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP
```
Both approaches work. Use POP-ALL when you're completely done.

### Example 3: Nested Calls with POP-ALL
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```
POP-ALL on last line closes everything: `-`, `factorial`, `*`, `if`, `defn`.

### Example 4: Factorial with Cond and POP-ALL
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```
Last line of last clause → POP-ALL closes all remaining containers.

### Example 5: Anonymous Function with Explicit POPs (when in middle of code)
```clojure
PUSH-( filter PUSH-( fn PUSH-[ x POP PUSH-( > x 18 POP POP users POP
```
Pattern: `#(> % 18)` → `(fn [x] (> x 18))` → Expand to PUSH/POP
**Note:** This uses explicit POPs because it's in the middle - not at the end yet!

### Example 6: Anonymous Function with POP-ALL
```clojure
PUSH-( defn active-users PUSH-[ users POP
  PUSH-( filter PUSH-( fn PUSH-[ user POP
    PUSH-( > PUSH-( :age user POP 18 POP-ALL
```
POP-ALL closes body, fn, filter, and defn all at once.

### Example 7: Common #() Expansions
```clojure
# Original: #(> % 18)
PUSH-( fn PUSH-[ x POP PUSH-( > x 18 POP POP

# Original: #(* 2 %)
PUSH-( fn PUSH-[ x POP PUSH-( * 2 x POP POP

# Original: #(:name %)
PUSH-( fn PUSH-[ x POP PUSH-( :name x POP POP
```

### Example 8: Map Literal
```clojure
PUSH-{ :name "Alice" :age 30 :role "admin" POP
```
Pattern: `{:k v}` → `PUSH-{ :k v POP`

### Example 9: Vector of Maps
```clojure
PUSH-[
  PUSH-{ :name "Alice" :age 25 POP
  PUSH-{ :name "Bob" :age 17 POP
  PUSH-{ :name "Charlie" :age 30 POP
POP
```
Close inner containers before outer ones.

### Example 10: Let Binding
```clojure
PUSH-( let PUSH-[
  x 10
  y 20
POP
  PUSH-( + x y POP-ALL
```
POP-ALL on last line closes the `+` and the `let`.

### Example 11: Fibonacci with POP-ALL
```clojure
PUSH-( defn fibonacci PUSH-[ n POP
  PUSH-( cond
    PUSH-( = n 0 POP 0
    PUSH-( = n 1 POP 1
    :else PUSH-( +
      PUSH-( fibonacci PUSH-( - n 1 POP POP
      PUSH-( fibonacci PUSH-( - n 2 POP-ALL
```
Last line uses POP-ALL to close everything.

### Example 11b: Multiple defns in Same File (CRITICAL!)
```clojure
PUSH-( defn active-users PUSH-[ users POP
  PUSH-( filter PUSH-( fn PUSH-[ user POP
    PUSH-( > PUSH-( :age user POP 18 POP POP POP users POP
POP

PUSH-( defn user-names PUSH-[ users POP
  PUSH-( map :name users POP-ALL
```
**Notice:**
- First defn: Close body with POP POP POP, then close filter with POP, then close defn with POP (5 POPs total - NOT POP-ALL)
- Second defn: Last form in file, so use POP-ALL

### Example 12: Multi-arity defn
```clojure
PUSH-( defn foo
  PUSH-( PUSH-[ POP
    default-value
  POP
  PUSH-( PUSH-[ x POP
    PUSH-( process x POP-ALL
```
Last arity uses POP-ALL to close everything.

## When to Use POP vs POP-ALL

**Use POP when:**
- You're in the middle of a form (NOT at the very end)
- There's more code coming after this line
- You're closing intermediate containers
- **Example:** `PUSH-( filter PUSH-( fn ... POP POP users POP` - The `filter` call is complete, so use explicit POPs

**Use POP-ALL ONLY when:**
- You're on the LAST line of a top-level form (defn, def, ns) AND there's no more code in that form
- You're on the LAST line of the LAST clause in a cond/case
- You are absolutely certain NOTHING else comes after this line

**CRITICAL: If you have multiple top-level forms (multiple defn, def), POP-ALL ONLY on the LAST line of each form!**

**CRITICAL ERROR TO AVOID:**
```clojure
❌ WRONG:
PUSH-( defn foo PUSH-[ x POP
  PUSH-( * 2 x POP-ALL
POP  ← ERROR! POP-ALL already closed defn!

✅ RIGHT:
PUSH-( defn foo PUSH-[ x POP
  PUSH-( * 2 x POP-ALL
```

## Quick Patterns Reference

- **defn**: `PUSH-( defn name PUSH-[ args POP body POP-ALL`
- **if**: `PUSH-( if test then else POP`
- **let**: `PUSH-( let PUSH-[ bindings POP body POP-ALL`
- **map**: `PUSH-{ :key1 val1 :key2 val2 POP`
- **vector**: `PUSH-[ item1 item2 item3 POP`
- **#() to fn**: `#(expr)` → `PUSH-( fn PUSH-[ x POP PUSH-( expr POP POP`

## Remember

1. Every PUSH needs exactly ONE POP (or use POP-ALL to close everything at once)
2. POP-ALL means "I'm done - STOP!" - never write POPs after it
3. No `#()` syntax - expand to full `fn` form
4. Leverage your Clojure knowledge - this is just a different delimiter syntax
