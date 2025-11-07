(ns examples.program10)
(defn user-badge [{:keys [name verified? role]}] [:div.badge [:h3 name] (when verified? [:span.verified "✓"]) [:p.role role]])
(defn user-list [users] [:div.user-list (for [user users] [:div {:key (:name user)} (user-badge user)])])
(defn stats-panel [stats] (let [total (reduce + (vals stats))] [:div.stats [:h2 (str "Total: " total)] (for [[k v] stats] [:div {:key k} [:span (name k) ": " v]])]))