(ns examples.program16)

(defn parse-char [c]
  (fn [input]
    (when (and (seq input) (= (first input) c))
      [(first input) (rest input)])))

(defn return [value]
  (fn [input]
    [value input]))

(defn bind [parser f]
  (fn [input]
    (when-let [[value remaining] (parser input)]
      ((f value) remaining))))

(defn choice [parser1 parser2]
  (fn [input]
    (or (parser1 input)
        (parser2 input))))

(defn many [parser]
  (fn [input]
    (loop [results []
           remaining input]
      (if-let [[value new-remaining] (parser remaining)]
        (recur (conj results value) new-remaining)
        [results remaining]))))
