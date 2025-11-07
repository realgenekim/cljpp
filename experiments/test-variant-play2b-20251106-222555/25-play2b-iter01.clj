(ns examples.program25 (:require [clojure.walk :refer [postwalk]]))
(defn update-in-multi [m paths-and-fns] (reduce (fn [acc [path f]] (update-in acc path f)) m paths-and-fns))
(defn transform-tree [pred f tree] (postwalk (fn [node] (if (and (map? node) (pred node)) (f node) node)) tree))