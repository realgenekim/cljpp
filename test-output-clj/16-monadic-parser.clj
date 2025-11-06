(ns examples.parser)

(defn parse-char [c]
  (fn [input]
    (if (and (seq input) (= (first input) c))
      [{:value c :rest (rest input)}]
      [])))

(defn bind [parser f]
  (fn [input]
    (mapcat
      (fn [result]
        ((f (:value result)) (:rest result)))
      (parser input))))

(defn return [v]
  (fn [input]
    [{:value v :rest input}]))

(defn choice [p1 p2]
  (fn [input]
    (concat (p1 input) (p2 input))))

(defn many [parser]
  (fn [input]
    (let [results (parser input)]
      (if (empty? results)
        [{:value [] :rest input}]
        (mapcat
          (fn [result]
            ((bind
               (many parser)
               (fn [rest-vals]
                 (return (cons (:value result) rest-vals))))
             (:rest result)))
          results)))))
