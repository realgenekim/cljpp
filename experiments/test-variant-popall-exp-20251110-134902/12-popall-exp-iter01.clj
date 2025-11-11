(ns examples.program12)
(def custom-transducer (comp (filter even?) (map (fn [x] (* 2 x)))))
(defn stateful-transducer [] (fn [rf] (let [seen (atom PUSH-# {})] (fn ([] (rf)) ([result] result) ([result input] (if (contains? (deref seen) input) result (do (swap! seen conj input) (rf result input))))))))
(defn process-with-transducers [coll] (into [] coll (comp (filter (fn [x] (> x 5)) (map (fn [x] (* x 3)) (stateful-transducer))))))