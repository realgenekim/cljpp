(ns examples.async
  (:require [clojure.core.async :refer [go chan >! <! close!]]))

(defn pipeline [input-ch]
  (let [step1-ch (chan)
        step2-ch (chan)]
    (go
      (loop []
        (when-let [v (<! input-ch)]
          (>! step1-ch (* v 2))
          (recur)))
      (close! step1-ch))
    (go
      (loop []
        (when-let [v (<! step1-ch)]
          (>! step2-ch (+ v 10))
          (recur)))
      (close! step2-ch))
    step2-ch))
