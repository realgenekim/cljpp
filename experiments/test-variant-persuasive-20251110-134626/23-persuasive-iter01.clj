(ns examples.program23)
(defn tree-map [f tree] (when tree {:value (f (:value tree)), :right (tree-map f (:right tree)), :left (tree-map f (:left tree))}))
(defn tree-reduce [f acc tree] (if tree (let [acc1 (f acc (:value tree)) acc2 (tree-reduce f acc1 (:left tree))] (tree-reduce f acc2 (:right tree))) acc))