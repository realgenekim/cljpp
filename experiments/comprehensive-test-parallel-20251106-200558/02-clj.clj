(ns examples.program2)

(defn calculate [x y]
  (let [sum (+ x y)
        product (* x y)]
    {:sum sum
     :product product}))
