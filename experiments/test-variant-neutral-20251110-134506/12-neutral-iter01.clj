(ns examples.program12)
(def custom-transducer (comp (filter even?) (map (fn [x] (* 2 x)))))
(defn stateful-transducer [] (let [seen (atom {})] (filter (fn [x] (let [already-seen? (contains? (deref seen) x)] (when-not already-seen? (swap! seen conj x)) (not already-seen?))))))
(defn process-with-transducers [coll] (let [xf (comp (filter odd?) (map inc) (stateful-transducer))] (into [] xf coll)))