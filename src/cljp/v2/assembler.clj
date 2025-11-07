(ns cljp.v2.assembler
  "CLJ-PP v2 assembler: converts token stream into Clojure source code.

  This is a direct string-building transpiler that maintains a stack
  to track open containers and emits Clojure syntax directly.

  Key differences from v1:
  - Simpler token set (LP/LV/LM/X/X2/X3 vs PUSH-(/POP/POP-ALL)
  - No POP-ALL or POP-LINE (data shows they reduce success rate!)
  - Direct string output (no intermediate s-expressions)

  Error codes:
    :underflow - X/Xn with insufficient stack depth
    :unclosed - EOF with non-empty stack"
  (:require [clojure.string :as str]))

(defn- opener->char [opener]
  "Convert opener string to opening char."
  (case opener
    "(" \(
    "[" \[
    "{" \{
    (throw (ex-info "Unknown opener" {:opener opener}))))

(defn- closer->char [opener]
  "Convert opener string to closing char."
  (case opener
    "(" \)
    "[" \]
    "{" \}
    (throw (ex-info "Unknown opener" {:opener opener}))))

(defn- parse-atom
  "Parse an atom string into a Clojure value string.
  Handles strings (already quoted), keywords, numbers, booleans, nil, and symbols."
  [atom-str]
  ;; For v2, we just pass through the atom as-is for string output
  ;; The atom is already in proper Clojure syntax from the tokenizer
  atom-str)

(defn assemble
  "Assemble CLJ-PP v2 tokens into Clojure source code.

  Input: Vector of tokens from tokenizer
  Output: Either:
    {:ok? true :source \"...\"}
    {:ok? false :error {:code ... :msg ... :line ... :col ...}}"
  [tokens]
  (try
    (let [sb (StringBuilder.)
          stack (transient [])]
      (loop [i 0]
        (if (= i (count tokens))
          ;; EOF - check for unclosed forms
          (let [final-stack (persistent! stack)]
            (if (empty? final-stack)
              {:ok? true
               :source (.toString sb)}
              {:ok? false
               :error {:code :unclosed
                       :msg (str "Unclosed forms at EOF (depth: " (count final-stack) ")")
                       :depth (count final-stack)
                       :stack-info (mapv #(select-keys % [:opener :line]) final-stack)}}))

          ;; Process next token
          (let [token (nth tokens i)
                [tag & rest-token] token]
            (case tag
              :push
              (let [[opener line col] rest-token]
                (.append sb (opener->char opener))
                (conj! stack {:opener opener :line line :col col})
                (recur (inc i)))

              :close
              (let [[n line col] rest-token
                    current-depth (count stack)]
                (when (< current-depth n)
                  (throw (ex-info (str "Underflow: X" (when (> n 1) n) " requires " n " open containers, but only " current-depth " on stack")
                                  {:code :underflow
                                   :pos i
                                   :line line
                                   :col col
                                   :need n
                                   :have current-depth})))
                ;; Close n levels
                (dotimes [_ n]
                  (let [top (peek stack)]
                    (.append sb (closer->char (:opener top)))
                    (pop! stack)))
                (recur (inc i)))

              :atom
              (let [[value line col] rest-token
                    parsed (parse-atom value)]
                ;; Add space before atom if needed (not after opener, not first char)
                (when (and (> (.length sb) 0)
                           (let [last-ch (.charAt sb (dec (.length sb)))]
                             (not (or (= last-ch \()
                                     (= last-ch \[)
                                     (= last-ch \{)))))
                  (.append sb \space))
                (.append sb parsed)
                (recur (inc i)))

              ;; Unknown token type
              (throw (ex-info (str "Unknown token type: " tag)
                            {:code :tokenize
                             :token token})))))))
    (catch clojure.lang.ExceptionInfo e
      {:ok? false
       :error (merge {:msg (.getMessage e)} (ex-data e))})))

(defn assemble-from-string
  "Convenience wrapper: tokenize and assemble in one call."
  [source]
  (let [tokenizer (requiring-resolve 'cljp.v2.tokenizer/tokenize)]
    (try
      (let [tokens (tokenizer source)]
        (assemble tokens))
      (catch Exception e
        {:ok? false
         :error {:code :tokenize
                 :msg (.getMessage e)
                 :data (ex-data e)}}))))
