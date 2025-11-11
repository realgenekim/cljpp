(ns examples.program21)
(defn split-words [s] (clojure.string/split s (re-pattern " ")))
(defn reverse-words [s] (->> (split-words s) (map clojure.string/reverse) (clojure.string/join " ")))
(defn title-case [s] (->> (split-words s) (map (fn [word] (str (clojure.string/upper-case (subs word 0 1)) (subs word 1)))) (clojure.string/join " ")))