# CLJ-PP Generation Guide for Claude Code

## Why CLJ-PP Exists (Motivation)

You're an autoregressive token generator trying to emit perfectly balanced s-expressions, but your output is fundamentally linear. You can't look ahead to count how many closing delimiters you'll need. You just emit tokens one at a time.

**The Delimiter Problem:** When you see this in Clojure:
```
]]])}))))]]]]))
```

What do you actually do? In deeply nested hiccup forms with `let` and multiple `if` statements, it's nearly impossible to count backwards and figure out where the error is.

**CLJ-PP's Solution:** Replace delimiter matching with explicit stack operations.

Instead of:
```clojure
(defn factorial [n]
  (if (<= n 1)
    1
    (* n (factorial (- n 1)))))
```

Write:
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
  POP
POP
```

**Key insight:** You already understand Clojure semantics perfectly. CLJ-PP just makes the bracket balancing **calculable instead of guessable**.

## When to Use CLJ-PP (.cljpp files)

**ALWAYS use CLJ-PP for:**
- Hiccup/Reagent components (mixing vectors, functions, maps)
- Complex destructuring in let bindings
- Deeply nested code (>3 levels)
- State machines with nested conditionals
- Parser combinators or recursive algorithms
- core.async pipelines

**Use regular Clojure for:**
- Simple functions (1-2 levels deep)
- REPL one-liners
- Reading existing code

## Syntax - The Only 4 Things You Need

```clojure
PUSH-(  # Opens list - use for functions, calls, if, let, etc.
PUSH-[  # Opens vector - use for parameters, hiccup, data
PUSH-{  # Opens map - use for {:key value}
POP     # Closes current container (assembler picks ), ], or })
```

**Everything else** (symbols, keywords, strings, numbers) - just write normally.

## THE GOLDEN RULE: ONE POP PER PUSH

**CRITICAL:** Every `PUSH-(`, `PUSH-[`, or `PUSH-{` needs **EXACTLY ONE** corresponding `POP`.

```clojure
PUSH-(           # Opens a list
  content here
POP              # Closes that list - ONE POP, no more!
```

**Common error:** Writing too many POPs
```clojure
❌ WRONG:  PUSH-( + 1 2 POP POP    # 1 PUSH but 2 POPs = ERROR!
✅ RIGHT:  PUSH-( + 1 2 POP        # 1 PUSH, 1 POP
```

## Mental Model: Write Contents THEN Pop

**Pattern:**
1. Open container with PUSH
2. Write ALL contents (symbols, nested PUSHes, etc.)
3. When done → ONE POP

**Example:**
```clojure
PUSH-( + 1 2 POP
       ↑     ↑
     open   close (after writing all contents: + 1 2)
```

## Critical Rules

1. **NEVER write closing delimiters** - no `]` `}` `)` except in strings
2. **POP has no arguments** - just write `POP` when done with current level
3. **Use hyphens** - `PUSH-(` not `PUSH (` (it's a keyword)
4. **ONE POP PER PUSH** - count your PUSHes, count your POPs, they must match

## Examples - Study These Patterns!

### Example 1: Simple Function (Basic Counting)
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP
```

**Count it:**
- Line 1: `PUSH-(` opens defn → needs 1 POP (at end)
- Line 1: `PUSH-[` opens vector → needs 1 POP (immediately: `POP`)
- Line 2: `PUSH-(` opens + call → needs 1 POP (end of line: `POP`)
- Line 3: Close the defn → `POP`

**Total: 3 PUSHes, 3 POPs** ✅

Transpiles to: `(defn add [a b] (+ a b))`

### Example 2: Nested Calls (Inner Before Outer)
```clojure
PUSH-( defn double-inc PUSH-[ x POP
  PUSH-( * 2 PUSH-( inc x POP POP
POP
```

**Count it:**
- `PUSH-( defn` → will need 1 POP at very end
- `PUSH-[ x` → needs 1 POP immediately
- `PUSH-( *` → will need 1 POP (after the inc call)
- `PUSH-( inc` → needs 1 POP immediately (closes inc)
- Then POP closes the `*` call
- Then POP closes the defn

**Total: 4 PUSHes, 4 POPs** ✅

Transpiles to: `(defn double-inc [x] (* 2 (inc x)))`

**Key pattern:** Inner POP before outer POP
```
PUSH-( * ... PUSH-( inc x POP POP
                          ↑   ↑
                       inner outer
```

### Example 3: Factorial with Cond (Real World)
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
  POP
POP
```

**Count the tricky line:**
```clojure
:else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
```
- `PUSH-( *` → opens multiplication
- `PUSH-( factorial` → opens recursive call
- `PUSH-( -` → opens subtraction
- `POP` → closes `-` call (1st POP)
- `POP` → closes `factorial` call (2nd POP)
- `POP` → closes `*` call (3rd POP)

**3 PUSHes → 3 POPs** ✅ (NOT 4!)

### Example 4: Hiccup with Conditionals
```clojure
PUSH-[ :div.header
  PUSH-[ :h2 title POP
  PUSH-( when verified?
    PUSH-[ :span.badge "✓" POP
  POP
POP
```

**Count it:**
- `PUSH-[` :div → needs 1 POP at end
- `PUSH-[` :h2 → needs 1 POP immediately
- `PUSH-(` when → needs 1 POP (after the span)
- `PUSH-[` :span → needs 1 POP immediately
- Then POP closes when
- Then POP closes div

**Total: 4 PUSHes, 4 POPs** ✅

### Example 5: Let with Map
```clojure
PUSH-( let PUSH-[
  user PUSH-{ :name "Alice" :age 30 POP
POP
  PUSH-( println user POP
POP
```

**Count it:**
- `PUSH-( let` → needs 1 POP at very end
- `PUSH-[` bindings → needs 1 POP after map
- `PUSH-{` map → needs 1 POP immediately
- `PUSH-( println` → needs 1 POP immediately
- Close let

**Total: 4 PUSHes, 4 POPs** ✅

## How to Count POPs Correctly

**When you write a nested expression, ask:**
1. How many containers did I open? (count the PUSHes)
2. Write contents from innermost to outermost
3. POP each container in reverse order (inner first, outer last)

**Example breakdown:**
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
       ↑        ↑            ↑          ↑   ↑   ↑
       |        |            |          |   |   |
    open *   open fact   open -    close - | close *
                                      close fact
```

## Common Errors and How to Avoid Them

### Error 1: Too Many POPs
```clojure
❌ WRONG:
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP POP
# 3 PUSHes but 4 POPs = "POP with empty stack" error!

✅ RIGHT:
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
# 3 PUSHes, 3 POPs
```

**How to fix:** Count backwards: 3 PUSHes on this line → exactly 3 POPs needed.

### Error 2: Too Few POPs
```clojure
❌ WRONG:
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
# Missing final POP to close defn = "Unclosed containers" error!

✅ RIGHT:
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP  # Close the defn!
```

### Error 3: Wrong Container Type
This is actually **impossible** in CLJ-PP! The assembler figures out whether to emit `]`, `)`, or `}` based on context. You just write `POP`.

## Multi-Arity Functions

Each arity needs **its own wrapper**:

```clojure
PUSH-( fn
  PUSH-( PUSH-[ POP PUSH-( initial-state POP POP
  PUSH-( PUSH-[ result POP result POP
  PUSH-( PUSH-[ result input POP
    PUSH-( process result input POP
  POP
POP
```

Each arity body is wrapped in `PUSH-( PUSH-[ args POP body POP`.

## Common Patterns - Your Cheat Sheet

**defn**: `PUSH-( defn name PUSH-[ args POP body POP`

**if**: `PUSH-( if test then else POP`

**let**: `PUSH-( let PUSH-[ bindings POP body POP`

**map**: `PUSH-{ :key1 val1 :key2 val2 POP`

**vector**: `PUSH-[ item1 item2 item3 POP`

**for**: `PUSH-( for PUSH-[ item items POP body POP`

**destructuring**: `PUSH-{ :keys PUSH-[ name age POP :as user POP`

## Error Messages Are Your Friend

If you get an error:
- `"POP with empty stack at position 187"` → You wrote more POPs than PUSHes (count again!)
- `"Unclosed containers at EOF"` → Missing POP at end (count again!)
- `"Map odd arity"` → Map has unpaired key (forgot value)

**These tell you EXACTLY where the problem is** - much better than Clojure's vague delimiter errors.

## Self-Check Before Generating

Before writing CLJ-PP code:

1. **Can you count?** Every PUSH needs exactly ONE POP
2. **Can you nest?** Inner calls POP before outer calls
3. **Can you be mechanical?** Don't guess - calculate

If you're counting delimiters in your head, **you're doing CLJ-PP wrong**. The whole point is to make it mechanical:
- Open container → PUSH
- Write contents
- Done → POP (once!)

## Quick Reference Card

```clojure
# Simple call
PUSH-( fn arg1 arg2 POP

# Nested call (2 levels)
PUSH-( outer PUSH-( inner x POP POP

# Three levels deep
PUSH-( a PUSH-( b PUSH-( c x POP POP POP
          ↑        ↑        ↑   ↑   ↑   ↑
       open a   open b   open c | | |
                              c↗  ↑ ↑
                                b↗ ↑
                                  a↗

# Vector in function
PUSH-( foo PUSH-[ 1 2 3 POP POP

# Map in let
PUSH-( let PUSH-[ x PUSH-{ :a 1 POP POP body POP
```

## Remember: You're Great at Clojure Already!

You have excellent Clojure training (95% correctness in testing). CLJ-PP isn't fixing broken generation - it's making already-good generation **easier and safer**, especially for:

- Editing deep nesting
- Hiccup components
- Complex destructuring
- Times when you want to be **certain** rather than **careful**

Use it as a **power tool** for hard problems, not a crutch for everything.

## Final Reminder: THE GOLDEN RULE

**ONE POP PER PUSH. NO MORE. NO LESS.**

If you write 3 PUSHes on a line, you need exactly 3 POPs.
If you write 5 PUSHes in a function, you need exactly 5 POPs.

Count them. Every time.
