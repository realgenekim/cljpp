(ns examples.program10)
(defn user-badge [{:keys [name verified? role]}] [:div.user-badge [:div.user-info [:span.name name] (when verified? [:span.badge "✓"])] [:div.role role]])
(defn user-list [users] [:div.user-list (for [user users] [:div {:key (:name user)} (user-badge user)])])
(defn stats-panel [stats] (let [total (reduce + (vals stats))] [:div.stats-panel [:div.total [:span "Total: "] [:span.count total]] (for [[stat-name stat-value] stats] [:div.stat {:key stat-name} [:span.label (name stat-name)] [:span.value stat-value]])]))