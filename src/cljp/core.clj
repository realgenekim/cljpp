(ns cljp.core
  "CLJP core API: main entry point for transpiling CLJP to Clojure.

  Public API:
    (transpile source)     - Convert CLJP string to Clojure string
    (transpile-file path)  - Read CLJP file, transpile, write .clj file
    (transpile-repl source)- Transpile and return forms for REPL evaluation"
  (:require [cljp.tokenizer :as tokenizer]
            [cljp.assembler :as assembler]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn transpile
  "Transpile CLJP source string to Clojure source string.

  Input: CLJP source code as string
  Output: Either:
    {:ok? true :clj \"...\" :forms [...]}
    {:ok? false :error {:code ... :msg ...}}"
  [source]
  (try
    (let [tokens (tokenizer/tokenize source)
          result (assembler/assemble tokens)]
      (if (:ok? result)
        {:ok? true
         :clj (:source result)
         :forms (:forms result)}
        result))
    (catch clojure.lang.ExceptionInfo e
      {:ok? false
       :error (merge {:msg (.getMessage e)} (ex-data e))})))

(defn transpile-repl
  "Transpile CLJP source and return forms ready for REPL evaluation.

  Input: CLJP source code as string
  Output: Vector of forms or throws on error"
  [source]
  (let [result (transpile source)]
    (if (:ok? result)
      (:forms result)
      (throw (ex-info (:msg (:error result))
                      (:error result))))))

(defn transpile-file
  "Transpile a .cljp file to a .clj file.

  Input: Path to .cljp file (string)
  Output: Path to generated .clj file (string) or throws on error

  Side effects:
    - Reads input-path file
    - Writes sibling .clj file (same name, different extension)
    - Throws on transpile errors"
  [input-path]
  (let [input-file (io/file input-path)]
    (when-not (.exists input-file)
      (throw (ex-info "Input file not found"
                      {:code :file-not-found
                       :path input-path})))

    (when-not (str/ends-with? input-path ".cljp")
      (throw (ex-info "Input file must have .cljp extension"
                      {:code :invalid-extension
                       :path input-path})))

    (let [source (slurp input-file)
          result (transpile source)]
      (if (:ok? result)
        (let [output-path (str/replace input-path #"\.cljp$" ".clj")
              output-file (io/file output-path)]
          (spit output-file (:clj result))
          output-path)
        (throw (ex-info (:msg (:error result))
                        (assoc (:error result) :input-path input-path)))))))

(defn -main
  "CLI entry point for CLJP transpiler.

  Usage:
    clojure -M -m cljp.core <input.cljp> [output.clj] [--force]

  Options:
    --force    Overwrite output file even if it's newer than input

  If output path is not specified, writes to sibling .clj file.
  By default, skips transpilation if .clj is newer than .cljp (use --force to override)."
  [& args]
  (try
    (when (empty? args)
      (binding [*out* *err*]
        (println "Usage: clojure -M -m cljp.core <input.cljp> [output.clj] [--force]")
        (println "")
        (println "Options:")
        (println "  --force    Overwrite output file even if it's newer than input")
        (println "")
        (println "By default, skips transpilation if .clj is newer than .cljp")
        (System/exit 2)))

    (let [force? (some #{"--force"} args)
          non-flag-args (remove #(str/starts-with? % "--") args)
          input-path (first non-flag-args)
          explicit-output (second non-flag-args)
          output-path (or explicit-output
                          (str/replace input-path #"\.cljp$" ".clj"))
          input-file (io/file input-path)
          output-file (io/file output-path)]

      (when-not (.exists input-file)
        (binding [*out* *err*]
          (println "Error: Input file not found:" input-path)
          (System/exit 1)))

      ;; Check if output is newer than input (unless --force)
      (when (and (.exists output-file)
                 (not force?)
                 (> (.lastModified output-file)
                    (.lastModified input-file)))
        (println "✓ Skipping (output is newer than input):" output-path)
        (println "  Use --force to overwrite")
        (System/exit 0))

      (let [source (slurp input-file)
            result (transpile source)]
        (if (:ok? result)
          (do
            (spit output-path (:clj result))
            (println "✓ Transpiled:" input-path "→" output-path)
            (System/exit 0))
          (do
            (binding [*out* *err*]
              (println "CLJP transpile error:")
              (clojure.pprint/pprint (:error result)))
            (System/exit 1)))))
    (catch Exception e
      (binding [*out* *err*]
        (println "Unexpected error:" (.getMessage e))
        (.printStackTrace e)
        (System/exit 1)))))

(comment
  ;; REPL usage examples

  ;; Simple function
  (transpile "PUSH ( defn foo PUSH [ x POP PUSH ( inc x POP POP")
  ;; => {:ok? true :clj "(defn foo [x]\n  (inc x))" :forms [(defn foo [x] (inc x))]}

  ;; Let with map
  (transpile "PUSH ( let PUSH [ m PUSH { :a 1 :b 2 POP POP PUSH ( println m POP POP")

  ;; Error: unclosed
  (transpile "PUSH ( defn foo PUSH [ x POP")
  ;; => {:ok? false :error {:code :unclosed ...}}

  ;; Error: map odd arity
  (transpile "PUSH ( def bad PUSH { :a 1 :b POP POP")
  ;; => {:ok? false :error {:code :map-odd-arity ...}}

  ;; File transpilation
  (spit "/tmp/test.cljp" "PUSH ( defn hello PUSH [ name POP PUSH ( println \"Hello,\" name POP POP")
  (transpile-file "/tmp/test.cljp")
  ;; => "/tmp/test.clj" (created)
  (slurp "/tmp/test.clj")
  ;; => "(defn hello [name]\n  (println \"Hello,\" name))\n"

  ;;
  )
