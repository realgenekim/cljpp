(ns examples.program17)
(defn lazy-range [start end] (letfn [([helper [n] (when (< n end) (lazy-seq (cons n (helper (inc n)))))])] (helper start)))
(defn lazy-fibonacci ([] (lazy-fibonacci 0 1)) ([a b] (lazy-seq (cons a (lazy-fibonacci b (+ a b))))))
(defn take-while-sum [pred coll] (letfn [([helper [s xs] (when-let [[f (first xs)]] (let [new-sum (+ s f)] (when (pred new-sum) (lazy-seq (cons new-sum (helper new-sum (rest xs)))))))])] (helper 0 coll)))