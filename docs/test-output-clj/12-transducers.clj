(ns examples.transducers)

(defn custom-transducer [rf]
  (fn
    ([] (rf))
    ([result] result)
    ([result input]
     (if (even? input)
       (rf result (* input 2))
       result))))

(defn stateful-transducer [rf]
  (let [seen (atom #{})]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (if (contains? @seen input)
         result
         (do
           (swap! seen conj input)
           (rf result input)))))))

(defn process-with-transducers [coll]
  (transduce
    (comp
      (filter pos?)
      (map inc)
      custom-transducer)
    conj
    []
    coll))
