(ns examples.program23)
(defn tree-map [f tree] (when tree {:value (f (:value tree)), :right (tree-map f (:right tree)), :left (tree-map f (:left tree))}))
(defn tree-reduce [f acc tree] (if tree (let [acc' (f acc (:value tree)) acc'' (tree-reduce f acc' (:left tree))] (tree-reduce f acc'' (:right tree))) acc))