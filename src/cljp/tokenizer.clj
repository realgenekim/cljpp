(ns cljp.tokenizer
  "CLJP tokenizer: converts CLJP source text into a token stream.

  Token types:
    [:push opener]  where opener is one of \"(\" \"[\" \"{\"
    [:pop]
    [:atom value]   where value is a string representation of the atom

  The tokenizer is intentionally simple and delegates most validation to the assembler."
  (:require [clojure.string :as str]))

(defn- is-delimiter? [c]
  (some #{c} [\( \[ \{ \) \]  \}]))

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
  "Read a word (symbol/keyword/number/boolean/nil) from source starting at index i.
  Returns [token next-index] or nil if nothing to read."
  [^String s i]
  (let [sb (StringBuilder.)]
    (loop [j i]
      (if (or (>= j (count s))
              (whitespace? (.charAt s j))
              (is-delimiter? (.charAt s j)))
        (let [tok (.toString sb)]
          (when-not (empty? tok)
            [[:atom tok] j]))
        (do (.append sb (.charAt s j))
            (recur (inc j)))))))

(defn- skip-comment
  "Skip a comment line starting at index i. Returns index after newline."
  [^String s i]
  (loop [j i]
    (if (or (>= j (count s)) (= \newline (.charAt s j)))
      (inc (min j (dec (count s))))
      (recur (inc j)))))

(defn- check-invalid-closer [ch pos]
  "Check if character is an invalid closer (] or }) and throw if so."
  (when (some #{ch} [\] \}])
    (throw (ex-info (str "Invalid closer '" ch "' in CLJP - use POP instead")
                    {:code :tokenize
                     :pos pos
                     :char ch
                     :msg "CLJP only allows ')' as a closer; use 'POP' to close all container types"}))))

(defn tokenize
  "Tokenize CLJP source string into a vector of tokens.

  Returns vector of tokens where each token is one of:
    [:push \"(\"]  or [:push \"[\"]  or [:push \"{\"]
    [:pop]
    [:atom \"value\"]

  Throws ex-info with {:code :tokenize ...} on lexical errors."
  [^String source]
  (let [n (count source)]
    (loop [i 0 tokens [] word-buffer nil]
      (if (>= i n)
        (if word-buffer
          (conj tokens [:atom word-buffer])
          tokens)
        (let [ch (.charAt source i)]
          (cond
            ;; Whitespace - flush word buffer and skip
            (whitespace? ch)
            (if word-buffer
              (recur (inc i) (conj tokens [:atom word-buffer]) nil)
              (recur (inc i) tokens nil))

            ;; Comment - flush word buffer and skip line
            (= ch \;)
            (let [tokens' (if word-buffer (conj tokens [:atom word-buffer]) tokens)]
              (recur (skip-comment source i) tokens' nil))

            ;; String literal
            (= ch \")
            (let [tokens' (if word-buffer (conj tokens [:atom word-buffer]) tokens)
                  [tok next-i] (read-string-literal source i)]
              (recur next-i (conj tokens' tok) nil))

            ;; Open delimiters - check if preceded by PUSH or if it's standalone
            (some #{ch} [\( \[ \{])
            (if word-buffer
              ;; Check if word-buffer is "PUSH"
              (if (= word-buffer "PUSH")
                (recur (inc i) (conj tokens [:push (str ch)]) nil)
                (throw (ex-info (str "Open delimiter '" ch "' must be preceded by PUSH keyword")
                                {:code :tokenize
                                 :pos i
                                 :char ch
                                 :preceding-word word-buffer})))
              (throw (ex-info (str "Open delimiter '" ch "' must be preceded by PUSH keyword")
                              {:code :tokenize
                               :pos i
                               :char ch})))

            ;; Close paren - check if preceded by POP or if it's standalone
            (= ch \))
            (if word-buffer
              (if (= word-buffer "POP")
                (recur (inc i) (conj tokens [:pop]) nil)
                (throw (ex-info "Close paren ')' must be preceded by POP keyword"
                                {:code :tokenize
                                 :pos i
                                 :preceding-word word-buffer})))
              (throw (ex-info "Close paren ')' must be preceded by POP keyword"
                              {:code :tokenize
                               :pos i})))

            ;; Invalid closers (] and })
            (some #{ch} [\] \}])
            (do
              (check-invalid-closer ch i)
              (recur (inc i) tokens word-buffer)) ; Never reaches here

            ;; Regular character - accumulate into word buffer
            :else
            (recur (inc i) tokens (str (or word-buffer "") ch))))))))

(defn format-token
  "Format a token for pretty-printing (used in error messages)."
  [[tag value]]
  (case tag
    :push (str "PUSH " value)
    :pop "POP"
    :atom value))

(defn tokens->source
  "Convert tokens back to CLJP source (for round-trip testing)."
  [tokens]
  (str/join " " (map format-token tokens)))
