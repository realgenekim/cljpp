(ns examples.program9)
(def transitions {:paused {:stop :idle, :resume :running}, :idle {:start :running, :stop :idle}, :running {:stop :idle, :pause :paused}})
(defn apply-event [state event] (get-in transitions [state event] state))
(defn run-machine [initial-state events] (reduce apply-event initial-state events))