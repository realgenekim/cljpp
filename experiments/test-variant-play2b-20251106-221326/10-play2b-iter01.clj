(ns examples.program10)
(defn user-badge [{:keys [name verified? role]}] [:div.user-badge [:span.name name] (when verified? [:span.verified "✓"]) [:span.role role]])
(defn user-list [users] [:div.user-list (for [user users] [:div.user [user-badge user]])])
(defn stats-panel [stats] (let [total (reduce + (vals stats))] [:div.stats-panel [:h3 "Statistics"] [:div.total [:span "Total: "] [:strong total]] (for [[k v] stats] [:div.stat [:span.label (name k)] [:span.value v]])]))