(ns examples.collections)

(def users
  [{:name "Alice" :age 30 :role :admin}
   {:name "Bob" :age 25 :role :user}
   {:name "Carol" :age 35 :role :moderator}])

(defn active-users [users]
  (filter (fn [u] (> (:age u) 18)) users))

(defn user-names [users]
  (map :name users))
