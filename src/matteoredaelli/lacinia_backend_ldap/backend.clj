(ns matteoredaelli.lacinia-backend-ldap.backend
  (:require

   [com.stuartsierra.component :as component]
   [io.pedestal.log :as log]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clj-ldap.client :as ldap]
   [clojure.string :as str])
  )

(defn ^:private pooled-data-source
  []
  (->
   (System/getenv "LACINIA_BACKEND_LDAP_CONFIG")
   slurp
   edn/read-string))

(defrecord LdapBackend [ds]

  component/Lifecycle

  (start [this]
    (assoc this
           :ds (ldap/connect (pooled-data-source))))

  (stop [this]
    (ldap/release-connection (pooled-data-source) :ds)
    (assoc this :ds nil)))

(defn new-backend
  []
  {:backend (map->LdapBackend {})})


(defn search-objects
  [component filter searchdn]
  (log/debug :component component
             :filter  filter
             :searchdn searchdn)
  (ldap/search (:ds component)
               searchdn
               {:filter filter})
  )

(defn get-object-by-dn
  [component dn]
  (ldap/get (:ds component) dn))

(defn get-objects-by-dn
  [component dn-list]
  (map #(get-object-by-dn component %)
       dn-list
       ))
