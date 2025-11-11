(ns examples.program22)
(def fibonacci-memo (memoize (fn [n] (if (< n 2) n (+ (fibonacci-memo (- n 1)) (fibonacci-memo (- n 2)))))))
(defn custom-memo [f] (let [cache (atom {})] (fn [& args] (if-let [cached (get @cache args)] cached (let [result (apply f args)] (swap! cache assoc args result) result)))))