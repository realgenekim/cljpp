(ns examples.threading)

(defn process-data [data]
  (-> data
      (update :count inc)
      (update :items count)
      (assoc :processed true)))

(defn transform-coll [items]
  (->> items
       (filter odd?)
       (map #(* % 2))
       (reduce +)))
