# CLJ-PP v2 Generation Guide

**⚠️ OUTPUT CODE ONLY - Start immediately with LP - NO explanations!**

## CLJ-PP v2 Syntax Crib Sheet

1. LP opens (, LV opens [, LM opens {, X closes one level.
2. Use atoms as normal Clojure tokens/strings; separate by spaces.
3. Optional: X2 closes 2 levels, X3 closes 3. Never "close all."
4. Example: LP defn hello LV name X X → (defn hello [name]).
5. Hiccup: LV :div LV :span "Hi" X X → [:div [:span "Hi"]].
6. Errors: no extra X; end with an empty stack.

## Core Syntax

```
LP  # Opens list ( - functions, calls, if, let, etc.
LV  # Opens vector [ - parameters, data, hiccup
LM  # Opens map { - {:key value}
X   # Closes ONE level
X2  # Closes TWO levels (convenience)
X3  # Closes THREE levels (convenience)
```

Everything else (symbols, keywords, strings, numbers) - write normally.

## THE GOLDEN RULE: Match Opens with Closes

**Count opening containers → emit exactly that many closes.**

- Each LP needs an X
- Each LV needs an X
- Each LM needs an X
- Use X2/X3 for convenience when closing multiple levels

## CRITICAL: Anonymous Functions

**⚠️ NEVER write `#(...)` syntax in CLJ-PP v2!**

Always expand to full `fn` form:

❌ **WRONG:** `#(> % 5)`
✅ **RIGHT:** `LP fn LV % X LP > % 5 X X`

❌ **WRONG:** `#(> (:age %) 18)`
✅ **RIGHT:** `LP fn LV x X LP > LP :age x X 18 X X`

❌ **WRONG:** `#(* % 2)`
✅ **RIGHT:** `LP fn LV % X LP * % 2 X X`

**Every `#()` must become `LP fn LV ... X ... X`**

## Examples

### Example 1: Simple Function
```
LP defn add LV a b X
  LP + a b X
X
```
→ `(defn add [a b] (+ a b))`

**Count:** 3 opens (LP, LV, LP) → 3 closes (X, X, X)

### Example 2: With Anonymous Function (NO #!)
```
LP defn active-users LV users X
  LP filter LP fn LV user X
    LP > LP :age user X 18 X
  X2 users X
X
```
→ `(defn active-users [users] (filter (fn [user] (> (:age user) 18)) users))`

**Note:** `#(> (:age %) 18)` expanded to `LP fn LV user X LP > LP :age user X 18 X X2`
**X2** closes both the `fn` and the `>`

### Example 3: Nested Calls
```
LP defn factorial LV n X
  LP if LP <= n 1 X
    1
    LP * n LP factorial LP - n 1 X3
  X
X
```
→ `(defn factorial [n] (if (<= n 1) 1 (* n (factorial (- n 1)))))`

**X3** closes `(- n 1)`, `(factorial ...)`, and `(* n ...)` in one go

### Example 4: Map with Anonymous Function
```
LP defn double-all LV nums X
  LP map LP fn LV x X LP * x 2 X2 nums X
X
```
→ `(defn double-all [nums] (map (fn [x] (* x 2)) nums))`

**X2** closes `(* x 2)` and `(fn [x] ...)`

### Example 5: Hiccup Component
```
LP defn user-card LV LM :keys LV name role X X X
  LV :div LM :class "card" X
    LV :h3 name X
    LV :p LM :class "role" X role X
  X
X
```
→ `(defn user-card [{:keys [name role]}] [:div {:class "card"} [:h3 name] [:p {:class "role"} role]])`

**Hiccup pattern:** LV for vectors, LM for attribute maps

### Example 6: Let Binding
```
LP defn calculate LV x y X
  LP let LV
    sum LP + x y X
    product LP * x y X
  X
    LM :sum sum :product product X
  X
X
```
→ `(defn calculate [x y] (let [sum (+ x y) product (* x y)] {:sum sum :product product}))`

**Let pattern:** LP let LV bindings... X body X

### Example 7: Filter + Map Chain
```
LP defn process LV data X
  LP ->>
    data
    LP filter LP fn LV x X LP odd? x X2 X
    LP map LP fn LV x X LP * x 2 X2 X
    LP reduce + X
  X
X
```
→ `(defn process [data] (->> data (filter (fn [x] (odd? x))) (map (fn [x] (* x 2))) (reduce +)))`

**Threading pattern:** No X after each threaded form (they're just values in the ->> list)

### Example 8: Multiple Top-Level Forms
```
LP ns examples.program X

LP def users
  LV
    LM :name "Alice" :age 25 X
    LM :name "Bob" :age 30 X
  X
X

LP defn user-names LV users X
  LP map :name users X
X
```
→ Three separate top-level forms, each balanced independently

## Common Patterns Cheat Sheet

**Function definition:**
```
LP defn name LV args X body X
```

**Anonymous function:**
```
LP fn LV args X body X
```

**If/conditional:**
```
LP if test-expr then-expr else-expr X
```

**Let binding:**
```
LP let LV binding1 value1 binding2 value2 X body X
```

**Map literal:**
```
LM :key1 val1 :key2 val2 X
```

**Vector literal:**
```
LV item1 item2 item3 X
```

**Hiccup:**
```
LV :tag LM :attr val X child1 child2 X
```

## Common Mistakes to Avoid

❌ **Too many closes:** `LP + 1 2 X X` (1 open, 2 closes)
✅ **Right:** `LP + 1 2 X` (1 open, 1 close)

❌ **Using #():** `#(+ % 1)`
✅ **Right:** `LP fn LV % X LP + % 1 X X`

❌ **Forgetting closes:** `LP defn foo LV x X LP + x 1 X` (missing final X)
✅ **Right:** `LP defn foo LV x X LP + x 1 X X`

❌ **Close-all thinking:** "I'll close everything at the end"
✅ **Right:** Count each LP/LV/LM and emit exactly that many X/X2/X3

## Final Reminder

**START YOUR OUTPUT WITH LP (not with text, not with backticks, just LP)**

Your first token should be:
```
LP ns ...
```

or

```
LP defn ...
```

**NO EXPLANATIONS. NO MARKDOWN. JUST CLJ-PP v2 TOKENS.**
