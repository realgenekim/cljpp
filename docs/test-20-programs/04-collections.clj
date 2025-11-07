(ns examples.collections)
(def users [{:role :admin, :age 30, :name "Alice"} {:role :user, :age 25, :name "Bob"} {:role :user, :age 35, :name "Carol"}])
(defn find-admins [users] (filter (fn [u] (= :admin (:role u))) users))
(defn get-names [users] (map :name users))