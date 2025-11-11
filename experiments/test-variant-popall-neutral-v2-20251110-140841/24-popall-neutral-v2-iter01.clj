(ns examples.program24)
(defmacro when-let-all [bindings & body] (if (seq bindings) (let [[(first bindings) (second bindings)]] (if (first bindings) (when-let-all (into [] (drop 2 bindings)) & body) nil))))
(do & body)
(defmacro defn-timed [name args & body] (let [fname# (str (quote name))] `PUSH-( defn ~ name ~ args (let [start# (System/nanoTime)] (println (str "Starting " ~ fname#)) (let [result# (do ~@ body) end# (System/nanoTime) elapsed# (/ (- end# start#) 1000000.0)] (println (str "Finished " ~ fname# " in " elapsed# "ms")) result#))))