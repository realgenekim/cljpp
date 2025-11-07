(ns examples.program17)

(defn lazy-range [start end]
  (letfn [(range-seq [n]
            (when (< n end)
              (lazy-seq (cons n (range-seq (inc n))))))]
    (range-seq start)))

(defn lazy-fibonacci
  ([] (lazy-fibonacci 0 1))
  ([a b]
   (lazy-seq (cons a (lazy-fibonacci b (+ a b))))))

(defn take-while-sum [pred coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (let [sum (reduce + (take-while pred s))]
        (cons sum nil)))))
