(ns examples.program12)
(def custom-transducer (comp (filter even?) (map (fn [x] (* 2 x)))))
(defn stateful-transducer [] (let [seen (atom {})] (fn [rf] (fn ([] (rf)) ([result] (rf result)) ([result input] (if (contains? (deref seen) input) result (do (swap! seen conj input) (rf result input))))))))
(defn process-with-transducers [coll] (transduce (comp custom-transducer (stateful-transducer)) conj [] coll))