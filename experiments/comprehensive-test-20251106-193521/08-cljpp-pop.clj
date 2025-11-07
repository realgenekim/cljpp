(ns examples.program8)
(defn process-user [{:as user, :keys [name age]}] (str "User: " name ", Age: " age ", Full: " user))
(defn process-items [[first second & rest]] {:second second, :first first, :rest rest})
(defn nested-extract [{:keys [address]}] (let [{:keys [city state]} address] (str city ", " state)))