(ns examples.destructuring)
(defn process-user [{:as user, :keys [name age email]}] {:raw user, :display-name name, :contact email, :adult? (>= age 18)})
(defn extract-coords [[x y & rest]] {:y y, :remaining rest, :x x})
(defn handle-response [{[error-msg] :error, :as full-data, [status body] :response}] (if error-msg {:ok? false, :error error-msg} {:ok? true, :status status, :data body}))