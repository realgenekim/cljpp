# CLJ-PP Generation Guide for Claude Code

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

## Critical Rules

1. **NEVER write closing delimiters** - no `]` `}` `)` except in strings
2. **POP has no arguments** - just write `POP` when done with current level
3. **Use hyphens** - `PUSH-(` not `PUSH (` (it's a keyword)
4. **Don't count** - let the assembler track depth

## Examples

### Simple Function
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP
```
→ `(defn add [a b] (+ a b))`

### Hiccup with Conditionals
```clojure
PUSH-[ :div.header
  PUSH-[ :h2 title POP
  PUSH-( when verified?
    PUSH-[ :span.badge "✓" POP
  POP
POP
```
→ `[:div.header [:h2 title] (when verified? [:span.badge "✓"])]`

### Let with Map
```clojure
PUSH-( let PUSH-[
  user PUSH-{ :name "Alice" :age 30 POP
POP
  PUSH-( println user POP
POP
```
→ `(let [user {:name "Alice" :age 30}] (println user))`

## Mental Model: Stack Operations, Not Delimiter Matching

**Think**:
1. Opening something? → `PUSH-(`/`PUSH-[`/`PUSH-{`
2. Write content (symbols, keywords, etc.)
3. Done with this level? → `POP`

**Don't think**:
- ❌ "Do I need 3 or 4 closing parens here?"
- ❌ "Is this a ] or a } or a )?"
- ❌ "Let me count backwards to match delimiters"

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

## Common Patterns

**defn**: `PUSH-( defn name PUSH-[ args POP body POP`

**if**: `PUSH-( if test then else POP`

**let**: `PUSH-( let PUSH-[ bindings POP body POP`

**map**: `PUSH-{ :key1 val1 :key2 val2 POP`

**vector**: `PUSH-[ item1 item2 item3 POP`

**for**: `PUSH-( for PUSH-[ item items POP body POP`

**destructuring**: `PUSH-{ :keys PUSH-[ name age POP :as user POP`

## Error Messages Are Your Friend

If you get an error:
- `"POP with empty stack at position 187"` → You POP'd without PUSH
- `"Unclosed containers at EOF"` → Missing POP at end
- `"Map odd arity"` → Map has unpaired key (forgot value)

**These tell you EXACTLY where the problem is** - much better than Clojure's vague delimiter errors.

## Workflow

1. Generate `.cljpp` file
2. Transpiler auto-runs → creates `.clj` file
3. Check `.clj` output for correctness
4. If error, fix in `.cljpp` source

## The Value Proposition

**Not**: "Regular Clojure is broken" (it works pretty well - 95% success)

**But**: "CLJ-PP reduces cognitive load"
- Zero mental counting
- Safer for editing nested code
- Precise error messages
- Works without pattern-matching training data

For hiccup, it's **transformative**. For simple code, it's **optional**.

## Quick Self-Check

✅ **Good**: You write PUSH/POP mechanically without counting
❌ **Bad**: You're counting delimiters (defeats the purpose!)

✅ **Good**: Complex nesting feels easy
❌ **Bad**: Simple code takes longer than regular Clojure (use .clj instead!)

## Remember

**You have excellent Clojure training** (95% correctness in testing). CLJ-PP isn't fixing broken generation - it's making already-good generation **easier and safer**, especially for:

- Editing deep nesting
- Hiccup components
- Complex destructuring
- Times when you want to be **certain** rather than **careful**

Use it as a **power tool** for hard problems, not a crutch for everything.
