(ns examples.recursion)
(defn factorial [n] (if (<= n 1) 1 (* n (factorial (dec n)))))
(defn fibonacci [n] (cond (= n 0) 0 (= n 1) 1 :else (+ (fibonacci (- n 1)) (fibonacci (- n 2)))))