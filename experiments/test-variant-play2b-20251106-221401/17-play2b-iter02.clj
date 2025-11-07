(ns examples.program17)
(defn lazy-range [start end] (letfn [(step [n] (when (< n end) (lazy-seq (cons n (step (inc n))))))] (step start)))
(defn lazy-fibonacci ([] (lazy-fibonacci 0 1)) ([a b] (lazy-seq (cons a (lazy-fibonacci b (+ a b))))))
(defn take-while-sum [pred coll] (letfn [(helper [xs acc] (if (empty? xs) acc (let [x (first xs) new-sum (+ acc x)] (if (pred new-sum) (lazy-seq (cons x (helper (rest xs) new-sum))) nil))))] (helper coll 0)))