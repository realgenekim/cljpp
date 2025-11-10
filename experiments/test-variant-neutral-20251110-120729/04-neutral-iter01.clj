(ns examples.program4)
(def users [{:role "admin", :age 25, :name "Alice"} {:role "user", :age 17, :name "Bob"} {:role "moderator", :age 30, :name "Carol"} {:role "user", :age 16, :name "Dave"}])
(defn active-users [users] (filter (fn [u] (> (:age u) 18)) users))
(defn user-names [users] (map :name users))