(ns examples.program35 (:require [clojure.test.check :as tc] [clojure.test.check.generators :as gen] [clojure.test.check.properties :as prop]))
(def user-gen (gen/hash-map :id (gen/large-integer* {:min 1}) :name (gen/not-empty gen/string-alphanumeric) :email (gen/fmap (fn [s] (str s "@example.com")) gen/string-alphanumeric) :age (gen/large-integer* {:min 0, :max 120})))
(def user-invariants (prop/for-all [user user-gen] (and (pos? (:id user)) (seq (:name user)) (clojure.string/includes? (:email user) "@") (<= 0 (:age user) 120))))
(defn run-tests [] (tc/quick-check 100 user-invariants))