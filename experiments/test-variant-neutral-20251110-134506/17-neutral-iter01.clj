(ns examples.program17)
(defn lazy-range [start end] (letfn [(helper [n] (when (< n end) (lazy-seq (cons n (helper (inc n))))))] (helper start)))
(defn lazy-fibonacci ([] (lazy-fibonacci 0 1)) ([a b] (lazy-seq (cons a (lazy-fibonacci b (+ a b))))))
(defn take-while-sum [pred coll] (letfn [(helper [s xs] (lazy-seq (when-let [[x (first xs)]] (let [new-sum (+ s x)] (when (pred new-sum) (cons x (helper new-sum (rest xs))))))))] (helper 0 coll)))