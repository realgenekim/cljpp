(ns examples.state-machine)

(def transitions
  {:idle {:start :running :stop :idle}
   :running {:pause :paused :stop :idle}
   :paused {:resume :running :stop :idle}})

(defn apply-event [state event]
  (get-in transitions [state event] state))

(defn run-machine [events]
  (reduce
    (fn [state event]
      (let [next-state (apply-event state event)]
        (if next-state
          (do
            (println "Transition:" state "->" event "->" next-state)
            next-state)
          (do
            (println "Invalid transition:" state event)
            state))))
    :idle
    events))
