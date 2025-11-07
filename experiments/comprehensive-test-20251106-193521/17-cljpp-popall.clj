(ns examples.program17)
(defn lazy-range [start end] (letfn [(step [n] (when (< n end) (lazy-seq (cons n (step (inc n))))))] (step start)))
(defn lazy-fibonacci ([] (lazy-fibonacci 0 1)) ([a b] (lazy-seq (cons a (lazy-fibonacci b (+ a b))))))
(defn take-while-sum [pred coll] (letfn [(helper [xs sum] (lazy-seq (when (seq xs) (let [x (first xs) new-sum (+ sum x)] (when (pred new-sum) (cons x (helper (rest xs) new-sum)))))))] (helper coll 0)))