#!/usr/bin/env bb

;; Test a single variant across one or more programs
;; Usage: bb bin/test-variant.clj <variant> <program-num> <iterations>
;;   variant: clj, pop, popall, v4, etc.
;;   program-num: 1-20, or "all" for all programs
;;   iterations: how many times to run (default: 1)

(ns test-variant
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [babashka.fs :as fs]))

(def variants
  {:clj    {:file nil :name "Regular Clojure"}
   :pop    {:file "CLJPP-PROMPT.md" :name "CLJ-PP (explicit POP)"}
   :popall {:file "CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md" :name "CLJ-PP (POP-ALL v2)"}
   :v3     {:file "CLJPP-PROMPT-WITH-POP-ALL-ONLY-v3.md" :name "CLJ-PP (POP-ALL v3)"}
   :v4     {:file "CLJPP-PROMPT-v4.md" :name "CLJ-PP v4 (Hybrid)"}})

(def timestamp (.format (java.time.LocalDateTime/now)
                       (java.time.format.DateTimeFormatter/ofPattern "yyyyMMdd-HHmmss")))

(defn get-prompt [num]
  "Extract prompt for a specific program number from test-prompts.txt"
  (let [lines (str/split-lines (slurp "test-prompts.txt"))
        start-pattern (re-pattern (str "^Program " num ":"))
        next-pattern #"^Program \d+:"]
    (->> lines
         (drop-while #(not (re-find start-pattern %)))
         (take-while #(or (re-find start-pattern %)
                         (not (re-find next-pattern %))))
         (str/join "\n"))))

(defn test-program [variant program-num iter results-dir]
  "Test a single program with the given variant"
  (let [variant-info (get variants variant)
        _ (when-not variant-info
            (throw (ex-info (str "Unknown variant: " variant
                                "\nAvailable: " (keys variants))
                           {})))

        prompt-text (get-prompt program-num)
        program-spec (str "Write a Clojure program for this task:\n\n"
                         prompt-text "\n\n"
                         "Requirements:\n"
                         "- Use namespace examples.program" program-num "\n"
                         "- Write idiomatic Clojure\n"
                         "- Output ONLY the code, starting with (ns or PUSH-(\n"
                         "- No explanations or markdown")

        is-cljpp (not= variant :clj)
        prefix (format "%02d-%s-iter%02d" program-num (name variant) iter)
        raw-file (str results-dir "/" prefix ".raw")
        code-file (str results-dir "/" prefix (if is-cljpp ".cljpp" ".clj"))
        final-file (str results-dir "/" prefix ".clj")]

    (println (format "\n[%02d/%s/iter%02d] Testing..." program-num (name variant) iter))

    (try
      ;; Generate code
      (let [full-prompt (if-let [prompt-file (:file variant-info)]
                         (str (slurp prompt-file) "\n\n" program-spec)
                         program-spec)
            result (shell/sh "claude" "--print" full-prompt)]
        (spit raw-file (:out result)))

      ;; Clean markdown fences
      (let [content (slurp raw-file)
            cleaned (-> content
                       (str/replace #"```clojure\n" "")
                       (str/replace #"```\n" "")
                       (str/replace #"```$" ""))]
        (spit code-file cleaned))

      ;; Transpile if CLJPP
      (when is-cljpp
        (let [transpile-result (shell/sh "bin/cljpp" code-file final-file)]
          (when-not (zero? (:exit transpile-result))
            (println "  ❌ TRANSPILE ERROR")
            (println "    " (:err transpile-result))
            (throw (ex-info "Transpile failed" {:exit (:exit transpile-result)
                                                 :stderr (:err transpile-result)})))))

      ;; Test execution
      (let [test-file (if is-cljpp final-file code-file)
            test-result (shell/sh "bb" test-file)]
        (if (zero? (:exit test-result))
          (do
            (println "  ✓ SUCCESS")
            {:status :success :program program-num :variant variant :iter iter})
          (do
            (println "  ❌ EXECUTION ERROR")
            {:status :failed :program program-num :variant variant :iter iter
             :error (:err test-result)})))

      (catch Exception e
        (println "  ❌ ERROR:" (.getMessage e))
        {:status :error :program program-num :variant variant :iter iter
         :error (.getMessage e)}))))

(defn run-tests [variant program-nums iterations]
  "Run tests for given variant, programs, and iterations"
  (let [results-dir (str "experiments/test-variant-" (name variant) "-" timestamp)]
    (fs/create-dirs results-dir)
    (println "========================================")
    (println (str "Testing variant: " (name variant)))
    (println (str "Programs: " (if (= program-nums :all) "1-20" (str/join ", " program-nums))))
    (println (str "Iterations: " iterations))
    (println (str "Results dir: " results-dir))
    (println "========================================")

    (let [programs (if (= program-nums :all) (range 1 21) program-nums)
          test-cases (for [prog programs
                           iter (range 1 (inc iterations))]
                       [prog iter])
          ;; Use pmap for parallel execution
          results (doall (pmap (fn [[prog iter]]
                                  (test-program variant prog iter results-dir))
                               test-cases))]

      ;; Summary
      (println "\n========================================")
      (println "SUMMARY")
      (println "========================================")
      (let [grouped (group-by :status results)
            success (count (get grouped :success []))
            total (count results)]
        (println (format "%s: %d/%d (%.0f%%)"
                        (get-in variants [variant :name])
                        success total
                        (* 100.0 (/ success total))))

        ;; Show failures
        (when-let [failures (seq (concat (get grouped :failed [])
                                        (get grouped :error [])))]
          (println "\nFailures:")
          (doseq [{:keys [program iter error]} failures]
            (println (format "  - Program %02d iter %02d: %s"
                            program iter (or error "failed")))))

        (println (format "\nResults saved to: %s" results-dir))))))

(defn usage []
  (println "Usage: bb bin/test-variant.clj <variant> <program> <iterations>")
  (println "")
  (println "Arguments:")
  (println "  variant     One of: clj, pop, popall, v3, v4")
  (println "  program     Program number (1-20) or 'all'")
  (println "  iterations  How many times to run (default: 1)")
  (println "")
  (println "Examples:")
  (println "  bb bin/test-variant.clj v4 3 10          # Test v4 on program 3, 10 times")
  (println "  bb bin/test-variant.clj v4 all 1         # Test v4 on all programs, once")
  (println "  bb bin/test-variant.clj pop 13 5         # Test explicit POP on program 13, 5 times")
  (println "")
  (println "Available variants:")
  (doseq [[k v] variants]
    (println (format "  %-7s %s" (name k) (:name v)))))

;; Main
(let [args *command-line-args*]
  (if (or (< (count args) 2)
          (some #(= % "--help") args)
          (some #(= % "-h") args))
    (usage)
    (let [variant (keyword (first args))
          program-arg (second args)
          iterations (if (>= (count args) 3)
                      (Integer/parseInt (nth args 2))
                      1)
          program-nums (if (= program-arg "all")
                        :all
                        [(Integer/parseInt program-arg)])]

      (when-not (contains? variants variant)
        (println (str "Error: Unknown variant '" variant "'"))
        (println (str "Available: " (str/join ", " (map name (keys variants)))))
        (System/exit 1))

      (run-tests variant program-nums iterations))))
