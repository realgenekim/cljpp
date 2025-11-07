(ns examples.program8)
(defn process-user [{:as user, :keys [name age]}] {:original user, :info (str "Age: " age), :greeting (str "Hello, " name)})
(defn process-items [[first second & rest]] {:second second, :count (+ 2 (count rest)), :first first, :rest rest})
(defn nested-extract [{:keys [address]}] (let [{:keys [street city state]} address] {:full (str street ", " city ", " state), :city city, :state state, :street street}))