(ns examples.program10)
(defn user-badge [{:keys [name verified? role]}] [:div {:class "user-badge"} [:span.name name] (when verified? [:span.verified "✓"]) [:span.role role]])
(defn user-list [users] [:div {:class "user-list"} (for [user users] [:div {:key (:name user)} [user-badge user]])])
(defn stats-panel [stats] (let [total (reduce + (vals stats))] [:div {:class "stats-panel"} [:h3 (str "Total: " total)] (for [[k v] stats] [:div {:key k} [:span (name k)] [:span v]])]))