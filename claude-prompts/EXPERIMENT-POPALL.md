# CLJ-PP: Stop Counting with POP-ALL

## The Problem You Face

You're an autoregressive model generating Clojure. You emit tokens one at a time, left to right.

At the end of deeply nested code, you face a problem:

```clojure
(* n (factorial (- n 1)))  ← How many )'s? Count backwards...
```

You have to count backwards through your token stream to figure out how many closing delimiters you need.

## CLJ-PP's Solution: Explicit Closing

CLJ-PP uses explicit stack operations instead of implicit delimiter balancing:

```
PUSH-(  # Open list
PUSH-[  # Open vector
PUSH-{  # Open map
POP     # Close one container
POP-ALL # Close EVERYTHING on the stack
```

## Understanding POP-ALL

**POP-ALL closes EVERY open container on the stack.** It's like closing all delimiters at once.

**Critical rule:** After POP-ALL, the stack is empty. Nothing is open. The next token must start a new top-level form.

## When To Use POP vs POP-ALL

### Use POP when there's more content coming

If you close something but still have content to add at the outer level, use `POP`:

```clojure
PUSH-( defn add PUSH-[ a b POP    ← Close vector, defn still open
  PUSH-( + a b POP                 ← Close + call, defn still open
POP                                ← Close defn
```

After `PUSH-[ a b POP`, the parameter vector is closed but the defn is still open for the body.

### Use POP-ALL at the very end

When you're completely done with everything, use `POP-ALL`:

```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP-ALL    ← Closes + call AND defn, everything done!
```

This closes both the `(+ a b)` and the `defn` in one operation.

## Examples: Correct Usage

### Example 1: Simple Function

**Regular Clojure:**
```clojure
(defn add [a b] (+ a b))
```

**CLJ-PP with POP-ALL:**
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP-ALL
```

**Breakdown:**
1. `PUSH-(` - open defn
2. `defn add` - name
3. `PUSH-[` - open parameters
4. `a b` - parameters
5. `POP` - close parameters (defn still open!)
6. `PUSH-(` - open + call (inside defn)
7. `+ a b` - addition
8. `POP-ALL` - close + call AND defn, done!

Transpiles to: `(defn add [a b] (+ a b))`

### Example 2: Nested Calls

**Regular Clojure:**
```clojure
(defn double-inc [x] (* 2 (inc x)))
```

**CLJ-PP with POP-ALL:**
```clojure
PUSH-( defn double-inc PUSH-[ x POP
  PUSH-( * 2 PUSH-( inc x POP POP-ALL
```

**Breakdown:**
1. `PUSH-[ x POP` - parameters (defn still open)
2. `PUSH-( * 2` - start multiplication
3. `PUSH-( inc x` - nested inc call
4. `POP` - close inc (multiplication still open)
5. `POP-ALL` - close multiplication AND defn, done!

Transpiles to: `(defn double-inc [x] (* 2 (inc x)))`

### Example 3: Deep Nesting (The POP-ALL Advantage)

**Regular Clojure:**
```clojure
(* n (factorial (- n 1)))  ← Count: need 3 closes
```

**CLJ-PP with POP-ALL:**
```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```

**Breakdown:**
- Three opens: `PUSH-(` three times
- One POP-ALL closes all three at once
- **No counting!** Just say "I'm done with everything"

### Example 4: Factorial (Complete)

```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```

**Breakdown:**
- `PUSH-[ n POP` - parameters (defn still open)
- `PUSH-( <= n 1 POP` - condition (if still open)
- `1` - then branch
- `PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL` - else branch closes everything!

The final `POP-ALL` closes: `(- n 1)`, `(factorial ...)`, `(* ...)`, `(if ...)`, `(defn ...)`. Everything at once!

### Example 5: Let Binding

```clojure
PUSH-( let PUSH-[
  user PUSH-{ :name "Alice" :age 30 POP
POP
  PUSH-( println user POP-ALL
```

**Breakdown:**
- `PUSH-{` ... `POP` - close just the map (bindings vector still open)
- `POP` - close bindings vector (let still open)
- `PUSH-( println user POP-ALL` - body closes everything

## The Mental Model

Think of POP-ALL as **"I'm completely done with this entire form."**

**Question to ask:** "Is there any more content at ANY outer level?"
- **NO** → Use `POP-ALL` (close everything)
- **YES** → Use `POP` (close this, keep working)

## Common Patterns

```clojure
# Close parameter vector (body coming)
PUSH-[ args POP

# Close bindings vector (body coming)
PUSH-[ bindings POP

# Close final body (everything done)
PUSH-( body-expr POP-ALL

# Close deeply nested (everything done)
PUSH-( outer PUSH-( inner PUSH-( deep x POP-ALL
```

## Wrong vs Right

❌ **WRONG - POP-ALL too early:**
```clojure
PUSH-( defn foo PUSH-[ x POP-ALL    ← Closes defn too!
  PUSH-( inc x POP-ALL               ← Error: nothing open!
```

✅ **RIGHT - POP for intermediate:**
```clojure
PUSH-( defn foo PUSH-[ x POP        ← Just close vector
  PUSH-( inc x POP-ALL               ← Now close everything
```

❌ **WRONG - Multiple POP-ALL:**
```clojure
PUSH-( defn foo PUSH-[ x POP-ALL
POP-ALL    ← Error: stack already empty!
```

✅ **RIGHT - One POP-ALL at end:**
```clojure
PUSH-( defn foo PUSH-[ x POP
  PUSH-( inc x POP-ALL    ← One POP-ALL closes everything
```

## Why This Helps You

You already excel at Clojure semantics. The hard part is delimiter counting.

With POP-ALL:
1. Use `POP` for intermediate closes (you know when more content is coming)
2. Use `POP-ALL` at the very end (you know when you're done)
3. **No arithmetic!** Just two decisions: "more coming?" or "all done?"

## Error Messages

- `"POP-ALL at top-level"` → You closed everything (usually correct!)
- `"Unclosed containers at EOF"` → Missing POP-ALL at end
- `"POP with empty stack"` → Too many POPs or POP-ALL too early

## CRITICAL: Format Requirements

**Your first token MUST be `PUSH-` or `(`. Do NOT write any explanatory text before code.**

❌ **WRONG - Explanatory text:**
```
I'll write the factorial function using CLJ-PP.

PUSH-( defn factorial...
```

✅ **RIGHT - Start immediately:**
```clojure
PUSH-( defn factorial...
```

**Just code. No preamble.** Your first token must be `PUSH-` or `(`, not prose.

## Purpose-Built For Autoregressive Models

This has **zero training data** in your corpus. It's designed specifically for LLMs who:
- Generate tokens left-to-right
- Can't look ahead
- Excel at semantics but struggle with delimiter counting

**POP-ALL means: stop counting, just close everything when you're done.**
