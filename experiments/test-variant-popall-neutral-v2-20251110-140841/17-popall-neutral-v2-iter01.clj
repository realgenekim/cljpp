(ns examples.program17)
(defn lazy-range [start end] (letfn [(step [n] (when (< n end) (lazy-seq (cons n (step (inc n))))))] (step start)))
(defn lazy-fibonacci [] (lazy-fibonacci 0 1) [a b] (lazy-seq (cons a (lazy-fibonacci b (+ a b)))))
(defn take-while-sum [pred coll] (letfn [(helper [remaining sum] (if (and (seq remaining) (pred (+ sum (first remaining)))) (lazy-seq (cons (first remaining) (helper (rest remaining) (+ sum (first remaining))))) []))] (helper coll 0)))