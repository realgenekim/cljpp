(ns examples.program5)
(defn process-data [data] (-> data (update :count inc) (assoc :items (count (:items data))) (assoc :processed true)))
(defn transform-coll [coll] (->> coll (filter (fn [x] (odd? x))) (map (fn [x] (* x 2))) (reduce +)))