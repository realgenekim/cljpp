# Experiment Reproducibility & Parallelization Plan

## Current Problem

Our experiments generate results but lack:
- **Traceability**: Which prompt version was used?
- **Reproducibility**: What was the git commit? Can we re-run exactly?
- **Speed**: 20 iterations = ~10 minutes (sequential)
- **Analysis**: Hard to compare across experiment runs

## Proposed Solution

### 1. Metadata Format: EDN (not JSON)

**Why EDN over JSON?**
- Native Clojure format (easy to parse in Babashka)
- Supports rich data types (keywords, sets, etc.)
- Human-readable
- No external dependencies
- Better for Clojure tooling

**Structure:**
```clojure
{:experiment-id "pop-all-v2-20251106-191450"
 :timestamp "2025-11-06T19:14:50Z"
 :git {:commit "abc123def456..."
       :dirty? false
       :branch "main"}
 :prompt {:file "CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md"
          :sha256 "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
          :lines 398}
 :parameters {:program 3
              :description "factorial/fibonacci with cond"
              :iterations 20}
 :environment {:clojure-version "1.11.1"
               :java-version "21.0.1"
               :os "Darwin 24.5.0"
               :claude-code-version "0.x.x"}
 :results {:success-count 20
           :transpile-errors 0
           :load-errors 0
           :success-rate 100.0
           :total-time-ms 583291
           :iterations
           [{:iteration 1
             :success? true
             :generated-at "2025-11-06T19:14:52Z"
             :transpile-time-ms 1234
             :load-time-ms 567
             :files {:raw "iter1.raw.cljpp"
                     :cleaned "iter1.cljpp"
                     :transpiled "iter1.clj"}}
            {:iteration 2 ...}]}
 :files {:output-dir "experiments/popall-v2-clean/run-20251106-191450"
         :prompt-copy "prompt-used.md"
         :metadata "metadata.edn"
         :log "experiment.log"}}
```

### 2. Automatic Metadata Generation

**In every test run:**
```bash
# Before starting iterations
generate-metadata-header > $OUTPUT_DIR/metadata.edn
# Contains: git commit, prompt hash, timestamp, parameters

# After each iteration
append-iteration-result >> $OUTPUT_DIR/metadata.edn
# Contains: success/fail, timing, files

# After all iterations
finalize-metadata >> $OUTPUT_DIR/metadata.edn
# Contains: summary, totals, success rate
```

**Helper functions:**
```bash
get-git-commit() {
    git rev-parse HEAD
}

get-git-dirty() {
    [[ -n $(git status --porcelain) ]] && echo true || echo false
}

get-prompt-hash() {
    sha256sum "$1" | cut -d' ' -f1
}
```

### 3. Babashka Parallel Test Runner

**Why Babashka?**
- Fast startup (~10ms vs ~2s for Clojure JVM)
- Native parallel execution (`pmap`, `future`)
- Can shell out to `claude --print`
- Built-in EDN support
- Cross-platform

**Architecture:**
```clojure
#!/usr/bin/env bb

(require '[clojure.java.shell :as shell]
         '[clojure.edn :as edn]
         '[babashka.fs :as fs])

(defn run-iteration
  "Run a single test iteration. Returns result map."
  [{:keys [i output-dir prompt-file spec]}]
  (let [start-time (System/currentTimeMillis)
        raw-file (str output-dir "/iter" i ".raw.cljpp")
        cljpp-file (str output-dir "/iter" i ".cljpp")
        clj-file (str output-dir "/iter" i ".clj")]

    ;; 1. Generate code
    (shell/sh "claude" "--print"
              (str (slurp prompt-file) "\n\n" spec)
              :out raw-file)

    ;; 2. Clean markdown fences
    (shell/sh "sed" "-e" "/^```clojure$/d" "-e" "/^```$/d"
              raw-file :out cljpp-file)

    ;; 3. Transpile
    (let [transpile (shell/sh "bin/cljpp" cljpp-file clj-file)]
      (if (zero? (:exit transpile))
        ;; 4. Load and test
        (let [load (shell/sh "clojure" "-M" "-e"
                            (str "(load-file \"" clj-file "\")"))]
          {:iteration i
           :success? (zero? (:exit load))
           :transpile-time-ms (- (System/currentTimeMillis) start-time)
           :error (when-not (zero? (:exit load)) (:err load))})

        {:iteration i
         :success? false
         :transpile-error (:err transpile)}))))

(defn run-experiment
  "Run experiment with parallel execution."
  [{:keys [iterations] :as params}]
  (let [start-time (System/currentTimeMillis)
        ;; Run iterations in parallel!
        results (pmap #(run-iteration (assoc params :i %))
                     (range 1 (inc iterations)))
        success-count (count (filter :success? results))
        total-time (- (System/currentTimeMillis) start-time)]

    {:results results
     :summary {:success-count success-count
               :success-rate (* 100.0 (/ success-count iterations))
               :total-time-ms total-time}}))

;; Usage:
;; bb test-runner.clj --prompt CLJPP-PROMPT.md --iterations 20
```

**Benefits:**
- 20 iterations in ~2 minutes instead of ~10 minutes
- Automatic metadata.edn generation
- Progress reporting
- Error handling

### 4. Directory Structure

```
experiments/
├── pop-all-v2-clean/
│   └── run-20251106-191450/
│       ├── metadata.edn          ← Complete experiment metadata
│       ├── prompt-used.md        ← Snapshot of exact prompt
│       ├── experiment.log        ← Console output
│       ├── iter1.raw.cljpp       ← Raw Claude output
│       ├── iter1.cljpp           ← Cleaned CLJ-PP
│       ├── iter1.clj             ← Transpiled Clojure
│       ├── iter2.raw.cljpp
│       └── ...
├── pop-line-all/
│   └── run-TIMESTAMP/
│       └── ...
└── baseline/
    └── run-TIMESTAMP/
        └── ...
```

## Implementation Plan

### Phase 1: Metadata Generation (Immediate - 1 hour)

1. **Create `bin/generate-metadata.clj` script**
   ```bash
   #!/usr/bin/env bb
   (println (pr-str
     {:experiment-id (str "exp-" (java.util.UUID/randomUUID))
      :timestamp (java.time.Instant/now)
      :git {:commit (get-git-commit)
            :dirty? (get-git-dirty?)}
      :prompt {:file *prompt-file*
               :sha256 (get-prompt-hash *prompt-file*)}
      ...}))
   ```

2. **Update test scripts to generate metadata**
   ```bash
   # In test script header
   bb bin/generate-metadata.clj \
       --prompt "$PROMPT_FILE" \
       --iterations $ITERATIONS \
       > "$OUTPUT_DIR/metadata.edn"

   # Copy prompt for reference
   cp "$PROMPT_FILE" "$OUTPUT_DIR/prompt-used.md"
   ```

3. **Test with one experiment run**
   ```bash
   ./test-popall-v2-clean.sh 1
   cat experiments/popall-v2-clean/run-TIMESTAMP/metadata.edn
   # Should see complete metadata
   ```

### Phase 2: Babashka Runner (2-3 hours)

1. **Create `bb-test-runner.clj`**
   - Parallel execution with `pmap`
   - Automatic metadata generation
   - Progress reporting
   - Error handling

2. **Test with factorial/fibonacci**
   ```bash
   bb bb-test-runner.clj \
       --prompt CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md \
       --iterations 20 \
       --parallel 5  # 5 concurrent Claude instances
   ```

3. **Compare timing:**
   - Sequential: ~10 minutes
   - Parallel (5): ~2-3 minutes
   - Parallel (10): ~1-2 minutes (but might hit rate limits)

4. **Add to Makefile**
   ```makefile
   test-parallel-pop-all:
   	bb bb-test-runner.clj --prompt CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md \
   	                      --iterations $(ITERS) --parallel 5
   ```

### Phase 3: Analysis Tools (1-2 hours)

1. **Create `bin/compare-experiments.clj`**
   ```bash
   bb bin/compare-experiments.clj \
       experiments/pop-all-v2/run-*/metadata.edn \
       experiments/baseline/run-*/metadata.edn
   ```

   Output:
   ```
   Experiment Comparison
   =====================

   POP-ALL v2 (20251106-191450):
     Success: 20/20 (100%)
     Time: 583s
     Prompt: CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md (sha256:e3b0c44...)

   Baseline (20251106-165432):
     Success: 9/10 (90%)
     Time: 312s
     Prompt: CLJPP-PROMPT.md (sha256:a7c2e18...)

   Difference: +10% success rate
   ```

2. **Create `bin/reproduce-experiment.clj`**
   ```bash
   bb bin/reproduce-experiment.clj \
       experiments/pop-all-v2/run-20251106-191450/metadata.edn
   ```

   - Checks out exact git commit
   - Uses exact prompt (from snapshot)
   - Re-runs with same parameters
   - Compares results

### Phase 4: POP-LINE+ALL Review (After parallelization)

1. **Review original POP-LINE+ALL failures**
   - Check `experiments/pop-line-all-tests/run-*/`
   - Identify failure patterns
   - Compare with POP-ALL v2 success

2. **Create improved POP-LINE+ALL prompt**
   - Apply learnings from POP-ALL v2
   - Add "CRITICAL RULE FOR POP-LINE" section
   - Show WRONG patterns with ❌
   - Emphasize scope (line-level vs stack-level)

3. **Run parallel test**
   ```bash
   bb bb-test-runner.clj \
       --prompt CLJPP-PROMPT-WITH-POP-LINE-ALL-v2.md \
       --iterations 20 --parallel 5
   ```

4. **Compare:**
   - Original POP-LINE+ALL: 80%
   - POP-ALL v2: 100%
   - POP-LINE+ALL v2: TBD (hypothesis: 95%+?)

## Success Criteria

### Phase 1 (Metadata)
- ✅ Every experiment has metadata.edn
- ✅ Can determine git commit from metadata
- ✅ Can verify prompt used (hash + snapshot)
- ✅ Can trace all generated files

### Phase 2 (Parallelization)
- ✅ 20 iterations in <3 minutes (vs ~10 min sequential)
- ✅ Automatic metadata generation
- ✅ No manual errors (all automated)
- ✅ Make targets for all test variants

### Phase 3 (Analysis)
- ✅ Can compare experiments side-by-side
- ✅ Can reproduce any experiment from metadata
- ✅ Can generate comparison reports

### Phase 4 (POP-LINE+ALL)
- ✅ Improved prompt created
- ✅ Test run complete (20 iterations)
- ✅ Results documented
- ✅ EXPERIMENT-RESULTS.md updated

## Risk Analysis

**Risks:**
1. **Claude rate limits** with parallel execution
   - Mitigation: Configurable parallelism (default 5)
   - Mitigation: Exponential backoff on errors

2. **Metadata format changes** breaking old experiments
   - Mitigation: Version metadata schema
   - Mitigation: Keep old metadata for reference

3. **Git commit mismatch** if working directory dirty
   - Mitigation: Warn if git dirty
   - Mitigation: Record dirty status in metadata

4. **Prompt file moves/renames**
   - Mitigation: Snapshot prompt in output dir
   - Mitigation: Record prompt hash for verification

## Timeline

| Phase | Time | Status |
|-------|------|--------|
| Phase 1: Metadata | 1 hour | Not started |
| Phase 2: Babashka | 2-3 hours | Not started |
| Phase 3: Analysis | 1-2 hours | Not started |
| Phase 4: Review | 2-3 hours | Not started |
| **Total** | **6-9 hours** | **0% complete** |

## Next Steps

1. ✅ Get approval on EDN format
2. Create `bin/generate-metadata.clj`
3. Test metadata generation
4. Create `bb-test-runner.clj`
5. Test parallel execution
6. Add Make targets
7. Review POP-LINE+ALL

## Questions for Review

1. **Format**: EDN vs JSON? (I recommend EDN)
2. **Parallelism**: How many concurrent Claude instances? (I suggest 5 as safe default)
3. **Prompt snapshots**: Always copy, or just hash? (I suggest copy for true reproducibility)
4. **Git dirty**: Allow running with dirty git? (I suggest warn but allow)
5. **Priority**: Metadata first, or jump straight to parallel runner? (I suggest metadata first for immediate value)
