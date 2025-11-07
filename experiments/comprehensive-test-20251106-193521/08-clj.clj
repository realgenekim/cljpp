(ns examples.program8)

(defn process-user
  [{:keys [name age] :as user}]
  {:processed-name name
   :processed-age age
   :original user})

(defn process-items
  [[first second & rest]]
  {:first first
   :second second
   :rest rest
   :rest-count (count rest)})

(defn nested-extract
  [{{:keys [street city state]} :address :as person}]
  {:street street
   :city city
   :state state
   :full-person person})
