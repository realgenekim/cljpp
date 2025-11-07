(ns examples.program4)
(def users [{:role "admin", :age 25, :name "Alice"} {:role "user", :age 17, :name "Bob"} {:role "user", :age 30, :name "Charlie"} {:role "guest", :age 16, :name "Diana"} {:role "moderator", :age 22, :name "Eve"}])
(defn active-users [users] (filter (fn [{:keys [age]}] (> age 18)) users))
(defn user-names [users] (map :name users))