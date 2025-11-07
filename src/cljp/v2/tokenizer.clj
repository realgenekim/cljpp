(ns cljp.v2.tokenizer
  "CLJ-PP v2 tokenizer: converts CLJ-PP v2 source text into a token stream.

  Token types:
    [:push opener line col]  where opener is one of \"(\" \"[\" \"{\"
    [:close n line col]      where n is 1, 2, 3, etc. (X, X2, X3, ...)
    [:atom value line col]   where value is a string representation of the atom

  Syntax:
    LP - opens list (
    LV - opens vector [
    LM - opens map {
    X  - closes 1 level
    X2 - closes 2 levels
    X3 - closes 3 levels (X4, X5, etc. for deeper closes)
    All other words are atoms

  The tokenizer is intentionally simple and delegates most validation to the assembler."
  (:require [clojure.string :as str]))

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
  "Read a word (symbol/keyword/number/boolean/nil/LP/LV/LM/X/Xn) from source starting at index i.
  Returns [token next-index] or nil if nothing to read."
  [^String s i line col]
  (let [sb (StringBuilder.)]
    (loop [j i]
      (if (or (>= j (count s))
              (whitespace? (.charAt s j)))
        (let [tok (.toString sb)]
          (when-not (empty? tok)
            ;; Check for LP, LV, LM, X, X2, X3, etc.
            (cond
              (= tok "LP") [[:push "(" line col] j]
              (= tok "LV") [[:push "[" line col] j]
              (= tok "LM") [[:push "{" line col] j]
              (= tok "X") [[:close 1 line col] j]
              (re-matches #"X[2-9]" tok)
              (let [n (- (int (first (subs tok 1))) (int \0))]
                [[:close n line col] j])
              :else [[:atom tok line col] j])))
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
  "Tokenize CLJ-PP v2 source string into a vector of tokens.

  Returns vector of tokens where each token is one of:
    [:push \"(\" line col]  or [:push \"[\" line col]  or [:push \"{\" line col]
    [:close n line col]    where n is 1, 2, 3, etc.
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

          ;; Regular word (includes LP, LV, LM, X, X2-X9, and atoms)
          :else
          (if-let [[tok next-i] (read-word source i line col)]
            (recur next-i line (+ col (- next-i i)) (conj tokens tok))
            (recur (inc i) line (inc col) tokens)))))))

(defn format-token
  "Format a token for pretty-printing (used in error messages)."
  [token]
  (let [[tag value-or-line line-or-col col] token]
    (case tag
      :push (case value-or-line
              "(" "LP"
              "[" "LV"
              "{" "LM")
      :close (if (= value-or-line 1)
               "X"
               (str "X" value-or-line))
      :atom value-or-line)))

(defn tokens->source
  "Convert tokens back to CLJ-PP v2 source (for round-trip testing)."
  [tokens]
  (str/join " " (map format-token tokens)))
