(ns examples.program20)

(defn form-field
  [id label-text error]
  [:div.form-group
   [:label {:for id} label-text]
   [:input {:type "text" :id id :name id}]
   [:span.error error]])

(defn registration-form
  []
  (let [errors {:email "Invalid email" :password "Too short"}
        loading false
        submitted true]
    [:form.registration
     (form-field "username" "Username" nil)
     (form-field "email" "Email" (:email errors))
     (form-field "password" "Password" (:password errors))
     [:div.actions
      [:button {:type "submit" :disabled loading} "Register"]
      (when submitted
        [:p.success "Registration complete!"])]]))
