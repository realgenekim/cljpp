(ns examples.program4)

(def users
  [{:name "Alice" :age 25 :role "engineer"}
   {:name "Bob" :age 17 :role "intern"}
   {:name "Carol" :age 30 :role "manager"}
   {:name "Dave" :age 16 :role "student"}
   {:name "Eve" :age 22 :role "designer"}])

(defn active-users [users]
  (filter #(> (:age %) 18) users))

(defn user-names [users]
  (map :name users))

(println "All users:" users)
(println "\nActive users (age > 18):" (active-users users))
(println "\nAll user names:" (user-names users))
(println "\nActive user names:" (user-names (active-users users)))
