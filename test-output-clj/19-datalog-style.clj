(ns examples.datalog)

(def db
  {:people [{:id 1 :name "Alice" :manager-id nil}
            {:id 2 :name "Bob" :manager-id 1}
            {:id 3 :name "Carol" :manager-id 2}]
   :projects [{:id 101 :name "Project A" :owner-id 2}
              {:id 102 :name "Project B" :owner-id 3}]})

(defn find-by [table pred]
  (filter pred (table db)))

(defn join [left-seq right-seq left-key right-key]
  (for [left left-seq
        right right-seq
        :when (= (left-key left) (right-key right))]
    (merge left {:joined right})))

(defn query-projects-with-managers []
  (let [people (:people db)
        projects (:projects db)
        with-owners (join projects people :owner-id :id)]
    (map
      (fn [p]
        {:project-name (:name p)
         :owner-name (get-in p [:joined :name])})
      with-owners)))
