(ns examples.program10)
(defn user-badge [{:keys [name verified? role]}] [:div.badge [:span.name name] (when verified? [:span.verified "✓"]) [:span.role role]])
(defn user-list [users] [:div.users (for [user users] (user-badge user))])
(defn stats-panel [stats] (let [total (reduce + (vals stats))] [:div.stats [:div.total total] (for [[k v] stats] [:div.stat k v])]))