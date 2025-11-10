(ns examples.program25 (:require [clojure.walk :as walk]))
(defn update-in-multi [data paths-and-fns] (reduce (fn [acc [path f]] (update-in acc path f)) data paths-and-fns))
(defn transform-tree [pred f tree] (walk/postwalk (fn [node] (if (and (map? node) (pred node)) (f node) node)) tree))