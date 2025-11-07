(ns examples.let-binding)

(defn calculate [x y]
  (let [sum (+ x y)
        product (* x y)]
    {:sum sum
     :product product}))
