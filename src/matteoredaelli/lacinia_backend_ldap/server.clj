(ns  matteoredaelli.lacinia-backend-ldap.server
  (:require [com.stuartsierra.component :as component]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [io.pedestal.http :as http]))

(defn html-response
  [html]
  {:status 200 :body html :headers {"Content-Type" "text/html"}})

;; Gather some data from the user to retain in their session.
(defn index-page
  "Prompt a user for their name, then remember it."
  [req]
  (html-response
   ;;(slurp (io/resource "public/index.html"))
   "<html><body>ldap backend</body></html>"
   ))

(def root-route
  ["/" :get `index-page])

(defn add-route
  [service-map]
  (let [{routes ::http/routes} service-map
        ext-routes (conj routes root-route)]
    (assoc service-map ::http/routes ext-routes)))

(defrecord LdapServer [schema-provider server port]

  component/Lifecycle
  (start [this]
    (assoc this :ldap-server (-> schema-provider
                                 :ldap-schema
                                 (lp/service-map
                                  {:graphiql true
                                   :ide-path "/graphiql"
                                   :port port
                                   :subscriptions true
                                   ;;  :ide-headers {:authorization "Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyLWlkIjozfQ.JH0Q2flkonyDPk_yiSrTK5VSKrbrsdR0FEePMgiEwDE"}
                              })
                            (merge {::http/resource-path "/public"})
                            (add-route)
                            http/create-server
                            http/start)))

  (stop [this]
    (http/stop server)
    (assoc this :ldap-server nil)))

(defn new-server
  []
  {:ldap-server (component/using (map->LdapServer {:port 8888})
                            [:schema-provider])})
