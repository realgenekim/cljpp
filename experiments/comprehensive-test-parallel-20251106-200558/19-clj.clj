(ns examples.program19)

(def db
  {:people [{:id 1 :name "Alice" :role "manager"}
            {:id 2 :name "Bob" :role "developer"}
            {:id 3 :name "Charlie" :role "manager"}
            {:id 4 :name "Diana" :role "developer"}]
   :projects [{:id 101 :name "Project X" :manager-id 1}
              {:id 102 :name "Project Y" :manager-id 3}
              {:id 103 :name "Project Z" :manager-id 1}]})

(defn find-by
  [coll pred]
  (filter pred coll))

(defn join
  [left right left-key right-key]
  (for [l left
        r right
        :when (= (get l left-key) (get r right-key))]
    (merge l r)))

(defn query-projects-with-managers
  [db]
  (let [managers (find-by (:people db) #(= (:role %) "manager"))
        projects (:projects db)]
    (map (fn [result]
           {:project-name (:name result)
            :manager-name (get result :name)
            :project-id (:id result)})
         (join projects managers :manager-id :id))))

(defn -main
  []
  (println "All managers:")
  (doseq [m (find-by (:people db) #(= (:role %) "manager"))]
    (println "  " (:name m)))
  (println "\nProjects with their managers:")
  (doseq [result (query-projects-with-managers db)]
    (println "  " (:project-name result) "managed by" (:manager-name result))))
