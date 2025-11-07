(ns examples.lazy)
(defn lazy-fib ([] (lazy-fib 0 1)) ([a b] (lazy-seq (cons a (lazy-fib b (+ a b))))))
(defn lazy-primes [] (letfn [(sieve [s] (lazy-seq (let [p (first s)] (cons p (sieve (filter (fn [x] (not= 0 (mod x p))) (rest s)))))))] (sieve (iterate inc 2))))
(defn lazy-tree-walk [tree] (letfn [(walk [node] (lazy-seq (when node (concat [(:value node)] (walk (:left node)) (walk (:right node))))))] (walk tree)))