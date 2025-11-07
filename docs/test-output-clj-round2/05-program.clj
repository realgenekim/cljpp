(ns examples.program5)
(defn process-data [data] (-> data (assoc :count (inc (:count data))) (assoc :items (count (:items data))) (assoc :processed true)))
(defn transform-coll [coll] (->> coll (filter odd?) (map (fn [x] (* x 2))) (reduce +)))