(ns examples.program8)
(defn process-user [{:as user, :keys [name age]}] {:full-user user, :info (str "Age: " age), :greeting (str "Hello, " name)})
(defn process-items [[first second & rest]] {:second second, :remaining rest, :count (+ 2 (count rest)), :first first})
(defn nested-extract [{:as nested, :keys [{:as address, :keys [city state]}]}] {:address address, :full nested, :location (str city ", " state)})