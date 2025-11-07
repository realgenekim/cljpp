(ns examples.program4)
(def users [{:role "Developer", :age 25, :name "Alice"} {:role "Intern", :age 17, :name "Bob"} {:role "Manager", :age 30, :name "Charlie"} {:role "Student", :age 16, :name "Diana"} {:role "Designer", :age 22, :name "Eve"}])
(defn active-users [users] (filter (fn [user] (> (:age user) 18)) users))
(defn user-names [users] (map :name users))