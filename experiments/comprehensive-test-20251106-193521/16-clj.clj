(ns examples.program16)

(defn parse-char [c]
  (fn [input]
    (when (and (seq input) (= c (first input)))
      [{:value c :rest (rest input)}])))

(defn bind [parser f]
  (fn [input]
    (when-let [results (parser input)]
      (mapcat (fn [result]
                (let [next-parser (f (:value result))]
                  (next-parser (:rest result))))
              results))))

(defn return [value]
  (fn [input]
    [{:value value :rest input}]))

(defn choice [& parsers]
  (fn [input]
    (seq (mapcat #(% input) parsers))))

(defn many [parser]
  (fn [input]
    (letfn [(parse-many [inp acc]
              (if-let [results (parser inp)]
                (let [result (first results)]
                  (parse-many (:rest result) (conj acc (:value result))))
                [{:value acc :rest inp}]))]
      (parse-many input []))))
