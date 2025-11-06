(ns examples.web)

(defn wrap-logging [handler]
  (fn [request]
    (println "Request:" (:uri request))
    (let [response (handler request)]
      (println "Response:" (:status response))
      response)))

(defn wrap-auth [handler]
  (fn [request]
    (if (get-in request [:headers "authorization"])
      (handler request)
      {:status 401 :body "Unauthorized"})))

(defn app [request]
  (case (:uri request)
    "/" {:status 200 :body "Home"}
    "/api/users" {:status 200 :body [{:id 1 :name "Alice"}]}
    {:status 404 :body "Not Found"}))

(def handler
  (-> app
      wrap-auth
      wrap-logging))
