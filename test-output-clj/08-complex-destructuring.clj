(ns examples.destructuring)

(defn process-user [{:keys [name age] :as user}]
  (str name " is " age " years old"))

(defn process-items [[first second & rest]]
  {:first first
   :second second
   :rest-count (count rest)})

(defn nested-extract [{{:keys [street city]} :address :keys [name]}]
  (str name " lives in " city " on " street))
