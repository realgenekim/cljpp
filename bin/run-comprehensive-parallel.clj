#!/usr/bin/env bb

(ns run-comprehensive-parallel
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [babashka.fs :as fs]))

(def timestamp (.format (java.time.LocalDateTime/now)
                       (java.time.format.DateTimeFormatter/ofPattern "yyyyMMdd-HHmmss")))
(def results-dir (str "experiments/comprehensive-test-parallel-" timestamp))

(defn get-prompt [num]
  (let [lines (str/split-lines (slurp "test-prompts.txt"))
        start-pattern (re-pattern (str "^Program " num ":"))
        next-pattern #"^Program \d+:"]
    (->> lines
         (drop-while #(not (re-find start-pattern %)))
         (take-while #(or (re-find start-pattern %)
                         (not (re-find next-pattern %))))
         (str/join "\n"))))

(defn test-single-approach [program-num prompt-text approach]
  (let [program-spec (str "Write a Clojure program for this task:\n\n"
                         prompt-text "\n\n"
                         "Requirements:\n"
                         "- Use namespace examples.program" program-num "\n"
                         "- Write idiomatic Clojure\n"
                         "- Output ONLY the code, starting with (ns or PUSH-(\n"
                         "- No explanations or markdown")
        prefix (str (format "%02d" program-num) "-" (name approach))
        raw-file (str results-dir "/" prefix ".raw")
        code-file (str results-dir "/" prefix (if (= approach :clj) ".clj" ".cljpp"))
        final-file (str results-dir "/" prefix ".clj")]

    (try
      ;; Generate code
      (let [full-prompt (case approach
                          :clj program-spec
                          :pop (str (slurp "CLJPP-PROMPT.md") "\n\n" program-spec)
                          :popall (str (slurp "CLJPP-PROMPT-WITH-POP-ALL-ONLY-v3.md") "\n\n" program-spec))
            result (shell/sh "claude" "--print" full-prompt)]
        (spit raw-file (:out result)))

      ;; Clean markdown fences
      (let [content (slurp raw-file)
            cleaned (-> content
                       (str/replace #"```clojure\n" "")
                       (str/replace #"```\n" "")
                       (str/replace #"```$" ""))]
        (spit code-file cleaned))

      ;; Transpile if needed
      (when (#{:pop :popall} approach)
        (let [transpile (shell/sh "bin/cljpp" code-file final-file)]
          (when-not (zero? (:exit transpile))
            (throw (ex-info "Transpile failed" {:exit (:exit transpile)})))))

      ;; Load and test
      (let [load-result (shell/sh "clojure" "-M" "-e"
                                 (str "(load-file \"" (if (= approach :clj) code-file final-file) "\")"))]
        (if (zero? (:exit load-result))
          {:program program-num :approach approach :result :success}
          {:program program-num :approach approach :result :load-error}))

      (catch Exception e
        {:program program-num :approach approach :result :error :msg (.getMessage e)}))))

(defn test-program [program-num]
  (let [prompt-text (get-prompt program-num)]
    (when-not (str/blank? prompt-text)
      (println (str "Testing program " (format "%02d" program-num) "..."))
      ;; Test all 3 approaches for this program
      (doall
        (for [approach [:clj :pop :popall]]
          (test-single-approach program-num prompt-text approach))))))

(defn -main []
  (fs/create-dirs results-dir)
  (println "==========================================")
  (println "Comprehensive CLJ-PP Experiment (PARALLEL BB)")
  (println "Testing all 20 programs with 3 approaches")
  (println (str "Results: " results-dir))
  (println "==========================================\n")

  ;; Run all programs in parallel using pmap
  (let [results (doall
                  (pmap test-program (range 1 21)))
        flattened (filter some? (flatten results))

        ;; Count successes by approach
        clj-success (count (filter #(and (= (:approach %) :clj)
                                        (= (:result %) :success)) flattened))
        pop-success (count (filter #(and (= (:approach %) :pop)
                                        (= (:result %) :success)) flattened))
        popall-success (count (filter #(and (= (:approach %) :popall)
                                           (= (:result %) :success)) flattened))]

    ;; Save results
    (spit (str results-dir "/results.edn")
          (pr-str {:timestamp timestamp
                   :results flattened
                   :summary {:clj clj-success
                            :pop pop-success
                            :popall popall-success}}))

    (println "\n==========================================")
    (println "FINAL RESULTS")
    (println "==========================================")
    (println (str "Regular Clojure:        " clj-success "/20 (" (int (* 100 (/ clj-success 20.0))) "%)"))
    (println (str "CLJ-PP (explicit POP):  " pop-success "/20 (" (int (* 100 (/ pop-success 20.0))) "%)"))
    (println (str "CLJ-PP (POP-ALL v2):    " popall-success "/20 (" (int (* 100 (/ popall-success 20.0))) "%)"))
    (println (str "\nResults saved to: " results-dir "/results.edn"))))

(-main)
