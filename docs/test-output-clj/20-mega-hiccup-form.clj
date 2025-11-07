(ns examples.mega-form)

(defn form-field [{:keys [label type value error]}]
  [:div.field
   [:label label]
   [:input {:type type
            :value value
            :class (when error "error")}]
   (when error
     [:span.error-message error])])

(defn registration-form [state]
  (let [errors (:errors state)
        loading? (:loading state)
        submitted? (:submitted state)]
    [:form.registration {:on-submit (:on-submit state)}
     [:h2 "Create Account"]

     (form-field {:label "Username"
                  :type "text"
                  :value (get-in state [:values :username])
                  :error (:username errors)})

     (form-field {:label "Email"
                  :type "email"
                  :value (get-in state [:values :email])
                  :error (:email errors)})

     (form-field {:label "Password"
                  :type "password"
                  :value (get-in state [:values :password])
                  :error (:password errors)})

     [:div.actions
      [:button {:type "submit"
                :disabled loading?
                :class (if loading? "loading" "")}
       (if loading? "Creating..." "Create Account")]

      (when submitted?
        [:div.success
         [:p "Account created successfully!"]
         [:a {:href "/login"} "Go to Login"]])]]))
