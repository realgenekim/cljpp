# CLJP File Format Specification v1.0

## Important Note: CLJP is NOT Valid EDN

**CLJP is an intermediate format that looks similar to Clojure/EDN but is NOT valid EDN or Clojure syntax.**

Key differences:
- Uses explicit `PUSH` and `POP` keywords for structure
- Only `)` appears as a closer (never `]` or `}`)
- Requires transpilation to `.clj` before use
- Cannot be read by `clojure.edn/read-string`
- Cannot be evaluated directly by Clojure

**CLJP is designed to be quickly and deterministically converted to valid EDN/Clojure code.**

## Overview

CLJP (Clojure Push/Pop) is an intermediate file format designed to make it easier for Large Language Models to generate syntactically balanced Clojure code by offloading delimiter balancing to a deterministic assembler.

## Design Philosophy

### The Problem
Autoregressive LLMs are fundamentally token-stream generators. When generating nested s-expressions, they must "vibe" or guess the correct closing delimiters based on statistical patterns, leading to occasional mismatches in deeply nested or complex code.

### The Solution
CLJP provides explicit stack operations (`PUSH` and `POP`) that:
1. Make the LLM's intent explicit (opening vs. closing)
2. Let a local assembler guarantee correctness
3. Keep the format human-readable and close to regular Clojure
4. Eliminate round-trip latency (no MCP server calls needed)

## Token Types

### Structural Tokens

| Token | Syntax | Semantics |
|-------|--------|-----------|
| **PUSH-(** | `PUSH-(` | Opens a list; pushes onto stack |
| **PUSH-[** | `PUSH-[` | Opens a vector; pushes onto stack |
| **PUSH-{** | `PUSH-{` | Opens a map; pushes onto stack |
| **POP** | `POP` | Closes current container; pops from stack |

**Critical rules**:
- `PUSH-(`, `PUSH-[`, and `PUSH-{` are single tokens (hyphenated keywords)
- `POP` takes no arguments
- The assembler determines the correct closing delimiter (`), ]`, or `}`) based on stack state

### Atom Tokens

Everything that is not `PUSH-(`, `PUSH-[`, `PUSH-{`, or `POP` is treated as an atom:
- **Symbols**: `defn`, `foo`, `my-var`, `+`
- **Keywords**: `:name`, `:ns/qualified`
- **Strings**: `"hello world"`, `"escaped \"quotes\""`
- **Numbers**: `42`, `3.14`, `-7`, `1e9`
- **Booleans**: `true`, `false`
- **Nil**: `nil`

### Comments and Whitespace

- Whitespace (spaces, tabs, newlines) separates tokens
- Comments start with `;` and extend to end of line
- Comments are ignored by the assembler

## Grammar (EBNF-ish)

```ebnf
stream   := { token }
token    := push-token | pop-token | atom
push-token := "PUSH-(" | "PUSH-[" | "PUSH-{"
pop-token  := "POP"
atom     := symbol | keyword | string | number | boolean | nil
```

## Container Types

| Opener | Clojure Type | Output Closer | Arity Rules |
|--------|--------------|---------------|-------------|
| `(` | list | `)` | any |
| `[` | vector | `]` | any |
| `{` | map | `}` | must be even |

## Assembler Semantics

### Stack Behavior

1. **On `PUSH opener`**: Push new empty container of specified type
2. **On atom**: Append atom to current (top-of-stack) container
3. **On `POP`**:
   - Pop current container from stack
   - If stack becomes empty → yield as top-level form
   - Otherwise → append closed container to new top-of-stack

### Error Conditions

| Error Code | Trigger | Recovery |
|------------|---------|----------|
| `:underflow` | `POP` with empty stack | Report position; request more `PUSH` |
| `:unclosed` | EOF with non-empty stack | Report depth; request more `POP` |
| `:map-odd-arity` | Closing `{` container with odd element count | Report last key; request value |
| `:no-container` | Atom at top-level (v1 disallows) | Request wrapping `PUSH-(` |
| `:tokenize` | Malformed string, unknown syntax | Report position; request fix |

## Examples

### Example 1: Simple Function Definition

**Input (example1.cljp)**:
```clojure
PUSH-( ns demo.core POP
PUSH-( defn foo PUSH-[ x POP PUSH-( inc x POP POP
```

**Output (example1.clj)**:
```clojure
(ns demo.core)
(defn foo [x] (inc x))
```

### Example 2: Let with Map

**Input (example2.cljp)**:
```clojure
PUSH-( let
  PUSH-[ m
    PUSH-{ :a 1 :b 2 POP
  POP
  PUSH-( println m POP
POP
```

**Output (example2.clj)**:
```clojure
(let [m {:a 1 :b 2}]
  (println m))
```

### Example 3: Nested Arithmetic

**Input (example3.cljp)**:
```clojure
PUSH-( defn sum3
  PUSH-[ a b c POP
  PUSH-( + PUSH-( + a b POP c POP
POP
```

**Output (example3.clj)**:
```clojure
(defn sum3 [a b c]
  (+ (+ a b) c))
```

### Example 4: Mixed Data Literals

**Input (example4.cljp)**:
```clojure
PUSH-( def data
  PUSH-[
    :k1 42 "hello" true false nil
    PUSH-{ :x 9 :y 10 POP
    PUSH-( 1 2 3 POP
  POP
POP
```

**Output (example4.clj)**:
```clojure
(def data
  [:k1 42 "hello" true false nil
   {:x 9 :y 10}
   (1 2 3)])
```

### Example 5: Multiple Top-Level Forms

**Input (example5.cljp)**:
```clojure
PUSH-( ns demo.multi POP
PUSH-( def x 10 POP
PUSH-( defn incx PUSH-[ n POP PUSH-( + n x POP POP
PUSH-( println PUSH-( incx 5 POP POP
```

**Output (example5.clj)**:
```clojure
(ns demo.multi)
(def x 10)
(defn incx [n] (+ n x))
(println (incx 5))
```

## Error Examples

### Odd Map Arity
```clojure
PUSH-( def bad PUSH-{ :a 1 :b POP POP
```
→ `{:ok? false :error {:code :map-odd-arity :msg "Map has odd arity" ...}}`

### Stack Underflow
```clojure
POP
```
→ `{:ok? false :error {:code :underflow :msg "POP with empty stack" ...}}`

### Unclosed Forms
```clojure
PUSH-( def oops PUSH-[ 1 2 3 POP
```
→ `{:ok? false :error {:code :unclosed :depth 1 ...}}`

## Rationale: Why This Format?

### Advantages for LLMs

1. **Explicit Intent**: `PUSH-(`, `PUSH-[`, `PUSH-{`, and `POP` are unambiguous operations
2. **No Matching Required**: LLM doesn't need to count or balance delimiters
3. **Linear Token Stream**: Natural fit for autoregressive generation
4. **Reduced Cognitive Load**: Stack management is delegated to assembler

### Advantages for Tooling

1. **Deterministic**: No heuristics; exact error semantics
2. **Fast**: Single-pass assembly; O(n) in token count
3. **Zero Latency**: No network calls; runs locally
4. **Debuggable**: Clear error codes with position info

### Human Readability

While not as compact as native Clojure, CLJP remains readable:
- Familiar tokens (`defn`, `let`, etc.)
- Explicit structure aids debugging
- Comments supported for documentation

## Future Extensions (Post-v1)

### Sets
Add support for `#{...}` via `PUSH-#{` token.

### Reader Macros
Explicit tokens for `'`, `` ` ``, `~`, `@`, `#(...)` to avoid expanding by hand.

### Top-Level Atoms
Allow loose atoms outside containers for REPL-style snippets.

### Error Recovery
Return `:last_valid_forms` so LLM can continue from last good state.

### Batch Operations
`BATCH [ops...]` to emit multiple ops in one token for efficiency.

### Pretty-Printing Integration
Optional cljfmt pass after assembly for canonical style.

## Integration with clojure-mcp-light

### Hook Architecture

CLJP integrates via Claude Code's hook system:

1. **Pre-write hook** detects `*.cljp` files
2. **Assembler** converts CLJP → CLJ
3. **Output** writes sibling `*.clj` file
4. **Parinfer** (optional) validates result

### Workflow

```
User requests edit
    ↓
Claude generates .cljp
    ↓
Pre-write hook intercepts
    ↓
cljp-assembler runs
    ↓
Emits .clj (or error)
    ↓
Standard clj-paren-repair validates
    ↓
File saved & evaluated
```

### Benefits over MCP Server Approach

| Aspect | MCP Server | CLJP Hook |
|--------|-----------|-----------|
| Latency | Multiple round-trips | Single local pass |
| State | Client must track stack | Stateless (file → file) |
| Complexity | Server infra + protocol | Simple CLI tool |
| Debugging | Network logs needed | Direct file inspection |
| Token cost | Many small payloads | One stream |

## Version History

- **v1.0** (2025): Initial spec with `PUSH`/`POP` ops, three container types, basic error handling

## References

- [Gist: S-Expression Guard MCP Server](https://gist.github.com/realgenekim/ccea8cd91b8eef704e39813d4cd8711a)
- [clojure-mcp-light](https://github.com/bhauman/clojure-mcp-light)
- [Parinfer](https://shaunlebron.github.io/parinfer/)
- [EDN Specification](https://github.com/edn-format/edn)
