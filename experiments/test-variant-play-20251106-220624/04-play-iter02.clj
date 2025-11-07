(ns examples.program4)
(def users [{:role "admin", :age 25, :name "Alice"} {:role "user", :age 17, :name "Bob"} {:role "moderator", :age 30, :name "Charlie"} {:role "user", :age 16, :name "Diana"} {:role "user", :age 22, :name "Eve"}])
(defn active-users [users] (filter (fn [user] (> (:age user) 18)) users))
(defn user-names [users] (map :name users))