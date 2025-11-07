(ns examples.program15)

(defn dfs [graph start]
  (loop [stack [start]
         visited #{}
         result []]
    (if (empty? stack)
      result
      (let [node (peek stack)
            stack' (pop stack)]
        (if (visited node)
          (recur stack' visited result)
          (recur (into stack' (reverse (get graph node [])))
                 (conj visited node)
                 (conj result node)))))))

(defn bfs [graph start]
  (loop [queue (conj clojure.lang.PersistentQueue/EMPTY start)
         visited #{}
         result []]
    (if (empty? queue)
      result
      (let [node (peek queue)
            queue' (pop queue)]
        (if (visited node)
          (recur queue' visited result)
          (recur (into queue' (get graph node []))
                 (conj visited node)
                 (conj result node)))))))
