(ns examples.program17)
(defn lazy-range [start end] (letfn [([helper [n] (if (< n end) (lazy-seq (cons n (helper (inc n)))) nil)])] (helper start)))
(defn lazy-fibonacci ([] (lazy-fibonacci 0 1)) ([a b] (lazy-seq (cons a (lazy-fibonacci b (+ a b))))))
(defn take-while-sum [pred coll] (letfn [([helper [s xs] (when (seq xs) (let [new-sum (+ s (first xs))] (if (pred new-sum) (lazy-seq (cons (first xs) (helper new-sum (rest xs)))) nil)))])] (helper 0 coll)))