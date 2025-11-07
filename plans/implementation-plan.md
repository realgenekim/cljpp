# POP-LINE and POP-ALL Implementation Plan

## Goal
Implement POP-LINE and POP-ALL in parser, but don't document in prompts yet. This allows us to test both approaches empirically.

## Overview

**Three closing operations:**
1. **POP** - Close exactly one container (already exists)
2. **POP-LINE** - Close all containers opened on current line (NEW)
3. **POP-ALL** - Close ALL containers in entire stack (NEW)

## Phase 1: Implementation

### 1.1 Lexer Changes (src/cljp/core.clj)

**Add new token types:**
```clojure
;; Current tokens
:push-paren   ; PUSH-(
:push-bracket ; PUSH-[
:push-brace   ; PUSH-{
:pop          ; POP

;; NEW tokens
:pop-line     ; POP-LINE
:pop-all      ; POP-ALL
```

**Lexer function changes:**
```clojure
(defn tokenize [input]
  ;; Add recognition for:
  ;; "POP-LINE" -> {:type :pop-line, :line N, :col M}
  ;; "POP-ALL"  -> {:type :pop-all, :line N, :col M}
  )
```

### 1.2 Parser State Changes

**Current state:**
```clojure
{:stack [{:type :list}]     ; Just container type
 :output []
 :pos 0}
```

**Enhanced state:**
```clojure
{:stack [{:type :list, :line 1, :col 0}]  ; Add line/col tracking
 :output []
 :pos 0
 :current-line 1}  ; Track current line for POP-LINE
```

### 1.3 POP-LINE Implementation

**Algorithm:**
```clojure
(defn handle-pop-line [state token]
  (let [current-line (:line token)
        ;; Find all containers opened on this line
        line-containers (filter #(= (:line %) current-line) (:stack state))]

    (if (empty? line-containers)
      ;; ERROR: No containers opened on this line
      (throw (ex-info "POP-LINE with no containers on this line"
                      {:line current-line
                       :pos (:pos token)}))

      ;; Close all containers from this line (reverse order - LIFO)
      (reduce (fn [s _] (pop-container s))
              state
              line-containers))))
```

**Key insight:** POP-LINE only closes containers where `(:line container) == current-line`

### 1.4 POP-ALL Implementation

**Algorithm:**
```clojure
(defn handle-pop-all [state token]
  (if (empty? (:stack state))
    ;; ERROR: Stack already empty
    (throw (ex-info "POP-ALL with empty stack"
                    {:pos (:pos token)}))

    ;; Close ALL containers (reverse order - LIFO)
    (reduce (fn [s _] (pop-container s))
            state
            (:stack state))))
```

**Key insight:** POP-ALL closes everything, regardless of line

### 1.5 Error Messages

**New error types:**
```clojure
;; POP-LINE errors
{:code :pop-line-no-containers
 :msg "POP-LINE with no containers opened on this line"
 :line 5
 :pos 123}

;; POP-ALL errors
{:code :pop-all-empty-stack
 :msg "POP-ALL with empty stack"
 :pos 456}

;; Helpful: show what WOULD have been closed
{:code :pop-line-no-containers
 :msg "POP-LINE with no containers opened on this line"
 :line 5
 :pos 123
 :stack-state [{:type :list, :line 3} {:type :vector, :line 3}]
 :help "Containers were opened on line 3, not line 5"}
```

## Phase 2: Tests

### 2.1 POP-LINE Tests

**Test 1: Basic POP-LINE (1 container)**
```clojure
;; Input
PUSH-( foo x POP-LINE

;; Expected output
(foo x)

;; Stack trace:
;; Line 1: PUSH-( foo x    -> stack: [(:list, line 1)]
;; Line 1: POP-LINE        -> close 1 container from line 1
;; Result: (foo x)
```

**Test 2: POP-LINE with 2 containers**
```clojure
;; Input
PUSH-( * 2 PUSH-( inc x POP-LINE

;; Expected output
(* 2 (inc x))

;; Stack trace:
;; Line 1: PUSH-( *        -> stack: [(:list, line 1)]
;; Line 1: PUSH-( inc      -> stack: [(:list, line 1), (:list, line 1)]
;; Line 1: POP-LINE        -> close 2 containers from line 1
;; Result: (* 2 (inc x))
```

**Test 3: POP-LINE with 4 deep nesting**
```clojure
;; Input
PUSH-( f PUSH-( g PUSH-( h PUSH-( i x POP-LINE

;; Expected output
(f (g (h (i x))))

;; Stack trace: All 4 opened on line 1, POP-LINE closes all 4
```

**Test 4: POP-LINE error - no containers on line**
```clojure
;; Input
PUSH-( foo
  POP-LINE

;; Expected error
{:code :pop-line-no-containers
 :msg "POP-LINE with no containers opened on this line"
 :line 2
 :stack-state [(:list, line 1)]}
```

**Test 5: Mixed POP and POP-LINE**
```clojure
;; Input
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP-LINE
POP

;; Expected output
(defn add [a b]
  (+ a b))

;; Stack trace:
;; Line 1: PUSH-( defn     -> [(:list, 1)]
;; Line 1: PUSH-[ a b      -> [(:list, 1), (:vector, 1)]
;; Line 1: POP             -> [(:list, 1)]  (closes vector)
;; Line 2: PUSH-( +        -> [(:list, 1), (:list, 2)]
;; Line 2: POP-LINE        -> [(:list, 1)]  (closes + from line 2)
;; Line 3: POP             -> []            (closes defn from line 1)
```

### 2.2 POP-ALL Tests

**Test 6: POP-ALL with simple nesting**
```clojure
;; Input
PUSH-( defn foo PUSH-[ x POP
  PUSH-( * 2 x POP-ALL

;; Expected output
(defn foo [x]
  (* 2 x))

;; Stack trace:
;; Line 1: PUSH-( defn     -> [(:list, 1)]
;; Line 1: PUSH-[ x        -> [(:list, 1), (:vector, 1)]
;; Line 1: POP             -> [(:list, 1)]
;; Line 2: PUSH-( *        -> [(:list, 1), (:list, 2)]
;; Line 2: POP-ALL         -> []  (closes * AND defn)
```

**Test 7: POP-ALL with deep nesting**
```clojure
;; Input
PUSH-( defn fibonacci PUSH-[ n POP
  PUSH-( cond
    PUSH-( = n 0 POP-LINE 0
    PUSH-( = n 1 POP-LINE 1
    :else PUSH-( +
      PUSH-( fibonacci PUSH-( - n 1 POP-LINE
      PUSH-( fibonacci PUSH-( - n 2 POP-ALL

;; Expected output
(defn fibonacci [n]
  (cond
    (= n 0) 0
    (= n 1) 1
    :else (+
      (fibonacci (- n 1))
      (fibonacci (- n 2)))))

;; Stack trace at last line:
;; Before POP-ALL: [(:list, 1), (:list, 2), (:list, 5), (:list, 7), (:list, 7)]
;;                  ^defn       ^cond       ^+         ^fibonacci  ^-
;; After POP-ALL: []  (closes all 5!)
```

**Test 8: POP-ALL error - empty stack**
```clojure
;; Input
PUSH-( foo x POP-ALL
POP-ALL

;; Expected error
{:code :pop-all-empty-stack
 :msg "POP-ALL with empty stack"
 :line 2}
```

### 2.3 Integration Tests

**Test 9: Factorial with all three operations**
```clojure
;; Input
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP-LINE
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL

;; Expected output
(defn factorial [n]
  (if (<= n 1)
    1
    (* n (factorial (- n 1)))))
```

**Test 10: Hiccup with vectors and conditionals**
```clojure
;; Input
PUSH-[ :div
  PUSH-[ :h1 "Title" POP-LINE
  PUSH-( when active?
    PUSH-[ :p "Active" POP-ALL

;; Expected output
[:div
  [:h1 "Title"]
  (when active?
    [:p "Active"])]
```

## Phase 3: Test Implementation

### 3.1 Test Structure

```clojure
(deftest pop-line-tests
  (testing "POP-LINE with 1 container"
    (is (= "(foo x)"
           (transpile "PUSH-( foo x POP-LINE"))))

  (testing "POP-LINE with 2 containers"
    (is (= "(* 2 (inc x))"
           (transpile "PUSH-( * 2 PUSH-( inc x POP-LINE"))))

  (testing "POP-LINE with 4 deep nesting"
    (is (= "(f (g (h (i x))))"
           (transpile "PUSH-( f PUSH-( g PUSH-( h PUSH-( i x POP-LINE"))))

  (testing "POP-LINE error - no containers on line"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"POP-LINE with no containers"
          (transpile "PUSH-( foo\n  POP-LINE")))))

(deftest pop-all-tests
  (testing "POP-ALL closes everything"
    (is (= "(defn foo [x]\n  (* 2 x))"
           (transpile "PUSH-( defn foo PUSH-[ x POP\n  PUSH-( * 2 x POP-ALL"))))

  (testing "POP-ALL error - empty stack"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"POP-ALL with empty stack"
          (transpile "PUSH-( foo x POP-ALL\nPOP-ALL")))))

(deftest integration-tests
  (testing "Factorial with all operations"
    (is (= "(defn factorial [n]\n  (if (<= n 1)\n    1\n    (* n (factorial (- n 1)))))"
           (transpile "PUSH-( defn factorial PUSH-[ n POP\n  PUSH-( if PUSH-( <= n 1 POP-LINE\n    1\n    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL")))))
```

### 3.2 Test Files to Create

1. **test/cljp/pop_line_test.clj** - All POP-LINE tests
2. **test/cljp/pop_all_test.clj** - All POP-ALL tests
3. **test/cljp/integration_test.clj** - Mixed operations

## Phase 4: Experiments

### 4.1 Baseline: Current Approach (Explicit POP only)

**Script:** `run-fresh-experiment-cljpp.sh` (use existing)

**What it tests:** Fresh Claude instances with current CLJPP-PROMPT.md (explicit POP counting)

**Expected result:** ~80-90% success (based on 100% for factorial with enhanced prompt)

**Metrics to collect:**
```bash
# Output format
Program 01: ✅ Success / ❌ Transpile failed / ⚠️ Load failed
Program 02: ...
...
Program 20: ...

Summary:
- Success: 17/20 (85%)
- Transpile failures: 2 (POP counting errors)
- Load failures: 1 (logic error)
```

### 4.2 Experiment: With POP-LINE and POP-ALL

**Script:** Create `run-fresh-experiment-cljpp-enhanced.sh`

**What it tests:** Fresh Claude instances with CLJPP-PROMPT-WITH-POP-LINE-ALL.md

**Prompt changes:**
```markdown
# Add to CLJPP-PROMPT.md

## Advanced: POP-LINE and POP-ALL

### POP-LINE - Close All on This Line

When you've opened multiple containers on one line:

PUSH-( * 2 PUSH-( inc x POP-LINE
                        ↑
                     closes: inc, *

### POP-ALL - Close Everything

When you're completely done with the entire form:

PUSH-( defn foo PUSH-[ x POP
  PUSH-( * 2 x POP-ALL
                ↑
             closes: *, defn (everything!)

### Decision Tree

Am I on the last line of this form?
├─ YES → POP-ALL (closes everything)
└─ NO → Did I open multiple containers on this line?
    ├─ YES → POP-LINE (closes this line)
    └─ NO → POP (precise control)
```

**Expected result:** 90-95% success (hypothesis: easier than counting)

### 4.3 Comparative Analysis

**Run both experiments 3 times each for statistical significance:**

```bash
# Baseline (explicit POP)
for i in 1 2 3; do
  ./run-fresh-experiment-cljpp.sh > results/baseline-run-$i.txt
done

# With POP-LINE and POP-ALL
for i in 1 2 3; do
  ./run-fresh-experiment-cljpp-enhanced.sh > results/enhanced-run-$i.txt
done
```

**Metrics to compare:**
| Metric | Baseline (POP) | Enhanced (POP-LINE/ALL) | Improvement |
|--------|----------------|-------------------------|-------------|
| Success rate | ? | ? | ? |
| Transpile errors | ? | ? | ? |
| POP counting errors | ? | ? | ? |
| Load errors | ? | ? | ? |

**Questions to answer:**
1. Does POP-LINE/ALL improve success rate?
2. Does it reduce POP counting errors specifically?
3. Are there new error modes introduced?
4. Which programs benefit most from POP-LINE/ALL?

## Implementation Checklist

### Code
- [ ] Add POP-LINE and POP-ALL to lexer
- [ ] Enhance parser state to track line numbers
- [ ] Implement handle-pop-line function
- [ ] Implement handle-pop-all function
- [ ] Add error messages for new operations
- [ ] Update main parse loop to handle new tokens

### Tests
- [ ] Write POP-LINE tests (Tests 1-5)
- [ ] Write POP-ALL tests (Tests 6-8)
- [ ] Write integration tests (Tests 9-10)
- [ ] Run test suite: `make runtests-once`
- [ ] Verify all tests pass

### Experiments
- [ ] Run baseline experiment (current prompt, 3 runs)
- [ ] Create enhanced prompt with POP-LINE/ALL
- [ ] Create run-fresh-experiment-cljpp-enhanced.sh
- [ ] Run enhanced experiment (3 runs)
- [ ] Collect and analyze results
- [ ] Document findings

## Success Criteria

**Minimum bar:**
- Implementation works correctly (all tests pass)
- POP-LINE/ALL doesn't make things worse (success rate ≥ baseline)

**Success:**
- POP-LINE/ALL improves success rate by ≥5 percentage points
- OR reduces POP counting errors significantly

**Home run:**
- Success rate ≥95% with POP-LINE/ALL
- Fresh LLMs consistently use them correctly
- Cognitive load measurably reduced

## Timeline Estimate

- **Code implementation:** 2-3 hours
- **Test writing:** 1-2 hours
- **Baseline experiments:** 30 minutes (3 runs × 10 min)
- **Enhanced prompt creation:** 1 hour
- **Enhanced experiments:** 30 minutes (3 runs × 10 min)
- **Analysis:** 1-2 hours

**Total:** 6-9 hours

## Next Steps

1. ✅ Write this plan
2. Implement lexer changes
3. Implement parser changes
4. Write tests
5. Run baseline experiments
6. Create enhanced prompt
7. Run enhanced experiments
8. Analyze and document results

---

**Ready to start implementation?** Let me know and I'll begin with the lexer and parser changes.
