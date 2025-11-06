# Claude Code Instructions for CLJP Files

## What is CLJP?

CLJP (Clojure Push/Pop) is an intermediate file format that makes it easier for you (Claude) to generate syntactically balanced Clojure code. Instead of worrying about matching closing delimiters, you emit explicit `PUSH` and `POP` operations.

## When to Use CLJP

**Use CLJP for**:
- Deeply nested code (3+ levels of nesting)
- Complex macro definitions
- Data structures with mixed delimiter types
- Any code where you're uncertain about delimiter balancing

**Don't use CLJP for**:
- Simple function definitions (1-2 levels deep)
- Straightforward data literals
- Code you're confident about

## CLJP Syntax Quick Reference

### Structural Tokens

```clojure
PUSH (    ; Opens a list
PUSH [    ; Opens a vector
PUSH {    ; Opens a map
POP       ; Closes the current container (auto-typed)
```

### Everything Else is an Atom

No `ATOM` keyword needed! Symbols, keywords, strings, numbers, booleans, and nil are just written normally.

### Critical Rules

1. **POP takes no arguments** - the assembler determines the correct closer
2. **Only `)` appears in CLJP** - never write `]` or `}`
3. **All atoms must be inside containers** (v1 restriction)
4. **Whitespace separates tokens** - newlines and indentation are optional but recommended for readability
5. **Comments start with `;`** - use them liberally!

## Examples

### Simple Function
```clojure
PUSH ( defn foo PUSH [ x POP
  PUSH ( inc x POP
POP
```
Compiles to: `(defn foo [x] (inc x))`

### Let with Map
```clojure
PUSH ( let PUSH [ m PUSH { :a 1 :b 2 POP POP
  PUSH ( println m POP
POP
```
Compiles to: `(let [m {:a 1 :b 2}] (println m))`

### Nested Arithmetic
```clojure
PUSH ( defn sum3 PUSH [ a b c POP
  PUSH ( + PUSH ( + a b POP c POP
POP
```
Compiles to: `(defn sum3 [a b c] (+ (+ a b) c))`

## Generation Strategy

### Mental Model

Think of CLJP generation as **mechanical stack operations**, not semantic understanding:

1. **Opening something?** → Emit `PUSH (` or `PUSH [` or `PUSH {`
2. **Adding content?** → Emit the atoms (symbols, keywords, etc.)
3. **Done with this level?** → Emit `POP`
4. **Never count delimiters** → the assembler guarantees correctness

### Step-by-Step Process

**Example task**: "Write a function that filters even numbers"

**Your thought process**:
```
1. "Need to define a function"
   → PUSH ( defn filter-evens

2. "Functions need parameters"
   → PUSH [ lst POP

3. "Body calls filter"
   → PUSH ( filter even? lst POP

4. "Done with function"
   → POP
```

**Output**:
```clojure
PUSH ( defn filter-evens PUSH [ lst POP
  PUSH ( filter even? lst POP
POP
```

### Common Patterns

**Function definition**:
```clojure
PUSH ( defn <name> PUSH [ <args> POP
  <body>
POP
```

**Let binding**:
```clojure
PUSH ( let PUSH [ <bindings> POP
  <body>
POP
```

**If expression**:
```clojure
PUSH ( if <test>
  <then-branch>
  <else-branch>
POP
```

**Map literal**:
```clojure
PUSH { :key1 val1 :key2 val2 POP
```

## Error Recovery

### Common Errors

1. **Underflow**: `POP` with empty stack
   - **Fix**: Add a `PUSH` before the orphan `POP`

2. **Unclosed**: EOF with non-empty stack
   - **Fix**: Add more `POP` tokens to close remaining forms

3. **Map odd arity**: Closing a map with odd element count
   - **Fix**: Add the missing value or remove the extra key

### Reading Error Messages

```clojure
CLJP error: Map odd arity
At: line 12, column 34
Context: "PUSH { :a 1 :b POP"
Stack depth: 3
Last valid form: (defn foo [x])
```

**How to fix**:
1. Look at line 12: `PUSH { :a 1 :b POP`
2. Count key-value pairs: `:a 1` (OK), `:b` (missing value!)
3. Add the missing value: `PUSH { :a 1 :b 2 POP`

## Integration with clojure-mcp

### Workflow

When you save a `.cljp` file:
1. Pre-write hook intercepts
2. CLJP assembler runs automatically
3. `.clj` file is generated
4. You (Claude) see the compiled `.clj` for validation

### File Management

- **Source of truth**: `.clj` files (human-readable)
- **Intermediate format**: `.cljp` files (machine-friendly)
- **Don't edit both**: Only generate `.cljp`, let tooling produce `.clj`

### Context Window Strategy

**During reading/comprehension**:
- Focus on `.clj` files (they're more readable)
- Ignore `.cljp` files (too verbose)

**During generation/editing**:
- Generate `.cljp` directly
- Check compiled `.clj` output for correctness
- Fix errors in `.cljp` source

## Readability Notes

### You Won't Read CLJP Often

CLJP is **not meant to be read** regularly. It's an intermediate format optimized for generation, not comprehension.

**When you might read CLJP**:
- Debugging transpiler errors (rare)
- Understanding what you just generated (uncommon)
- Fixing a specific error at a line/column (occasional)

**What to do instead**:
- Always read the compiled `.clj` first
- Use `.clj` to understand semantics
- Use `.cljp` only to see the operations you emitted

### CLJP is Like Assembly Language

- **Readable in principle** (you can parse it)
- **Not fluent** (requires mental stack simulation)
- **Best for machines** (deterministic, unambiguous)
- **Humans prefer high-level** (`.clj` is the "source code")

## Self-Assessment: Am I Using CLJP Correctly?

**Good signs**:
✅ You emit `PUSH`/`POP` mechanically without counting
✅ You feel confident about balanced delimiters
✅ Complex nesting is easier than plain Clojure
✅ Errors are caught immediately by assembler

**Warning signs**:
⚠️ You're counting parentheses in CLJP (defeats the purpose!)
⚠️ Simple code takes longer than plain Clojure
⚠️ You forget `PUSH` keywords frequently
⚠️ You write `]` or `}` in CLJP (syntax error!)

## Troubleshooting

### "I keep forgetting to write PUSH"

**Symptom**: You write `( defn foo ...` instead of `PUSH ( defn foo ...`

**Solution**: Think "I'm **pushing** a new list onto the stack" not "I'm opening a paren"

### "I'm confused about what POP closes"

**Symptom**: You write `POP` but aren't sure if it's closing a list, vector, or map

**Solution**: **Don't worry about it!** The assembler knows. Just emit `POP` when you're done with the current level.

### "CLJP feels more verbose than plain Clojure"

**Answer**: Yes, it is ~50% more tokens. But the benefit is **guaranteed correctness** and **reduced mental load** for complex code.

### "Should I use CLJP for everything?"

**Answer**: No! Use it selectively for complex/nested code. For simple stuff, plain Clojure is faster.

## Advanced: When CLJP Shines

### Example: Deeply Nested Macro

**Plain Clojure** (error-prone):
```clojure
(defmacro complex [& body]
  `(let [~'x (fn []
               ~@(for [form body]
                   `(when (pred? ~form)
                      (let [~'y ~form]
                        (process ~'y)))))]
     (~'x)))
```

Did I count right? Are all delimiters balanced? Hard to say without running it.

**CLJP** (confident):
```clojure
PUSH ( defmacro complex PUSH [ & body POP
  PUSH ` PUSH ( let PUSH [ PUSH ~' x
    PUSH ( fn PUSH [ POP
      PUSH ~@ PUSH ( for PUSH [ form body POP
        PUSH ` PUSH ( when PUSH ( pred? PUSH ~ form POP POP
          PUSH ( let PUSH [ PUSH ~' y PUSH ~ form POP POP
            PUSH ( process PUSH ~' y POP
          POP
        POP POP
      POP POP
    POP POP POP
    PUSH ( PUSH ~' x POP
  POP POP
POP
```

Every `PUSH` has a matching `POP`. The assembler guarantees correctness.

## Final Thoughts

**CLJP is a tool, not a replacement** for plain Clojure. Use it when it helps (complex nesting), skip it when it doesn't (simple code).

**The goal**: Write code faster and more confidently, not to make everything verbose.

**Your advantage**: You can emit tokens without "understanding" semantics—just follow the mechanical rules and let the assembler handle correctness.

---

**Remember**: You're generating a **stream of operations**, not writing code for humans to read. Stay mechanical, trust the assembler, and you'll generate perfect Clojure every time.
