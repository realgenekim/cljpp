(ns examples.program11
  (:require [clojure.core.async :as async :refer [go go-loop chan <! >!]]))

(defn pipeline [input-ch]
  (let [step1-ch (chan)
        step2-ch (chan)]
    (go-loop []
      (when-some [v (<! input-ch)]
        (>! step1-ch (* v 2))
        (recur)))
    (go-loop []
      (when-some [v (<! step1-ch)]
        (>! step2-ch (+ v 10))
        (recur)))
    step2-ch))
