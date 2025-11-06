(ns cljp.assembler
  "CLJP assembler: converts token stream into Clojure s-expressions.

  Takes a vector of tokens (from tokenizer) and assembles them into:
  - A vector of top-level forms (success case)
  - An error map (failure case)

  Error codes:
    :underflow - POP with empty stack
    :unclosed - EOF with non-empty stack
    :map-odd-arity - Closing a map with odd number of elements
    :no-container - Atom at top-level without open container"
  (:require [clojure.string :as str]))

(defn- open-container [opener]
  "Create an empty container for the given opener."
  (case opener
    "(" (list)
    "[" []
    "{" {}
    (throw (ex-info "Unknown opener" {:code :internal :opener opener}))))

(defn- container-type [opener]
  "Return the type keyword for an opener."
  (case opener
    "(" :list
    "[" :vec
    "{" :map))

(defn- closer-for-type [type]
  "Return the appropriate closing delimiter for a container type."
  (case type
    :list ")"
    :vec "]"
    :map "}"))

(defn- parse-atom
  "Parse an atom string into a Clojure value.
  Handles strings, keywords, numbers, booleans, nil, and symbols."
  [atom-str]
  (cond
    ;; nil, true, false
    (= atom-str "nil") nil
    (= atom-str "true") true
    (= atom-str "false") false

    ;; String literal (already quoted)
    (and (str/starts-with? atom-str "\"")
         (str/ends-with? atom-str "\""))
    (try
      (read-string atom-str)
      (catch Exception _
        (symbol atom-str))) ; If read fails, treat as symbol

    ;; Keyword
    (str/starts-with? atom-str ":")
    (keyword (subs atom-str 1))

    ;; Number (simple heuristic)
    (re-matches #"-?\d+(\.\d+)?([eE][+-]?\d+)?" atom-str)
    (try
      (read-string atom-str)
      (catch Exception _
        (symbol atom-str)))

    ;; Default: symbol
    :else
    (symbol atom-str)))

(defn- emit-into
  "Add an element to a container.
  Lists: append to end
  Vectors: conj
  Maps: accumulate as flat sequence, will be validated at close time"
  [coll x]
  (cond
    (list? coll) (concat coll (list x))
    (vector? coll) (conj coll x)
    (map? coll) (let [kvs (vec (mapcat identity coll))
                      kvs' (conj kvs x)]
                  (into {} (partition-all 2 kvs')))
    :else (throw (ex-info "Unknown container type"
                          {:code :internal :type (type coll)}))))

(defn- finalize-map!
  "Validate and finalize a map. Throws if odd arity."
  [m]
  (let [kvs (vec (mapcat identity m))]
    (when (odd? (count kvs))
      (throw (ex-info "Map has odd arity"
                      {:code :map-odd-arity
                       :kvs kvs
                       :count (count kvs)})))
    m))

(defn assemble
  "Assemble tokens into Clojure forms.

  Input: Vector of tokens from tokenizer
  Output: Either:
    {:ok? true :forms [...] :source \"...\"}
    {:ok? false :error {:code ... :msg ... :pos ...}}"
  [tokens]
  (try
    (loop [ts tokens
           stack []     ; Stack of {:type :list/:vec/:map :opener \"(\" :coll ...}
           forms []]    ; Completed top-level forms
      (if (empty? ts)
        ;; EOF - check for unclosed forms
        (if (empty? stack)
          {:ok? true
           :forms forms
           :source (str/join "\n" (map pr-str forms))}
          {:ok? false
           :error {:code :unclosed
                   :msg (str "Unclosed forms at EOF (depth: " (count stack) ")")
                   :depth (count stack)
                   :stack-info (mapv #(select-keys % [:type :opener]) stack)}})

        ;; Process next token
        (let [[tag value] (first ts)]
          (case tag
            :push
            (let [type (container-type value)
                  coll (open-container value)]
              (recur (rest ts)
                     (conj stack {:type type :opener value :coll coll})
                     forms))

            :pop
            (if (empty? stack)
              {:ok? false
               :error {:code :underflow
                       :msg "POP with empty stack"
                       :pos (count tokens)}}
              (let [{:keys [type coll]} (peek stack)]
                ;; Finalize the container
                (let [coll' (if (= type :map) (finalize-map! coll) coll)]
                  (if (= 1 (count stack))
                    ;; Closing a top-level form
                    (recur (rest ts) [] (conj forms coll'))
                    ;; Attach to parent
                    (let [parent (peek (pop stack))
                          parent' (update parent :coll emit-into coll')]
                      (recur (rest ts) (conj (pop (pop stack)) parent') forms))))))

            :atom
            (if (empty? stack)
              {:ok? false
               :error {:code :no-container
                       :msg "Atom at top-level without container (v1 disallows loose atoms)"
                       :atom value}}
              (let [parsed (parse-atom value)
                    top (peek stack)
                    top' (update top :coll emit-into parsed)]
                (recur (rest ts) (conj (pop stack) top') forms)))

            ;; Unknown token type
            {:ok? false
             :error {:code :tokenize
                     :msg (str "Unknown token type: " tag)
                     :token [tag value]}}))))
    (catch clojure.lang.ExceptionInfo e
      {:ok? false
       :error (merge {:msg (.getMessage e)} (ex-data e))})))

(defn pretty-print
  "Pretty-print a form with proper indentation."
  [form]
  (binding [*print-length* nil
            *print-level* nil]
    (with-out-str
      (clojure.pprint/pprint form))))

(defn assemble-to-string
  "Assemble tokens and return formatted Clojure source code."
  [tokens]
  (let [result (assemble tokens)]
    (if (:ok? result)
      {:ok? true
       :source (str/join "\n" (map pretty-print (:forms result)))}
      result)))
