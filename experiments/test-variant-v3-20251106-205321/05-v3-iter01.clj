(ns examples.program5)
(defn process-data [data] (-> data (update :count inc) (assoc :items (count (:items data))) (assoc :processed true)))
(defn transform-coll [coll] (->> coll (filter odd?) (map (partial * 2)) (reduce +)))