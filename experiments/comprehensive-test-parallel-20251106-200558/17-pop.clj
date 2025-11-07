(ns examples.program17)
(defn lazy-range [start end] (letfn [([step [current]] (when (< current end) (lazy-seq (cons current (step (inc current))))))] (step start)))
(defn lazy-fibonacci ([] (lazy-fibonacci 0 1)) ([a b] (lazy-seq (cons a (lazy-fibonacci b (+ a b))))))
(defn take-while-sum [pred coll] (letfn [([step [s xs]] (when (seq xs) (let [x (first xs) new-sum (+ s x)] (if (pred new-sum) (lazy-seq (cons x (step new-sum (rest xs)))) nil))))] (step 0 coll)))