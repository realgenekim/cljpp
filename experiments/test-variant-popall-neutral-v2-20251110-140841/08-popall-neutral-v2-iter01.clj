(ns examples.program8)
(defn process-user [{:as user, :keys [name age]}] {:age age, :full user, :name name})
(defn process-items [[first second & rest]] {:second second, :first first, :rest rest})
(defn nested-extract [{:keys [{:keys [{:as address, :keys [city]}]} :as nested]} {:address address, :nested nested, :city city}])