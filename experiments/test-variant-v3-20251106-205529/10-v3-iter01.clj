(ns examples.program10)
(defn user-badge [{:keys [name verified? role]}] [:div.user-badge [:div.user-name name] (when verified? [:span.verified-badge "✓"]) [:div.user-role role]])
(defn user-list [users] [:div.user-list (for [user users] [:div.user-item [user-badge user]])])
(defn stats-panel [stats] (let [total (reduce + (vals stats))] [:div.stats-panel [:div.total [:span "Total: "] [:span.count total]] (for [[k v] stats] [:div.stat-item [:span.stat-label (name k)] [:span.stat-value v]])]))