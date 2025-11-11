(ns examples.program23)
(defn tree-map [f tree] (if (nil? tree) nil {:value (f (:value tree)), :right (tree-map f (:right tree)), :left (tree-map f (:left tree))}))
(defn tree-reduce [f acc tree] (if (nil? tree) acc (let [acc1 (tree-reduce f acc (:left tree)) acc2 (f acc1 (:value tree))] (tree-reduce f acc2 (:right tree)))))