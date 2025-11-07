(ns examples.program12)

(defn custom-transducer
  "Transducer that filters even numbers and doubles them"
  []
  (comp
    (filter even?)
    (map #(* 2 %))))

(defn stateful-transducer
  "Transducer that tracks seen values using an atom"
  []
  (fn [rf]
    (let [seen (atom #{})]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (if (contains? @seen input)
           result
           (do
             (swap! seen conj input)
             (rf result input))))))))

(defn process-with-transducers
  "Process a collection by composing filters"
  [coll]
  (into []
        (comp
          (custom-transducer)
          (stateful-transducer))
        coll))
