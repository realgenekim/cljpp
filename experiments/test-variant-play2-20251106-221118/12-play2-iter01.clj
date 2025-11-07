(ns examples.program12)
(defn custom-transducer [] (comp (filter even?) (map (fn [x] (* 2 x)))))
(defn stateful-transducer [] (fn [rf] (let [seen (atom {})] (fn ([] (rf)) ([result] result) ([result input] (if (contains? @seen input) result (do (swap! seen conj input) (rf result input))))))))
(defn process-with-transducers [coll] (let [xf1 (custom-transducer) xf2 (comp (filter (fn [x] (> x 5))) (stateful-transducer))] {:filtered (into [] xf1 coll), :composed (into [] xf2 coll)}))