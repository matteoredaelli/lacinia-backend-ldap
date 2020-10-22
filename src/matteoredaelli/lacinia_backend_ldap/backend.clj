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

(defn ldap-connect
  [system-info]
  (log/info :info "connecting to ldap"
            :system-info system-info)
  (ldap/connect system-info))

(defrecord LdapBackend [ds]

  component/Lifecycle

  (start [this]
    (assoc this
           :ds (into {}
                     (map (fn [[system system-info]] {system
                                                      (ldap-connect system-info)})
                          (:systems (pooled-data-source))))
           ))

  (stop [this]
    ;; TODO
    ;;(ldap/release-connection (pooled-data-source) :ds)
    (assoc this :ds nil)))

(defn new-backend
  []
  {:ldap-backend (map->LdapBackend {})})




(defn get-system-ds
  [component system]
  (get-in component
          [:ds (keyword system)]))

(defn search-objects
  [component system filter searchdn]
  (log/debug :component component
             :filter    filter
             :system    system
             :searchdn  searchdn)
  (ldap/search (get-system-ds component system)
               searchdn
               {:filter filter})
  )

(defn get-object-by-dn
  [component system dn]
  (log/debug :component component
             :system    system
             :dn  dn)
  (ldap/get (get-system-ds component system)
            dn))

(defn get-objects-by-dn
  [component system dn-list]
  (map #(get-object-by-dn component system %)
       dn-list
       ))
