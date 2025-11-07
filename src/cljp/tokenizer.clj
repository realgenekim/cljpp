(ns cljp.tokenizer
  "CLJP tokenizer: converts CLJP source text into a token stream.

  Token types:
    [:push opener line col]  where opener is one of \"(\" \"[\" \"{\"
    [:pop line col]
    [:pop-line line col]
    [:pop-all line col]
    [:atom value line col]   where value is a string representation of the atom

  Syntax:
    PUSH-( PUSH-[ PUSH-{ - opens a container
    POP - closes current container
    POP-LINE - closes all containers opened on current line
    POP-ALL - closes all containers in stack
    All other words are atoms

  The tokenizer is intentionally simple and delegates most validation to the assembler."
  (:require [clojure.string :as str]))

(defn- is-delimiter? [c]
  "Only ), ], } are delimiters (and they're all invalid in CLJP)"
  (some #{c} [\) \] \}]))

(defn- whitespace? [c]
  (Character/isWhitespace ^char c))

(defn- read-string-literal
  "Read a string literal from source starting at index i.
  Returns [token next-index] or throws on error."
  [^String s i line col]
  (loop [j (inc i) sb (StringBuilder.) esc? false]
    (when (>= j (count s))
      (throw (ex-info "Unclosed string literal"
                      {:code :tokenize
                       :pos i
                       :line line
                       :col col
                       :context (subs s i (min (+ i 20) (count s)))})))
    (let [ch (.charAt s j)]
      (cond
        esc? (do (.append sb ch) (recur (inc j) sb false))
        (= ch \\) (do (.append sb ch) (recur (inc j) sb true))
        (= ch \") [[:atom (str "\"" (.toString sb) "\"") line col] (inc j)]
        :else (do (.append sb ch) (recur (inc j) sb false))))))

(defn- read-word
  "Read a word (symbol/keyword/number/boolean/nil/PUSH-/POP) from source starting at index i.
  Returns [token next-index] or nil if nothing to read.
  Recognizes PUSH-(, PUSH-[, PUSH-{, POP, POP-LINE, POP-ALL as special tokens."
  [^String s i line col]
  (let [sb (StringBuilder.)]
    (loop [j i]
      (if (or (>= j (count s))
              (whitespace? (.charAt s j))
              (is-delimiter? (.charAt s j)))
        (let [tok (.toString sb)]
          (when-not (empty? tok)
            ;; Check for PUSH- and POP tokens
            (case tok
              "PUSH-(" [[:push "(" line col] j]
              "PUSH-[" [[:push "[" line col] j]
              "PUSH-{" [[:push "{" line col] j]
              "POP" [[:pop line col] j]
              "POP-LINE" [[:pop-line line col] j]
              "POP-ALL" [[:pop-all line col] j]
              [[:atom tok line col] j])))
        (do (.append sb (.charAt s j))
            (recur (inc j)))))))

(defn- skip-comment
  "Skip a comment line starting at index i. Returns index after newline."
  [^String s i]
  (loop [j i]
    (if (or (>= j (count s)) (= \newline (.charAt s j)))
      (inc (min j (dec (count s))))
      (recur (inc j)))))

(defn tokenize
  "Tokenize CLJP source string into a vector of tokens.

  Returns vector of tokens where each token is one of:
    [:push \"(\" line col]  or [:push \"[\" line col]  or [:push \"{\" line col]
    [:pop line col]
    [:pop-line line col]
    [:pop-all line col]
    [:atom \"value\" line col]

  Throws ex-info with {:code :tokenize ...} on lexical errors."
  [^String source]
  (loop [i 0 line 1 col 0 tokens []]
    (if (>= i (count source))
      tokens
      (let [ch (.charAt source i)]
        (cond
          ;; Newline - increment line, reset col
          (= ch \newline)
          (recur (inc i) (inc line) 0 tokens)

          ;; Whitespace (not newline) - skip
          (whitespace? ch)
          (recur (inc i) line (inc col) tokens)

          ;; Comment - skip line
          (= ch \;)
          (let [next-i (skip-comment source i)]
            (recur next-i (inc line) 0 tokens))

          ;; String literal
          (= ch \")
          (let [[tok next-i] (read-string-literal source i line col)]
            (recur next-i line (+ col (- next-i i)) (conj tokens tok)))

          ;; Invalid closers ), ], }
          (is-delimiter? ch)
          (throw (ex-info (str "Invalid closer '" ch "' in CLJP - use POP instead")
                          {:code :tokenize
                           :pos i
                           :line line
                           :col col
                           :char ch
                           :msg "CLJP only allows POP to close containers"}))

          ;; Regular word (includes PUSH-(, PUSH-[, PUSH-{, POP, POP-LINE, POP-ALL, and atoms)
          :else
          (if-let [[tok next-i] (read-word source i line col)]
            (recur next-i line (+ col (- next-i i)) (conj tokens tok))
            (recur (inc i) line (inc col) tokens)))))))

(defn format-token
  "Format a token for pretty-printing (used in error messages)."
  [token]
  (let [[tag value-or-line line-or-col col] token]
    (case tag
      :push (str "PUSH-" value-or-line)
      :pop "POP"
      :pop-line "POP-LINE"
      :pop-all "POP-ALL"
      :atom value-or-line)))

(defn tokens->source
  "Convert tokens back to CLJP source (for round-trip testing)."
  [tokens]
  (str/join " " (map format-token tokens)))
