(ns examples.errors)

(defn safe-divide [a b]
  (try
    (let [result (/ a b)]
      {:status :ok :value result})
    (catch Exception e
      {:status :error :message (ex-message e)})))

(defn validate-user [user]
  (cond
    (nil? user) {:valid false :reason "User is nil"}
    (empty? (:name user)) {:valid false :reason "Name is empty"}
    (< (:age user) 0) {:valid false :reason "Age is negative"}
    :else {:valid true}))
