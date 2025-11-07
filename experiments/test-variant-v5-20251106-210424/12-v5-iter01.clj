(ns examples.program12)
(defn custom-transducer [] (comp (filter (fn [x] (even? x))) (map (fn [x] (* x 2)))))
(defn stateful-transducer [] (fn [rf] (let [seen (atom {})] (fn [] (rf) [result] (rf result) [result input] (if (contains? (deref seen) input) result (do (swap! seen conj input) (rf result input)))))))
(defn process-with-transducers [coll] (into [] (comp (filter (fn [x] (pos? x))) (stateful-transducer) (custom-transducer)) coll))