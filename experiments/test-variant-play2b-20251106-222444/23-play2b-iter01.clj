(ns examples.program23)
(defn tree-map [f tree] (when tree (-> tree (assoc :value (f (:value tree))) (update :left (partial tree-map f)) (update :right (partial tree-map f)))))
(defn tree-reduce [f acc tree] (if tree (let [acc1 (f acc (:value tree)) acc2 (tree-reduce f acc1 (:left tree)) acc3 (tree-reduce f acc2 (:right tree))] acc3) acc))