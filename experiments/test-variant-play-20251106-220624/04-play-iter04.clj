(ns examples.program4)
(def users [{:role "developer", :age 25, :name "Alice"} {:role "intern", :age 17, :name "Bob"} {:role "manager", :age 30, :name "Charlie"} {:role "student", :age 16, :name "Diana"}])
(defn active-users [users] (filter (fn [user] (> (:age user) 18)) users))
(defn user-names [users] (map :name users))