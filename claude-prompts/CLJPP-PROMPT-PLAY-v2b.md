# CLJ-PP Generation Guide for Claude Code

## ⚠️ CRITICAL OUTPUT FORMAT REQUIREMENT ⚠️

**YOU MUST OUTPUT CLJ-PP FORMAT ONLY - NO EXPLANATIONS, NO MARKDOWN!**

When you generate code in response to this prompt:
- ❌ DO NOT output regular Clojure with `)` `]` `}` closing delimiters
- ❌ DO NOT output ANY explanatory text before, during, or after the code
- ❌ DO NOT wrap code in markdown fences (no ```clojure or ```)
- ❌ DO NOT use `#()` anonymous function syntax
- ✅ DO output ONLY raw CLJ-PP format using PUSH-( PUSH-[ PUSH-{ POP POP-ALL
- ✅ Start IMMEDIATELY with `PUSH-( ns` or `PUSH-( defn` - first character of first line
- ✅ End with POP or POP-ALL - no text after

**Examples of FORBIDDEN output:**
```
❌ "I'll write this in CLJ-PP format:"  (NO explanations!)
❌ ```clojure                           (NO markdown fences!)
❌ "Here's the code:"                   (NO commentary!)
```

**Correct output starts IMMEDIATELY:**
```
✅ PUSH-( ns examples.program1 POP

   PUSH-( defn foo...
```

**This is not optional. CLJ-PP format ONLY, with ZERO additional text.**

## Why CLJ-PP Exists (Motivation)

You're an autoregressive token generator trying to emit perfectly balanced s-expressions, but your output is fundamentally linear. You can't look ahead to count how many closing delimiters you'll need. You just emit tokens one at a time.

**The Delimiter Problem:** When you see this in Clojure:
```
]]])}))))]]]])
)
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

## Syntax - Two Ways to Close

```clojure
# Opening containers:
PUSH-(  # Opens list - use for functions, calls, if, let, etc.
PUSH-[  # Opens vector - use for parameters, hiccup, data
PUSH-{  # Opens map - use for {:key value}

# Closing containers - TWO OPTIONS:
POP      # Closes exactly ONE container (precise control)
POP-ALL  # Closes ALL containers in the entire stack (finish EVERYTHING!)
```

**Everything else** (symbols, keywords, strings, numbers) - just write normally.

## When to Use POP vs POP-ALL?

### Use POP when:
- You need precise control over closing one thing at a time
- You're in the middle of a form (not at the end)
- You're closing intermediate containers

### Use POP-ALL ONLY when:
- **This is the ABSOLUTE LAST line of your entire top-level form**
- You are 100% done with the defn/def/ns
- There should be NOTHING after POP-ALL (no trailing POPs!)

## THE CRITICAL RULE FOR POP-ALL

**❌ NEVER write POPs after POP-ALL:**
```clojure
:else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
  POP   ← ERROR! POP-ALL already closed everything!
POP     ← ERROR! Stack is empty!
```

**✅ POP-ALL means "I'm done - STOP!":**
```clojure
:else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
# Nothing else! POP-ALL closed: -, factorial, *, cond, defn - EVERYTHING!
```

## Pattern Recognition: When is POP-ALL Safe?

**Safe pattern** - Last line of entire function:
```clojure
PUSH-( defn simple PUSH-[ x POP
  PUSH-( * 2 x POP-ALL    ← Closes *, then defn. Done!

# Next line starts NEW top-level form:
PUSH-( defn another...
```

**Safe pattern** - Last line of last clause in cond:
```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL

# Next line starts NEW top-level form:
PUSH-( defn fibonacci...
```

**UNSAFE pattern** - Middle of function:
```clojure
PUSH-( defn foo PUSH-[ x POP
  PUSH-( let PUSH-[ y PUSH-( inc x POP-ALL  ← NO! This closes defn too!
    # Can't continue here - defn is already closed!
```

## THE GOLDEN RULE: ONE POP PER PUSH (or POP-ALL to finish)

**CRITICAL:** Every `PUSH-(`, `PUSH-[`, or `PUSH-{` needs **EXACTLY ONE** corresponding `POP` OR you use `POP-ALL` to close everything at once.

```clojure
PUSH-(           # Opens a list
  content here
POP              # Closes that list - ONE POP, no more!
```

**Common error:** Writing POPs after POP-ALL
```clojure
❌ WRONG:  PUSH-( + 1 2 POP-ALL POP    # POP-ALL already closed everything!
✅ RIGHT:  PUSH-( + 1 2 POP-ALL        # Done! No trailing POPs!
```

## Mental Model: Write Contents THEN Close

**Pattern:**
1. Open container with PUSH
2. Write ALL contents (symbols, nested PUSHes, etc.)
3. When done → ONE POP (or POP-ALL if completely finished)

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
5. **POP-ALL means STOP** - never write POPs after POP-ALL

## Examples - Study These Patterns!

### Example 1: Simple Function with POP-ALL

```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP-ALL
```

**Count it:**
- Line 1: `PUSH-(` opens defn
- Line 1: `PUSH-[` opens vector → `POP` closes it
- Line 2: `PUSH-(` opens + call
- Line 2: `POP-ALL` closes + AND closes defn → Done!

**Total: 3 PUSHes, 1 POP + 1 POP-ALL** ✅

Transpiles to: `(defn add [a b] (+ a b))`

### Example 2: Simple Function with Explicit POPs

```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP
```

**Count it:**
- Same as above, but closes + with POP, then closes defn with another POP

**Total: 3 PUSHes, 3 POPs** ✅

Both approaches work! Use POP-ALL when you're done, or explicit POPs for clarity.

### Example 3: Nested Calls with Explicit POPs

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

### Example 4: Nested Calls with POP-ALL

```clojure
PUSH-( defn double-inc PUSH-[ x POP
  PUSH-( * 2 PUSH-( inc x POP-ALL
```

**Count it:**
- Same structure, but POP-ALL at the end closes inc, *, and defn all at once

**Total: 4 PUSHes, 1 POP + 1 POP-ALL** ✅

Same result, fewer closing operations!

### Example 5: Factorial with Cond - Using Explicit POPs

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

**3 PUSHes → 3 POPs** ✅

Then line 5: `POP` closes cond
Then line 6: `POP` closes defn

### Example 6: Factorial with Cond - Using POP-ALL ✅

```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```

**This is the last line of the function, so POP-ALL closes:**
1. The `-` call
2. The `factorial` call
3. The `*` call
4. The `cond` form
5. The `defn` form

**Everything is closed! No trailing POPs needed!**

### Example 7: Fibonacci - Complete Example with POP-ALL

```clojure
PUSH-( defn fibonacci PUSH-[ n POP
  PUSH-( cond
    PUSH-( = n 0 POP 0
    PUSH-( = n 1 POP 1
    :else PUSH-( +
      PUSH-( fibonacci PUSH-( - n 1 POP POP
      PUSH-( fibonacci PUSH-( - n 2 POP-ALL
```

**Analysis:**
- Lines 1-4: Standard cond clauses with explicit POPs
- Line 5: `:else` clause opens `+` but doesn't close it yet
- Line 6: First fibonacci call - closes with `POP POP` (closes `-`, then closes `fibonacci`)
- Line 7: Second fibonacci call - uses `POP-ALL` to close EVERYTHING (closes `-`, closes second `fibonacci`, closes `+`, closes `cond`, closes `defn`)

**No trailing POPs needed!**

### Example 8: WRONG - POPs after POP-ALL ❌

```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
  POP   ← ERROR! Stack is empty - POP-ALL already closed everything!
POP     ← ERROR! Stack is empty!
```

**Why this fails:**
- POP-ALL on line 4 closed: -, factorial, *, cond, defn (everything!)
- Line 5 tries to close cond → but it's already closed!
- Line 6 tries to close defn → but it's already closed!

**The fix:** Remove the trailing POPs!

## How to Count POPs Correctly

**When you write a nested expression, ask:**
1. How many containers did I open? (count the PUSHes)
2. Write contents from innermost to outermost
3. Either:
   - POP each container in reverse order (inner first, outer last), OR
   - Use POP-ALL on the last line to close everything at once

**Example breakdown:**
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
       ↑        ↑            ↑          ↑   ↑   ↑
       |        |            |          |   |   |
    open *   open fact   open -    close - | close *
                                      close fact
```

**Same thing with POP-ALL:**
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
       ↑        ↑            ↑          ↑
       |        |            |          |
    open *   open fact   open -    close ALL (-, fact, *)
```

## Common Errors and How to Avoid Them

### Error 1: POPs After POP-ALL ❌

```clojure
❌ WRONG:
PUSH-( defn foo PUSH-[ x POP
  PUSH-( * 2 x POP-ALL
POP  ← ERROR! POP-ALL already closed defn!

✅ RIGHT:
PUSH-( defn foo PUSH-[ x POP
  PUSH-( * 2 x POP-ALL
# Done! Nothing after POP-ALL!
```

### Error 2: Too Many POPs (Without POP-ALL)

```clojure
❌ WRONG:
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP POP
# 3 PUSHes but 4 POPs = "POP with empty stack" error!

✅ RIGHT:
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
# 3 PUSHes, 3 POPs
```

**How to fix:** Count backwards: 3 PUSHes on this line → exactly 3 POPs needed.

### Error 3: Too Few POPs (Without POP-ALL)

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

### Error 4: Using POP-ALL in the Middle

```clojure
❌ WRONG:
PUSH-( defn foo PUSH-[ x POP
  PUSH-( let PUSH-[ y 1 POP-ALL  ← Closes let AND defn!
    PUSH-( println y POP         ← ERROR! defn is already closed!

✅ RIGHT:
PUSH-( defn foo PUSH-[ x POP
  PUSH-( let PUSH-[ y 1 POP      ← Just close the vector
    PUSH-( println y POP-ALL     ← Now close everything
```

## When Should You Use POP-ALL?

**Use POP-ALL when:**
1. You're on the **last line** of a top-level form (defn, def, ns)
2. You're on the **last line of the last clause** in a cond/case
3. You are **absolutely certain** there's nothing more to write in this form

**Use explicit POPs when:**
1. You're in the middle of a form
2. You want to be extra careful and explicit
3. You're not sure if you're at the end

**Both approaches work!** Choose based on your confidence level.

## Multi-Arity Functions

Each arity needs **its own wrapper**:

```clojure
PUSH-( fn
  PUSH-( PUSH-[ POP PUSH-( initial-state POP POP
  PUSH-( PUSH-[ result POP result POP
  PUSH-( PUSH-[ result input POP
    PUSH-( process result input POP-ALL
```

The last arity uses POP-ALL to close everything.

## Common Patterns - Your Cheat Sheet

**defn with explicit POPs**:
```clojure
PUSH-( defn name PUSH-[ args POP body POP
```

**defn with POP-ALL**:
```clojure
PUSH-( defn name PUSH-[ args POP body POP-ALL
```

**if**: `PUSH-( if test then else POP`

**let**: `PUSH-( let PUSH-[ bindings POP body POP`

**map**: `PUSH-{ :key1 val1 :key2 val2 POP`

**vector**: `PUSH-[ item1 item2 item3 POP`

**for**: `PUSH-( for PUSH-[ item items POP body POP`

**destructuring**: `PUSH-{ :keys PUSH-[ name age POP :as user POP`

## Error Messages Are Your Friend

If you get an error:
- `"POP with empty stack at position 187"` → You wrote more POPs than PUSHes (or used POPs after POP-ALL!)
- `"Unclosed containers at EOF"` → Missing POP at end (or should have used POP-ALL)
- `"Map odd arity"` → Map has unpaired key (forgot value)

**These tell you EXACTLY where the problem is** - much better than Clojure's vague delimiter errors.

## Self-Check Before Generating

Before writing CLJ-PP code:

1. **Can you count?** Every PUSH needs exactly ONE POP (or use POP-ALL to finish)
2. **Can you nest?** Inner calls POP before outer calls
3. **Can you stop?** If you use POP-ALL, STOP - no more POPs!

If you're counting delimiters in your head, **you're doing CLJ-PP wrong**. The whole point is to make it mechanical:
- Open container → PUSH
- Write contents
- Done with this container → POP
- Done with EVERYTHING → POP-ALL (then STOP!)

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

# Same thing with POP-ALL
PUSH-( a PUSH-( b PUSH-( c x POP-ALL
          ↑        ↑        ↑   ↑
       open a   open b   open c closes ALL!

# Vector in function
PUSH-( foo PUSH-[ 1 2 3 POP POP

# Map in let
PUSH-( let PUSH-[ x PUSH-{ :a 1 POP POP body POP
```

## Remember: You're Great at Clojure Already!

You have excellent Clojure training. CLJ-PP isn't fixing broken generation - it's making already-good generation **easier and safer**, especially for:

- Editing deep nesting
- Hiccup components
- Complex destructuring
- Times when you want to be **certain** rather than **careful**

Use it as a **power tool** for hard problems, not a crutch for everything.

## Final Reminders

1. **ONE POP PER PUSH** (or POP-ALL to close everything)
2. **POP-ALL means STOP** - never write POPs after it
3. **POP-ALL is for the END** - last line of a form only
4. **When in doubt, use explicit POPs** - they're always safe

If you write 3 PUSHes on a line, you need exactly 3 POPs (or POP-ALL to close everything).
If you use POP-ALL, you are DONE - do not write any more POPs!

Count them. Every time. And remember: POP-ALL = "I'm completely finished!"
