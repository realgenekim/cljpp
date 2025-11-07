(ns examples.program5)
(defn process-data [data] (-> data (update :count inc) (update :items count) (assoc :processed true)))
(defn transform-coll [coll] (->> coll (filter odd?) (map (partial * 2)) (reduce +)))