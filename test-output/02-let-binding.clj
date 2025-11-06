(ns examples.let-demo)
(defn process-data [x y] (let [sum (+ x y) product (* x y) result {:product product, :sum sum}] (println "Result:" result) result))