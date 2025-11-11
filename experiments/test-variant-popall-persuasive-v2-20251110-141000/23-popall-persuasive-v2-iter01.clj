(ns examples.program23)
(defn tree-map [f tree] (if (nil? tree) nil {:value (f (:value tree)), :right (tree-map f (:right tree)), :left (tree-map f (:left tree))}))
(defn tree-reduce [f acc tree] (if (nil? tree) acc (let [acc1 (f acc (:value tree)) acc2 (tree-reduce f acc1 (:left tree)) acc3 (tree-reduce f acc2 (:right tree))] acc3)))