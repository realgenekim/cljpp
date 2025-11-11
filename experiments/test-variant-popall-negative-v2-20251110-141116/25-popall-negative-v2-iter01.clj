(ns examples.program25 (:require [clojure.walk :refer [postwalk]]))
(defn update-in-multi [m paths f] (reduce (fn [acc path] (update-in acc path f)) m paths))
(defn transform-tree [tree pred f] (postwalk (fn [node] (if (and (map? node) (pred node)) (f node) node)) tree))