(ns examples.program8)
(defn process-user [{:as user, :keys [name age]}] {:full-data user, :age age, :name name})
(defn process-items [[first second & rest]] {:second second, :first first, :rest rest})
(defn nested-extract [{:keys [address]}] (get-in address [:city]))