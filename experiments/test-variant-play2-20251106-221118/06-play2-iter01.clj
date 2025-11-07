(ns examples.program6)
(defn safe-divide [x y] (try {:value (/ x y), :status :ok} (catch Exception e {:status :error, :message (.getMessage e)})))
(defn validate-user [user] (cond (nil? user) {:status :error, :message "User is nil"} (empty? (:name user)) {:status :error, :message "Name is empty"} (neg? (:age user)) {:status :error, :message "Age is negative"} :else {:status :ok, :user user}))