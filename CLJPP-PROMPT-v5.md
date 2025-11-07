# CLJ-PP Generation Guide

**⚠️ OUTPUT CODE ONLY - Start immediately with PUSH-( - NO explanations!**

## Core Syntax

```
PUSH-(  # Opens list (functions, calls, if, let, etc.)
PUSH-[  # Opens vector (parameters, data, hiccup)
PUSH-{  # Opens map ({:key value})
POP     # Closes ONE container
```

Everything else (symbols, keywords, strings, numbers) - write normally.

## THE GOLDEN RULE: ONE POP PER PUSH

**Count opening containers → emit exactly that many POPs.**

## CRITICAL: Anonymous Functions

**⚠️ NEVER write `#(...)` syntax in CLJPP!**

Always expand to full `fn` form:

❌ **WRONG:** `#(> % 5)`  
✅ **RIGHT:** `PUSH-( fn PUSH-[ % POP PUSH-( > % 5 POP POP`

❌ **WRONG:** `#(> (:age %) 18)`  
✅ **RIGHT:** `PUSH-( fn PUSH-[ x POP PUSH-( > PUSH-( :age x POP 18 POP POP`

❌ **WRONG:** `#(* % 2)`  
✅ **RIGHT:** `PUSH-( fn PUSH-[ % POP PUSH-( * % 2 POP POP`

**Every `#()` must become `PUSH-( fn PUSH-[ ... POP ... POP`**

## Examples

### Example 1: Simple Function
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP
```
**Count:** 3 PUSHes (defn, [, +) → 3 POPs

### Example 2: With Anonymous Function (NO #!)
```clojure
PUSH-( defn active-users PUSH-[ users POP
  PUSH-( filter PUSH-( fn PUSH-[ user POP
    PUSH-( > PUSH-( :age user POP 18 POP
  POP POP users POP
POP
```
**Note:** `#(> (:age %) 18)` expanded to `PUSH-( fn PUSH-[ user POP ...`

### Example 3: Nested Calls
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
  POP
POP
```
**Line 4:** 3 PUSHes (*, factorial, -) → 3 POPs

### Example 4: Map with Anonymous Function
```clojure
PUSH-( defn double-all PUSH-[ nums POP
  PUSH-( map PUSH-( fn PUSH-[ x POP PUSH-( * x 2 POP POP nums POP
POP
```
**Note:** `#(* % 2)` expanded to `PUSH-( fn PUSH-[ x POP PUSH-( * x 2 POP POP`

### Example 5: Multiple Top-Level Forms
```clojure
PUSH-( ns examples.program POP

PUSH-( def users
  PUSH-[
    PUSH-{ :name "Alice" :age 25 POP
    PUSH-{ :name "Bob" :age 30 POP
  POP
POP

PUSH-( defn user-names PUSH-[ users POP
  PUSH-( map :name users POP
POP
```
**Each top-level form gets its own POPs**

### Example 6: Filter + Map Chain
```clojure
PUSH-( defn process PUSH-[ data POP
  PUSH-( ->>
    data
    PUSH-( filter PUSH-( fn PUSH-[ x POP PUSH-( odd? x POP POP POP
    PUSH-( map PUSH-( fn PUSH-[ x POP PUSH-( * x 2 POP POP POP
    PUSH-( reduce + POP
  POP
POP
```

## Common Errors

### Error 1: Using #() Syntax
❌ **WRONG:**
```clojure
PUSH-( filter #(> % 18) users POP
```
✅ **RIGHT:**
```clojure
PUSH-( filter PUSH-( fn PUSH-[ % POP PUSH-( > % 18 POP POP users POP
```

### Error 2: Wrong POP Count
❌ **WRONG:** 3 PUSHes but only 2 POPs
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP
```
✅ **RIGHT:** 3 PUSHes = 3 POPs
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
```

### Error 3: Writing Explanation Text
❌ **WRONG:**
```
I'll write this in CLJ-PP format...

PUSH-( ns examples POP
```
✅ **RIGHT:** Start immediately with code
```clojure
PUSH-( ns examples POP
```

## Quick Reference

- **defn:** `PUSH-( defn name PUSH-[ args POP body POP`
- **if:** `PUSH-( if test then else POP`
- **let:** `PUSH-( let PUSH-[ bindings POP body POP`
- **map:** `PUSH-{ :key1 val1 :key2 val2 POP`
- **vector:** `PUSH-[ item1 item2 item3 POP`
- **fn (not #!):** `PUSH-( fn PUSH-[ args POP body POP`

## Remember

1. **ONE POP PER PUSH** - count them!
2. **NO #() syntax** - always expand to `fn`
3. **Start immediately** with PUSH- - no explanations
4. **Count carefully** - most errors are miscounting POPs

**Start your code NOW with PUSH-(**
