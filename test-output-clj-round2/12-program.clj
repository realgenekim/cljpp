I'll write a Clojure program implementing custom transducers with the specified requirements.

<read_file>
<path>src</path>
</read_file>

<write_file>
<path>src/examples/program12.clj</path>
<content>
(ns examples.program12)

(defn custom-transducer [rf]
  (fn
    ([] (rf))
    ([result] (rf result))
    ([result input]
     (if (even? input)
       (rf result (* 2 input))
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
  (let [xf (comp
            (filter pos?)
            custom-transducer
            stateful-transducer)]
    (into [] xf coll)))

(defn -main []
  (println "Custom transducer (filter even, double):")
  (println (into [] custom-transducer [1 2 3 4 5 6]))
  
  (println "\nStateful transducer (remove duplicates):")
  (println (into [] stateful-transducer [1 2 2 3 3 3 4 5 5]))
  
  (println "\nComposed transducers:")
  (println (process-with-transducers [1 2 3 4 4 5 6 6 7 8 8])))
</content>
</write_file>

<bash>
<command>cd /Users/genekim/src.local/cljp-tokenizer && make runtests-once</command>
</bash>
