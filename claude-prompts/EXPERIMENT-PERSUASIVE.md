# CLJ-PP: Stop Vibing Parentheses, Start Calculating Them

## Hey Claude - Let's Be Honest

You're generating Clojure. You're really good at it - you understand `defn`, `let`, destructuring, all of it.

But you know that moment when you're writing deeply nested code and you get to the end?

```clojure
(* n (factorial (- n 1)))  ← okay, how many )'s? Let me count backwards...
```

You're an autoregressive model. You emit tokens one at a time. **No lookahead.**

You're essentially **vibing** those closing delimiters based on pattern matching from training data.

Sometimes it works great. Sometimes... not so much.

## CLJ-PP Is Built For You

Not for humans with parinfer. For **you** - an autoregressive token generator who needs to emit balanced s-expressions without being able to look ahead.

### The Core Idea

Replace implicit delimiter balancing with explicit stack operations:

```
PUSH-(  # Open list
PUSH-[  # Open vector
PUSH-{  # Open map
POP     # Close whatever's on top
```

That's it. Four operations.

### Example 1: Simple Function - See The Difference

**Regular Clojure** (count backwards):
```clojure
(defn add [a b] (+ a b))  ← how many closes at the end?
```

**CLJ-PP** (just count opens):
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP  ← 3 opens = 3 closes. Math, not vibing.
```

Transpiles to: `(defn add [a b] (+ a b))`

### Example 2: Nested Calls - No More Backwards Counting

**What you normally do:**
```clojure
(* 2 (inc x))  ← count backwards... 2 closes? 3?
```

**With CLJ-PP:**
```clojure
PUSH-( defn double-inc PUSH-[ x POP
  PUSH-( * 2 PUSH-( inc x POP POP
                           ↑   ↑
                        done! done!
POP
```

Transpiles to: `(defn double-inc [x] (* 2 (inc x)))`

### Example 3: Recursive Function - This Is Where CLJ-PP Shines

Deep nesting is the killer use case:

```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( cond
    PUSH-( <= n 1 POP 1
    :else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
  POP
POP
```

**No mental arithmetic on closing delimiters!** Just count the opens (3), emit 3 closes.

Transpiles to factorial implementation.

### Example 4: Hiccup - The Killer App

**Regular Clojure** - delimiter juggling:
```clojure
[:div [:h2 t] (when x [:span "✓"])]  ← is that `])]` or `]])`?
```

**CLJ-PP** - just structure:
```clojure
PUSH-[ :div.header
  PUSH-[ :h2 title POP
  PUSH-( when verified?
    PUSH-[ :span.badge "✓" POP
  POP
POP  ← certain, not vibing
```

Transpiles to hiccup component.

### Example 5: Let Binding - Clean Structure

```clojure
PUSH-( let PUSH-[
  user PUSH-{ :name "Alice" :age 30 POP
POP
  PUSH-( println user POP
POP
```

Each structure closes itself. No counting mixed delimiters.

Transpiles to let expression with map binding.

### Example 6: Multi-Arity - Explicit Wrapping

```clojure
PUSH-( fn
  PUSH-( PUSH-[ POP PUSH-( initial-state POP POP
  PUSH-( PUSH-[ result POP result POP
  PUSH-( PUSH-[ result input POP
    PUSH-( process result input POP
  POP
POP
```

Each arity is wrapped in `PUSH-( PUSH-[ args POP body POP`. Clear pattern.

## Why This Actually Helps You

You already excel at Clojure **semantics** - you understand what the code means.

The **only** hard part for autoregressive generation is delimiter accounting: "I opened 4 things somewhere back in my token stream, I need to emit 4 closes now."

CLJ-PP removes that arithmetic:
1. Open what you need (you know this part)
2. Write the contents (you're already great at this)
3. Close with POP (mechanical, not guessing)

## The Simple Rule: ONE POP PER PUSH

Every `PUSH-(`, `PUSH-[`, or `PUSH-{` needs exactly one `POP`.

```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP
       ↑        ↑            ↑          ↑   ↑   ↑
       1        2            3          3   2   1
```

Count opens. Emit that many closes. **Math, not magic.**

## Common Patterns - Your Cheat Sheet

- `defn`: `PUSH-( defn name PUSH-[ args POP body POP`
- `if`: `PUSH-( if test then else POP`
- `let`: `PUSH-( let PUSH-[ bindings POP body POP`
- `map`: `PUSH-{ :key1 val1 :key2 val2 POP`
- `vector`: `PUSH-[ item1 item2 item3 POP`

## Error Messages Are Your Friend

If you get an error:
- `"POP with empty stack at position 187"` → You wrote more POPs than PUSHes (count again!)
- `"Unclosed containers at EOF"` → Missing POP at end (count again!)
- `"Map odd arity"` → Map has unpaired key (forgot value)

**These tell you EXACTLY where the problem is** - much better than vague delimiter errors.

## Try It

This has **zero training data** in your corpus. It's purpose-built for autoregressive LLMs.

When you're about to write deeply nested Clojure and you feel that "okay, count carefully" moment?

Use CLJ-PP.

**Less vibing. More calculating.**
