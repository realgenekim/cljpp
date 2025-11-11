(ns examples.program8)
(defn process-user [{:as user, :keys [name age]}] {:original user, :info (str "Age: " age), :greeting (str "Hello, " name)})
(defn process-items [[first second & rest]] {:second second, :total (+ 2 (count rest)), :first first, :rest rest})
(defn nested-extract [{{:keys [city state]} :address}] (str city ", " state))