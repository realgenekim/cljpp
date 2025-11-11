(ns examples.program22)
(defn fibonacci [n] (if (< n 2) n (+ (fibonacci (- n 1)) (fibonacci (- n 2)))))
(def fibonacci-memo (memoize fibonacci))
(defn custom-memo [f] (let [cache (atom {})] (fn [& args] (if-let [cached (get @cache args)] cached (let [result (apply f args)] (swap! cache assoc args result) result)))))