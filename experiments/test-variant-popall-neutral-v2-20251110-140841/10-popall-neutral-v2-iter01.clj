(ns examples.program10)
(defn user-badge [{:keys [name verified? role]}] [:div.user-badge [:span.name name] (when verified? [:span.badge "✓"]) [:span.role role]])
(defn user-list [users] [:div.users (for [user users] [user-badge user])])
(defn stats-panel [stats] (let [total (reduce + (vals stats))] [:div.stats [:h3 (str "Total: " total)] (for [[k v] stats] [:div.stat [:span.label (name k)] [:span.value v]])]))