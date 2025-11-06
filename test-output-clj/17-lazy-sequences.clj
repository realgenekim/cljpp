(ns examples.lazy)

(defn lazy-range [n]
  (letfn [(step [i]
            (lazy-seq
              (when (< i n)
                (cons i (step (inc i))))))]
    (step 0)))

(defn lazy-fibonacci
  ([] (lazy-fibonacci 0 1))
  ([a b]
   (lazy-seq
     (cons a (lazy-fibonacci b (+ a b))))))

(defn take-while-sum [pred coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (let [f (first s)]
        (when (pred f)
          (cons f (take-while-sum pred (rest s))))))))
