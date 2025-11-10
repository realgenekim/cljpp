(ns examples.program22)
(def fibonacci-memo (memoize (fn [n] (cond (= n 0) 0 (= n 1) 1 :else (+ (fibonacci-memo (- n 1)) (fibonacci-memo (- n 2)))))))
(defn custom-memo [f] (let [cache (atom {})] (fn [& args] (if-let [cached (get @cache args)] cached (let [result (apply f args)] (swap! cache assoc args result) result)))))