(ns cljp.tokenizer
  "CLJP tokenizer: converts CLJP source text into a token stream.

  Token types:
    [:push opener]  where opener is one of \"(\" \"[\" \"{\"
    [:pop]
    [:atom value]   where value is a string representation of the atom

  Syntax:
    PUSH-( PUSH-[ PUSH-{ - opens a container
    POP - closes current container
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
  [^String s i]
  (loop [j (inc i) sb (StringBuilder.) esc? false]
    (when (>= j (count s))
      (throw (ex-info "Unclosed string literal"
                      {:code :tokenize
                       :pos i
                       :context (subs s i (min (+ i 20) (count s)))})))
    (let [ch (.charAt s j)]
      (cond
        esc? (do (.append sb ch) (recur (inc j) sb false))
        (= ch \\) (do (.append sb ch) (recur (inc j) sb true))
        (= ch \") [[:atom (str "\"" (.toString sb) "\"")] (inc j)]
        :else (do (.append sb ch) (recur (inc j) sb false))))))

(defn- read-word
  "Read a word (symbol/keyword/number/boolean/nil/PUSH-/POP) from source starting at index i.
  Returns [token next-index] or nil if nothing to read.
  Recognizes PUSH-(, PUSH-[, PUSH-{ and POP as special tokens."
  [^String s i]
  (let [sb (StringBuilder.)]
    (loop [j i]
      (if (or (>= j (count s))
              (whitespace? (.charAt s j))
              (is-delimiter? (.charAt s j)))
        (let [tok (.toString sb)]
          (when-not (empty? tok)
            ;; Check for PUSH- and POP tokens
            (case tok
              "PUSH-(" [[:push "("] j]
              "PUSH-[" [[:push "["] j]
              "PUSH-{" [[:push "{"] j]
              "POP" [[:pop] j]
              [[:atom tok] j])))
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
    [:push \"(\"]  or [:push \"[\"]  or [:push \"{\"]
    [:pop]
    [:atom \"value\"]

  Throws ex-info with {:code :tokenize ...} on lexical errors."
  [^String source]
  (loop [i 0 tokens []]
    (if (>= i (count source))
      tokens
      (let [ch (.charAt source i)]
        (cond
          ;; Whitespace - skip
          (whitespace? ch)
          (recur (inc i) tokens)

          ;; Comment - skip line
          (= ch \;)
          (recur (skip-comment source i) tokens)

          ;; String literal
          (= ch \")
          (let [[tok next-i] (read-string-literal source i)]
            (recur next-i (conj tokens tok)))

          ;; Invalid closers ), ], }
          (is-delimiter? ch)
          (throw (ex-info (str "Invalid closer '" ch "' in CLJP - use POP instead")
                          {:code :tokenize
                           :pos i
                           :char ch
                           :msg "CLJP only allows POP to close containers"}))

          ;; Regular word (includes PUSH-(, PUSH-[, PUSH-{, POP, and atoms)
          :else
          (if-let [[tok next-i] (read-word source i)]
            (recur next-i (conj tokens tok))
            (recur (inc i) tokens)))))))

(defn format-token
  "Format a token for pretty-printing (used in error messages)."
  [[tag value]]
  (case tag
    :push (str "PUSH-" value)
    :pop "POP"
    :atom value))

(defn tokens->source
  "Convert tokens back to CLJP source (for round-trip testing)."
  [tokens]
  (str/join " " (map format-token tokens)))
