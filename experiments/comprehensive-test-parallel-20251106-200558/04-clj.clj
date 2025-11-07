(ns examples.program4)

(def users
  [{:name "Alice" :age 25 :role "admin"}
   {:name "Bob" :age 17 :role "user"}
   {:name "Charlie" :age 30 :role "moderator"}
   {:name "Diana" :age 16 :role "user"}
   {:name "Eve" :age 22 :role "admin"}])

(defn active-users [users]
  (filter #(> (:age %) 18) users))

(defn user-names [users]
  (map :name users))

(comment
  (active-users users)
  ;; => ({:name "Alice", :age 25, :role "admin"}
  ;;     {:name "Charlie", :age 30, :role "moderator"}
  ;;     {:name "Eve", :age 22, :role "admin"})
  
  (user-names users)
  ;; => ("Alice" "Bob" "Charlie" "Diana" "Eve")
  
  (user-names (active-users users))
  ;; => ("Alice" "Charlie" "Eve")
  )
