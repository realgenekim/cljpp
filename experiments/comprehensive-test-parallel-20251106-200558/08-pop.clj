(ns examples.program8)
(defn process-user [{:as user, :keys [name age]}] (println (str "Name: " name ", Age: " age)) (println (str "Full user: " user)))
(defn process-items [[first second & rest]] (println (str "First: " first)) (println (str "Second: " second)) (println (str "Rest: " rest)))
(defn nested-extract [{{:as address, :keys [city street]} :address, :keys [name]}] (println (str "Name: " name)) (println (str "City: " city)) (println (str "Street: " street)) (println (str "Full address: " address)))