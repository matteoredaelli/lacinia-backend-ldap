(ns matteoredaelli.lacinia-backend-ldap.system
  (:require
   [com.stuartsierra.component :as component]
   [matteoredaelli.lacinia-backend-ldap.schema :as schema]
   [matteoredaelli.lacinia-backend-ldap.server :as server]
   [matteoredaelli.lacinia-backend-ldap.backend :as backend]
))

(defn new-system
  []
  (merge (component/system-map)
         (server/new-server)
         (schema/new-schema-provider)
         (backend/new-backend)))
