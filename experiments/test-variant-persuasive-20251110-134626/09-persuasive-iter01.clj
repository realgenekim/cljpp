(ns examples.program9)
(def transitions {:paused {:stop :idle, :resume :running}, :idle {:start :running, :pause :idle}, :running {:stop :idle, :pause :paused}})
(defn apply-event [state event] (get-in transitions [state event] state))
(defn run-machine [events] (reduce apply-event :idle events))