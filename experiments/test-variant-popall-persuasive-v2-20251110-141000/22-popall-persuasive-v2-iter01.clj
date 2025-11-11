(ns examples.program22)
(defn fibonacci-naive [n] (if (<= n 1) n (+ (fibonacci-naive (- n 1)) (fibonacci-naive (- n 2)))))
(def fibonacci-memo (memoize fibonacci-naive))
(defn custom-memo [f] (let [cache (atom {})] (fn [& args] (if-let [cached (get @cache args)] cached (let [result (apply f args)] (swap! cache assoc args result) result)))))