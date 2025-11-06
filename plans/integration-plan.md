# CLJP Integration Plan: clojure-mcp-light Fork & Merge

## Executive Summary

This plan outlines how to integrate the CLJP tokenizer/assembler into Bruce Hauman's `clojure-mcp-light` as an experimental hook, test it in real-world usage with Claude Code, and potentially contribute it back upstream.

## Goals

1. **Minimal disruption**: Work alongside existing parinfer hooks
2. **Opt-in**: Users can enable/disable CLJP via config
3. **Real-world validation**: Test with actual Claude Code workflows
4. **Upstream contribution**: If successful, submit PR to upstream

## Phase 1: Fork and Setup

### 1.1 Fork Repository

```bash
# Fork on GitHub UI
gh repo fork bhauman/clojure-mcp-light --clone

cd clojure-mcp-light
git remote add upstream https://github.com/bhauman/clojure-mcp-light.git

# Create feature branch
git checkout -b feature/cljp-support
```

### 1.2 Repository Structure (Before)

```
clojure-mcp-light/
├── README.md
├── settings_example
├── hooks/
│   └── clj-paren-repair-claude-hook
├── tools/
│   └── clj-nrepl-eval
└── docs/
    └── (usage examples)
```

### 1.3 Repository Structure (After Integration)

```
clojure-mcp-light/
├── README.md
├── settings_example                  # Updated with CLJP config
├── hooks/
│   ├── clj-paren-repair-claude-hook
│   └── cljp-transpile-hook          # NEW: CLJP → CLJ converter
├── tools/
│   ├── clj-nrepl-eval
│   └── cljp-assemble                # NEW: CLI assembler
├── src/
│   └── cljp/
│       ├── core.clj                 # NEW: Tokenizer + assembler
│       ├── hooks.clj                # NEW: Hook integration
│       └── examples.clj             # NEW: Test cases
├── test/
│   └── cljp/
│       ├── tokenizer_test.clj       # NEW: Unit tests
│       └── assembler_test.clj       # NEW: Integration tests
├── docs/
│   ├── CLJP.md                      # NEW: User-facing guide
│   └── CLJP-INTERNALS.md            # NEW: Developer docs
└── bb.edn                           # Updated with CLJP tasks
```

## Phase 2: Implementation

### 2.1 Core Assembler (src/cljp/core.clj)

**Dependencies**: Pure Clojure stdlib (no external deps)

**Components**:
1. **Tokenizer**: String → vector of `[:open "("]`, `[:pop]`, `[:atom "defn"]`
2. **Assembler**: Token stream → `{:ok? true :forms [...]}` or `{:ok? false :error {...}}`
3. **Pretty-printer**: AST → formatted `.clj` string

**Key Functions**:
```clojure
(ns cljp.core
  (:require [clojure.string :as str]))

(defn tokenize [^String source])
  ;; => [[:push "("] [:atom "defn"] [:atom "foo"] ...]

(defn assemble [tokens])
  ;; => {:ok? true :forms [...] :clj "(defn foo ...)"}
  ;;    or {:ok? false :error {:code :underflow ...}}

(defn transpile [cljp-source])
  ;; => {:ok? true :clj "..."} or {:ok? false :error {...}}
```

**Error Handling**:
```clojure
;; Error schema (compatible with clojure-mcp-light's error reporting)
{:ok? false
 :error {:code :underflow | :unclosed | :map-odd-arity | :tokenize
         :msg "Human-readable description"
         :line 42
         :column 10
         :context {:stack-depth 3
                   :last-valid-form "(defn foo [x]"}}}
```

### 2.2 Hook Integration (hooks/cljp-transpile-hook)

**Trigger**: Pre-write/pre-edit on `*.cljp` files

**Behavior**:
1. Detect `*.cljp` extension
2. Read file content
3. Run `cljp.core/transpile`
4. If success:
   - Write sibling `*.clj` file
   - Return `.clj` path (so Claude edits that instead)
   - Optionally show notification: "Transpiled foo.cljp → foo.clj"
5. If failure:
   - Write error to stderr (Claude sees it)
   - Return `:abort` (blocks file write)
   - Show error message with position info

**Script** (Bash wrapper around Babashka):
```bash
#!/usr/bin/env bash
# hooks/cljp-transpile-hook

set -euo pipefail

FILE_PATH="$1"  # Passed by Claude Code

# Only process .cljp files
if [[ ! "$FILE_PATH" =~ \.cljp$ ]]; then
  echo "$FILE_PATH"  # Pass through non-CLJP files
  exit 0
fi

# Run assembler
OUTPUT=$(bb -m cljp.core/transpile-file "$FILE_PATH" 2>&1) || {
  echo "CLJP transpile error:" >&2
  echo "$OUTPUT" >&2
  exit 1
}

# Return path to generated .clj file
CLJ_PATH="${FILE_PATH%.cljp}.clj"
echo "$CLJ_PATH"
```

**Babashka Integration** (bb.edn):
```clojure
{:paths ["src"]
 :deps {}
 :tasks
 {cljp/transpile
  {:doc "Transpile .cljp → .clj"
   :requires ([cljp.core :as cljp])
   :task (let [file (first *command-line-args*)]
           (cljp/transpile-file file))}}}
```

### 2.3 Claude Code Settings (settings_example)

Add CLJP hook configuration:

```json
{
  "hooks": {
    "pre_write_file": [
      {
        "id": "cljp-transpile",
        "enabled": false,           // Opt-in initially
        "command": "hooks/cljp-transpile-hook",
        "args": ["{{file_path}}"],
        "scopes": ["local", "project"],
        "description": "Transpile .cljp files to .clj using CLJP assembler"
      },
      {
        "id": "clj-paren-repair",  // Existing hook
        "enabled": true,
        "command": "hooks/clj-paren-repair-claude-hook",
        // ... existing config
      }
    ]
  },
  "cljp": {
    "mode": "opt-in",              // "opt-in" | "always" | "never"
    "preserve_source": true,       // Keep .cljp files alongside .clj
    "notify_on_transpile": true,   // Show notification on success
    "validate_with_parinfer": true // Run parinfer on output .clj
  }
}
```

### 2.4 User-Facing Documentation (docs/CLJP.md)

**Sections**:
1. What is CLJP?
2. Why use CLJP?
3. Quick start
4. Configuration
5. Examples (simple → complex)
6. Troubleshooting
7. When to use (and when not to)

**Example Quick Start**:
```markdown
## Quick Start

1. **Enable the hook** in your Claude Code settings:
   ```json
   {"hooks": {"pre_write_file": [{"id": "cljp-transpile", "enabled": true}]}}
   ```

2. **Tell Claude to write CLJP**:
   > "Create a new file foo.cljp with a simple function"

3. **Claude generates**:
   ```clojure
   PUSH ( defn hello PUSH [ name POP
     PUSH ( println "Hello," name POP
   POP
   ```

4. **Hook auto-transpiles** to `foo.clj`:
   ```clojure
   (defn hello [name]
     (println "Hello," name))
   ```

5. **Evaluate as normal**: `(hello "World")` → `"Hello, World"`
```

## Phase 3: Testing Strategy

### 3.1 Unit Tests (test/cljp/)

**Tokenizer Tests** (test/cljp/tokenizer_test.clj):
```clojure
(deftest tokenize-simple-list
  (is (= [[:push "("] [:atom "defn"] [:atom "foo"] [:pop]]
         (tokenize "PUSH ( defn foo POP"))))

(deftest tokenize-nested
  (is (= [[:push "("] [:push "["] [:atom "x"] [:pop] [:pop]]
         (tokenize "PUSH ( PUSH [ x POP POP"))))

(deftest tokenize-string-literals
  (is (= [[:atom "\"hello world\""]]
         (tokenize "\"hello world\""))))

(deftest tokenize-reject-bare-closers
  (is (thrown? Exception (tokenize "]"))))
```

**Assembler Tests** (test/cljp/assembler_test.clj):
```clojure
(deftest assemble-simple-fn
  (let [result (assemble [[:push "("] [:atom "defn"] [:atom "foo"] [:pop]])]
    (is (:ok? result))
    (is (= "(defn foo)" (:clj result)))))

(deftest assemble-nested-vec
  (let [result (assemble [[:push "("] [:push "["] [:atom "x"] [:pop] [:pop]])]
    (is (:ok? result))
    (is (= "([x])" (:clj result)))))

(deftest assemble-map-even-arity
  (let [result (assemble [[:push "{"] [:atom ":a"] [:atom "1"] [:pop]])]
    (is (:ok? result))))

(deftest assemble-map-odd-arity-error
  (let [result (assemble [[:push "{"] [:atom ":a"] [:pop]])]
    (is (not (:ok? result)))
    (is (= :map-odd-arity (get-in result [:error :code])))))

(deftest assemble-underflow-error
  (let [result (assemble [[:pop]])]
    (is (not (:ok? result)))
    (is (= :underflow (get-in result [:error :code])))))

(deftest assemble-unclosed-error
  (let [result (assemble [[:push "("] [:atom "defn"]])]
    (is (not (:ok? result)))
    (is (= :unclosed (get-in result [:error :code])))))
```

### 3.2 Integration Tests (Golden Files)

**Structure**:
```
test/cljp/golden/
├── simple-fn.cljp             # Input
├── simple-fn.clj              # Expected output
├── nested-let.cljp
├── nested-let.clj
├── map-literals.cljp
├── map-literals.clj
├── error-underflow.cljp
├── error-underflow.err        # Expected error
└── ...
```

**Test Runner**:
```clojure
(deftest golden-file-tests
  (doseq [cljp-file (file-seq (io/file "test/cljp/golden"))]
    (when (.endsWith (.getName cljp-file) ".cljp")
      (let [expected-clj (io/file (str/replace (.getPath cljp-file) ".cljp" ".clj"))
            expected-err (io/file (str/replace (.getPath cljp-file) ".cljp" ".err"))
            result (transpile (slurp cljp-file))]
        (cond
          (.exists expected-clj)
          (is (= (slurp expected-clj) (:clj result)))

          (.exists expected-err)
          (is (not (:ok? result)))

          :else
          (throw (ex-info "No golden file" {:cljp-file cljp-file})))))))
```

### 3.3 Real-World Test Cases

**Test Suite** (curated list of real Clojure patterns):
1. Simple function definitions (`defn`)
2. Multi-arity functions
3. Destructuring in arglists
4. `let` bindings with maps/vectors
5. Nested function calls (deeply nested arithmetic)
6. Threading macros (`->`, `->>`, `as->`)
7. Data literals (maps, vectors, sets, lists)
8. Quoted forms (`'(...)`, `` ` (...)``)
9. Reader conditionals (`#?(:clj ...)`)
10. Anonymous functions (`#(...)`)

**Comparison Methodology**:
```clojure
;; Generate each test case TWO ways:

;; Method A: Ask Claude to write plain .clj
;; Method B: Ask Claude to write .cljp, transpile to .clj

;; Measure:
;; - Delimiter error rate (how many corrections needed?)
;; - Token count (A vs B)
;; - Time to generate
;; - Human readability score (subjective)
;; - Correctness (does it eval without error?)
```

## Phase 4: Real-World Validation

### 4.1 Dogfooding Setup

**Participants**: Gene Kim + volunteers willing to test

**Environment**:
```bash
# Install forked version
git clone https://github.com/<your-fork>/clojure-mcp-light.git
cd clojure-mcp-light
git checkout feature/cljp-support

# Configure Claude Code
cp settings_example ~/.config/claude-code/settings.json
# Edit: enable cljp-transpile hook
```

**Workflow**:
1. **Week 1**: Use CLJP for ALL new Clojure files (forced mode)
2. **Week 2**: Use CLJP only when Claude suggests errors (mixed mode)
3. **Week 3**: Disable CLJP, use plain .clj (control)

**Metrics to Track**:
```clojure
;; Log file: ~/.cljp-metrics.edn
{:session-id "uuid"
 :timestamp "2025-01-15T10:30:00Z"
 :file "src/demo/core.cljp"
 :mode :cljp | :plain
 :input-tokens 245
 :output-tokens 312
 :transpile-success? true
 :transpile-time-ms 23
 :parinfer-corrections 0   ;; How many fixes did parinfer make?
 :delimiter-errors 0        ;; Manual count by human
 :subjective-ease 4}        ;; 1-5 scale
```

### 4.2 Observability

**Logging Hook** (reports to metrics file):
```bash
#!/usr/bin/env bash
# hooks/cljp-metrics-logger

FILE_PATH="$1"
START_TIME=$(date +%s%3N)

# Run transpile
OUTPUT=$(bb -m cljp.core/transpile-file "$FILE_PATH") || EXIT_CODE=$?

END_TIME=$(date +%s%3N)
ELAPSED=$((END_TIME - START_TIME))

# Log to metrics file
bb -e "
(require '[clojure.edn :as edn])
(spit (str (System/getProperty \"user.home\") \"/.cljp-metrics.edn\")
      (pr-str {:file \"$FILE_PATH\"
               :success? $([ $EXIT_CODE -eq 0 ] && echo true || echo false)
               :elapsed-ms $ELAPSED
               :timestamp (str (java.time.Instant/now))})
      :append true)
"

exit ${EXIT_CODE:-0}
```

### 4.3 Feedback Loop

**Weekly Review**:
1. Analyze metrics (success rate, time, token cost)
2. Collect qualitative feedback (frustrations, surprises)
3. Identify pain points (e.g., "Maps are annoying in CLJP")
4. Iterate on format or tooling

**Decision Criteria** (after 3 weeks):
- **Success**: Delimiter error rate drops >30% → contribute upstream
- **Neutral**: No significant improvement → keep as optional fork
- **Failure**: Errors increase or DX degrades → abandon

## Phase 5: Upstream Contribution

### 5.1 Pre-PR Checklist

**Code Quality**:
- [ ] All tests pass (`bb test`)
- [ ] No new dependencies (pure stdlib)
- [ ] Code follows project style (match existing hooks)
- [ ] Error messages are helpful
- [ ] Performance is acceptable (<50ms for typical files)

**Documentation**:
- [ ] README.md updated with CLJP section
- [ ] docs/CLJP.md is comprehensive
- [ ] settings_example includes CLJP config
- [ ] CHANGELOG.md entry added

**Testing**:
- [ ] Golden file tests (20+ cases)
- [ ] Unit tests (>90% coverage)
- [ ] Integration tests with Claude Code
- [ ] Real-world validation data (3+ weeks)

### 5.2 PR Strategy

**Title**: `feat: Add CLJP (Clojure Push/Pop) transpiler hook`

**Description** (template):
```markdown
## Summary
This PR adds support for `.cljp` files, an intermediate format that helps
LLMs generate well-balanced s-expressions by using explicit `PUSH`/`POP`
operations instead of matched delimiters.

## Motivation
While LLMs are generally good at balancing parentheses, errors still occur
in deeply nested code. CLJP provides an optional "training wheels" mode
where delimiter balancing is guaranteed by a deterministic assembler.

## Implementation
- `src/cljp/core.clj`: Tokenizer and assembler (pure Clojure, no deps)
- `hooks/cljp-transpile-hook`: Pre-write hook that converts `.cljp` → `.clj`
- `bb.edn`: New `cljp/transpile` task
- `test/cljp/`: Unit and integration tests
- `docs/CLJP.md`: User guide

## Validation
- ✅ 100+ test cases (golden files)
- ✅ 3 weeks dogfooding in real projects
- ✅ Measured 35% reduction in delimiter errors (sample size: N=50 files)
- ✅ Avg transpile time: 18ms (acceptable overhead)

## Configuration
CLJP is **opt-in** by default. Enable in settings:
```json
{"hooks": {"pre_write_file": [{"id": "cljp-transpile", "enabled": true}]}}
```

## Alternatives Considered
1. **MCP server approach** (too much latency)
2. **Tree-sitter error recovery** (post-hoc, not preventative)
3. **Enhanced parinfer** (heuristic, not guaranteed)

## Breaking Changes
None. CLJP is purely additive; existing workflows are unaffected.

## Open Questions
- Should CLJP be enabled by default? (Propose: no)
- Should we add LSP support for `.cljp`? (Future work)
```

**Review Process**:
1. Submit PR to `bhauman/clojure-mcp-light`
2. Address maintainer feedback
3. Add requested tests/docs
4. Iterate until approval
5. Merge to main
6. Celebrate 🎉

### 5.3 Contingency: If PR Rejected

**Reasons PR might be rejected**:
- Adds complexity without clear benefit
- Maintainer philosophically opposed to intermediate formats
- Conflicts with other planned features
- Insufficient real-world validation

**Fallback Plan**:
1. **Keep as fork**: Maintain `clojure-mcp-light-cljp` independently
2. **Document clearly**: "This is an experimental fork; use at your own risk"
3. **Provide migration path**: Easy to merge upstream changes
4. **Build community**: Share on Clojurians Slack, Reddit, blog posts

**Exit criteria**:
- If CLJP proves valuable, others will fork/adopt
- If CLJP is not useful, quietly archive the fork

## Phase 6: Future Enhancements (Post-v1)

### 6.1 LSP Support
- Syntax highlighting for `.cljp` files
- Inline error squiggles (underflow, unclosed)
- Hover tooltips showing compiled `.clj` output
- Autocomplete for `PUSH`, `POP` keywords

### 6.2 Bidirectional Conversion
- **CLJ → CLJP**: Decompile regular Clojure to CLJP for editing
- Use case: "Convert this complex macro to CLJP so I can safely refactor it"

### 6.3 Gradual CLJP
- **Hybrid mode**: Mix `.clj` and `.cljp` syntax in same file
- Example:
  ```clojure
  (ns demo.core)  ;; Regular Clojure

  ; cljp-mode: on
  PUSH ( defmacro complex [& body]
    PUSH ` PUSH ( let ...
  ; cljp-mode: off

  (defn simple [x]  ;; Back to regular Clojure
    (inc x))
  ```

### 6.4 CLJP Optimization
- **Token compression**: `PUSH( ... POP` → `FORM( ... )`
- **Macro expansion**: Pre-expand common patterns (`defn` → `def` + `fn`)
- **Source maps**: Map compiled `.clj` positions back to `.cljp` for debugging

### 6.5 Generalize to Other Lisps
- **ELISP**: `.elispp` for Emacs Lisp
- **Common Lisp**: `.clp` for CL
- **Scheme**: `.scmp` for Scheme

## Timeline

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| **Phase 1**: Fork & setup | 1 day | Forked repo, branch created |
| **Phase 2**: Implementation | 1 week | Core assembler, hook, tests |
| **Phase 3**: Testing | 1 week | Unit tests, golden files, CI |
| **Phase 4**: Validation | 3 weeks | Real-world usage data |
| **Phase 5**: PR | 1 week | PR submitted, review, merge |
| **Phase 6**: Future work | Ongoing | Enhancements based on feedback |

**Total time to MVP**: ~2 weeks
**Total time to upstream PR**: ~6 weeks

## Success Metrics

### Must-Have (MVP)
- ✅ Assembler transpiles 100+ test cases correctly
- ✅ Hook works seamlessly in Claude Code
- ✅ Error messages are actionable
- ✅ No regressions in existing parinfer workflow

### Should-Have (Upstream PR)
- ✅ Delimiter error rate reduction >20%
- ✅ Subjective DX is neutral or positive
- ✅ Transpile time <50ms per file
- ✅ Comprehensive docs (users can self-serve)

### Nice-to-Have (Future)
- ✅ LSP support for `.cljp` files
- ✅ Bidirectional CLJ ↔ CLJP conversion
- ✅ Adoption by >10 users outside core team

## Risk Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| PR rejected by maintainer | Medium | High | Keep as fork; build community |
| CLJP doesn't reduce errors | Medium | High | Make opt-in; don't force adoption |
| Transpiler has bugs | High | Medium | Comprehensive tests; graceful failures |
| Performance issues | Low | Medium | Profile and optimize hot paths |
| Confuses users | Low | High | Excellent docs; clear when to use |
| Conflicts with upstream changes | Medium | Low | Regularly merge upstream; minimal changes |

## Open Questions

1. **Should CLJP support reader macros** (`'`, `` ` ``, `~`, `@`, `#()`)?
   - **Proposal**: v1 requires explicit expansion; v2 adds macro tokens

2. **Should top-level atoms be allowed** (currently disallowed)?
   - **Proposal**: Keep strict in v1; add `:loose` mode in v2

3. **Should we integrate with cljfmt** for pretty-printing?
   - **Proposal**: Optional; add `cljp.format-output` config flag

4. **Should errors abort file writes** or just warn?
   - **Proposal**: Abort by default; add `cljp.error-mode: warn` option

5. **Should we support sets** (`#{...}`) in v1?
   - **Proposal**: No; use `(set [...])` workaround until v2

## Conclusion

This plan provides a clear, phased approach to integrating CLJP into `clojure-mcp-light`. The key insight is to **start small, validate empirically, and iterate based on real-world feedback**. If CLJP proves valuable, we contribute upstream; if not, we learn something interesting about LLMs and delimiters.

**Next immediate steps**:
1. Fork `clojure-mcp-light` (done above)
2. Implement core assembler (`src/cljp/core.clj`)
3. Write 20 golden file tests
4. Dogfood for 1 week and collect data

Let's ship it! 🚀
