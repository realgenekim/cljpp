(ns examples.fsm)
(def transitions {:paused {:stop :stopped, :resume :running}, :idle {:start :running, :stop :idle}, :running {:stop :stopped, :pause :paused}, :stopped {:reset :idle}})
(defn transition [state event] (get-in transitions [state event]))
(defn process-events [initial-state events] (reduce (fn [state event] (let [next-state (transition state event)] (if next-state (do (println "Transition:" state "->" event "->" next-state) next-state) (do (println "Invalid transition:" state event) state)))) initial-state events))