(ns examples.program10)

(defn user-badge
  [{:keys [name verified? role]}]
  [:div.user-badge
   [:span.name name]
   (when verified?
     [:span.verified "✓"])
   [:span.role role]])

(defn user-list
  [users]
  [:div.user-list
   (for [user users]
     [user-badge user])])

(defn stats-panel
  [stats]
  (let [total (reduce + (vals stats))]
    [:div.stats-panel
     [:div.total "Total: " total]
     [:ul.stats
      (for [[k v] stats]
        [:li [:span.key (name k)] ": " [:span.value v]])]]))
