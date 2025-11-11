(ns examples.program8)
(defn process-user [{:as user, :keys [name age]}] {:age age, :name name, :full-user user})
(defn process-items [[first second & rest]] {:second second, :first first, :rest rest})
(defn nested-extract [{:keys [{:keys [{:as address, :keys [street city]}]} :as nested]} {:address address, :nested nested, :city city, :street street}])