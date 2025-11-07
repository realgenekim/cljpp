(ns examples.program17)
(defn lazy-range [start end] (letfn [(step [n] (when (< n end) (lazy-seq (cons n (step (inc n))))))] (step start)))
(defn lazy-fibonacci ([] (lazy-fibonacci 0 1)) ([a b] (lazy-seq (cons a (lazy-fibonacci b (+ a b))))))
(defn take-while-sum [max-sum coll] (letfn [(take-helper [remaining xs] (lazy-seq (when (and (seq xs) (pos? remaining)) (let [f (first xs)] (when (<= f remaining) (cons f (take-helper (- remaining f) (rest xs))))))))] (take-helper max-sum coll)))