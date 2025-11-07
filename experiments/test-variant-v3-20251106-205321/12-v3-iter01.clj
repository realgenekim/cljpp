(ns examples.program12)
(defn custom-transducer [] (comp (filter even?) (map (fn [x] (* 2 x)))))
(defn stateful-transducer [] (fn [rf] (let [seen (atom {})] (fn ([] (rf)) ([result] result) ([result input] (if (contains? @seen input) result (do (swap! seen conj input) (rf result input))))))))
(defn process-with-transducers [coll] (let [xf (comp (filter odd?) (map inc) (stateful-transducer))] (into [] xf coll)))