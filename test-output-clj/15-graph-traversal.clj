(ns examples.graph)

(defn dfs [graph start]
  (loop [stack [start]
         visited #{}
         result []]
    (if (empty? stack)
      result
      (let [node (peek stack)
            stack' (pop stack)]
        (if (contains? visited node)
          (recur stack' visited result)
          (let [neighbors (get graph node [])
                unvisited (remove visited neighbors)]
            (recur
              (into stack' unvisited)
              (conj visited node)
              (conj result node))))))))

(defn bfs [graph start]
  (loop [queue [start]
         visited #{start}
         result []]
    (if (empty? queue)
      result
      (let [node (first queue)
            queue' (vec (rest queue))
            neighbors (get graph node [])
            unvisited (remove visited neighbors)]
        (recur
          (into queue' unvisited)
          (into visited unvisited)
          (conj result node))))))
