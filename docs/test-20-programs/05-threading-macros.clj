(ns examples.threading)
(defn process-order [order] (-> order (assoc :status :processing) (update :items count) (assoc :timestamp (System/currentTimeMillis))))
(defn calculate-total [items] (->> items (filter :in-stock) (map :price) (reduce + 0)))