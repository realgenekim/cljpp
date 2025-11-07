(ns examples.program12)
(defn custom-transducer [] (comp (filter even?) (map (fn [x] (* x 2)))))
(defn stateful-transducer [] (fn [rf] (let [seen (atom {})] (fn ([] (rf)) ([result] (rf result)) ([result input] (if (contains? (deref seen) input) result (do (swap! seen conj input) (rf result input))))))))
(defn process-with-transducers [coll] (let [xf1 (custom-transducer) xf2 (comp (stateful-transducer) (filter (fn [x] (> x 5)))) result1 (into [] xf1 coll) result2 (into [] xf2 coll)] {:custom result1, :stateful result2}))