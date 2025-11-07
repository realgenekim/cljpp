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
  (:require [clojure.string :as str]
            [clojure.pprint]))

(defn- open-container [opener]
  "Create an empty container for the given opener.
  Maps are represented as vectors during assembly."
  (case opener
    "(" (list)
    "[" []
    "{" [] ; Maps accumulate as vectors, converted on close
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
  All containers (lists, vectors, maps) are vectors during assembly."
  [coll x]
  (cond
    (or (list? coll) (seq? coll)) (apply list (concat coll (list x)))
    (vector? coll) (conj coll x)
    :else (throw (ex-info "Unknown container type"
                          {:code :internal :type (type coll)}))))

(defn- finalize-container!
  "Finalize a container based on its type.
  Maps need conversion from vector and arity validation."
  [type coll]
  (case type
    :list (apply list coll)
    :vec coll
    :map (let [kvs coll]
           (when (odd? (count kvs))
             (throw (ex-info "Map has odd arity"
                             {:code :map-odd-arity
                              :kvs kvs
                              :count (count kvs)})))
           (apply hash-map kvs))))

(defn assemble
  "Assemble tokens into Clojure forms.

  Input: Vector of tokens from tokenizer
  Output: Either:
    {:ok? true :forms [...] :source \"...\"}
    {:ok? false :error {:code ... :msg ... :pos ...}}"
  [tokens]
  (try
    (loop [ts tokens
           stack []     ; Stack of {:type :list/:vec/:map :opener \"(\" :line N :col N :coll ...}
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
                   :stack-info (mapv #(select-keys % [:type :opener :line]) stack)}})

        ;; Process next token
        (let [token (first ts)
              [tag & rest-token] token]
          (case tag
            :push
            (let [[opener line col] rest-token
                  type (container-type opener)
                  coll (open-container opener)]
              (recur (rest ts)
                     (conj stack {:type type :opener opener :line line :col col :coll coll})
                     forms))

            :pop
            (let [[line col] rest-token]
              (if (empty? stack)
                {:ok? false
                 :error {:code :underflow
                         :msg "POP with empty stack"
                         :line line
                         :col col}}
                (let [{:keys [type coll]} (peek stack)]
                  ;; Finalize the container
                  (let [coll' (finalize-container! type coll)]
                    (if (= 1 (count stack))
                      ;; Closing a top-level form
                      (recur (rest ts) [] (conj forms coll'))
                      ;; Attach to parent
                      (let [parent (peek (pop stack))
                            parent' (update parent :coll emit-into coll')]
                        (recur (rest ts) (conj (pop (pop stack)) parent') forms)))))))

            :pop-line
            (let [[line col] rest-token
                  ;; Find all containers opened on this line
                  line-containers (filterv #(= (:line %) line) stack)]
              (if (empty? line-containers)
                {:ok? false
                 :error {:code :pop-line-no-containers
                         :msg (str "POP-LINE with no containers opened on line " line)
                         :line line
                         :col col
                         :stack-state (mapv #(select-keys % [:type :line]) stack)}}
                ;; Close all containers from this line (in reverse order)
                (let [[final-stack final-forms]
                      (loop [remaining-pops (count line-containers)
                             current-stack stack
                             current-forms forms]
                        (if (zero? remaining-pops)
                          [current-stack current-forms]
                          (let [{:keys [type coll]} (peek current-stack)
                                coll' (finalize-container! type coll)]
                            (if (= 1 (count current-stack))
                              ;; Closing a top-level form
                              (recur (dec remaining-pops) [] (conj current-forms coll'))
                              ;; Attach to parent
                              (let [parent (peek (pop current-stack))
                                    parent' (update parent :coll emit-into coll')]
                                (recur (dec remaining-pops) (conj (pop (pop current-stack)) parent') current-forms))))))]
                  (recur (rest ts) final-stack final-forms))))

            :pop-all
            (let [[line col] rest-token]
              (if (empty? stack)
                {:ok? false
                 :error {:code :pop-all-empty-stack
                         :msg "POP-ALL with empty stack"
                         :line line
                         :col col}}
                ;; Close ALL containers in the stack
                (let [[final-stack final-forms]
                      (loop [current-stack stack
                             current-forms forms]
                        (if (empty? current-stack)
                          [current-stack current-forms]
                          (let [{:keys [type coll]} (peek current-stack)
                                coll' (finalize-container! type coll)]
                            (if (= 1 (count current-stack))
                              ;; Closing a top-level form
                              (recur [] (conj current-forms coll'))
                              ;; Attach to parent
                              (let [parent (peek (pop current-stack))
                                    parent' (update parent :coll emit-into coll')]
                                (recur (conj (pop (pop current-stack)) parent') current-forms))))))]
                  (recur (rest ts) final-stack final-forms))))

            :atom
            (let [[value line col] rest-token]
              (if (empty? stack)
                {:ok? false
                 :error {:code :no-container
                         :msg "Atom at top-level without container (v1 disallows loose atoms)"
                         :atom value
                         :line line
                         :col col}}
                (let [parsed (parse-atom value)
                      top (peek stack)
                      top' (update top :coll emit-into parsed)]
                  (recur (rest ts) (conj (pop stack) top') forms))))

            ;; Unknown token type
            {:ok? false
             :error {:code :tokenize
                     :msg (str "Unknown token type: " tag)
                     :token token}}))))
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
