(ns examples.hiccup)

(defn user-badge [{:keys [name verified? role]}]
  [:div.user-badge
   [:span.name name]
   (when verified?
     [:span.verified "✓"])
   [:span.role (str role)]])

(defn user-list [users]
  [:div.user-list
   [:h2 "Users"]
   (for [user users]
     [:div.user {:key (:id user)}
      (user-badge user)])])

(defn stats-panel [stats]
  (let [total (reduce + (vals stats))]
    [:div.stats
     [:h3 "Statistics"]
     [:div.total "Total: " total]
     (for [[k v] stats]
       [:div.stat {:key k}
        [:span.label (name k)]
        [:span.value v]])]))
