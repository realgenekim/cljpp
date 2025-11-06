(ns examples.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::name string?)
(s/def ::age (s/and int? #(> % 0)))
(s/def ::email (s/and string? #(re-matches #".+@.+\..+" %)))
(s/def ::user (s/keys :req-un [::name ::age ::email]))

(defn validate-user [user]
  (if (s/valid? ::user user)
    {:valid true :user user}
    {:valid false :errors (s/explain-data ::user user)}))
