(ns examples.program8)
(defn process-user [{:as user, :keys [name age]}] {:age age, :name name, :user user})
(defn process-items [[first second & rest]] {:second second, :first first, :rest rest})
(defn nested-extract [{{:keys [street city]} :address}] {:city city, :street street})