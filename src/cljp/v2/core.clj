(ns cljp.v2.core
  "CLJ-PP v2 public API.

  Main entry points:
    (transpile source) - Convert CLJ-PP v2 source to Clojure
    (transpile-file input-path output-path) - File-based transpilation"
  (:require [cljp.v2.tokenizer :as tokenizer]
            [cljp.v2.assembler :as assembler]
            [clojure.java.io :as io]))

(defn transpile
  "Transpile CLJ-PP v2 source string to Clojure source string.

  Returns a map with either:
    {:ok? true :source \"...\"}
    {:ok? false :error {...}}"
  [source]
  (try
    (let [tokens (tokenizer/tokenize source)
          result (assembler/assemble tokens)]
      result)
    (catch Exception e
      {:ok? false
       :error {:code :exception
               :msg (.getMessage e)
               :data (ex-data e)}})))

(defn transpile-file
  "Transpile a CLJ-PP v2 file to a Clojure file.

  Returns:
    {:ok? true} on success
    {:ok? false :error {...}} on failure"
  [input-path output-path]
  (try
    (let [source (slurp input-path)
          result (transpile source)]
      (if (:ok? result)
        (do
          (spit output-path (:source result))
          {:ok? true})
        result))
    (catch Exception e
      {:ok? false
       :error {:code :io
               :msg (.getMessage e)
               :input input-path
               :output output-path}})))

(defn format-error
  "Format an error map into a human-readable string."
  [error]
  (let [{:keys [code msg line col pos depth]} error]
    (cond
      (and line col)
      (format "Error at line %d, col %d: %s" line col msg)

      pos
      (format "Error at position %d: %s" pos msg)

      depth
      (format "Error (depth %d): %s" depth msg)

      :else
      (format "Error: %s" msg))))

(defn -main
  "CLI entry point for transpilation.
  Usage: cljpp-v2 <input-file> <output-file>"
  [& args]
  (if (not= (count args) 2)
    (do
      (println "Usage: cljpp-v2 <input-file> <output-file>")
      (println "")
      (println "Transpiles CLJ-PP v2 source to Clojure.")
      (System/exit 1))
    (let [[input output] args
          result (transpile-file input output)]
      (if (:ok? result)
        (do
          (println (str "✓ Transpiled " input " -> " output))
          (System/exit 0))
        (do
          (println (str "✗ Transpilation failed: " input))
          (println (format-error (:error result)))
          (System/exit 1))))))
