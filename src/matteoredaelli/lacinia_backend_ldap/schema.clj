(ns matteoredaelli.lacinia-backend-ldap.schema
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.resolve :as resolve]
    [com.stuartsierra.component :as component]
    [matteoredaelli.lacinia-backend-ldap.backend :as backend]))

(defn filedate-to-unixtime
  [filedate]
  (* (+ -11644473590 (quot (Long. filedate) 10000000)) 1000))

(defn filedate-to-date
  [filedate]
  (java.util.Date. (filedate-to-unixtime filedate)))

(defn filedate-diff-from-now-in-seconds
  [filedate]
  (let [now (.getTime (java.util.Date.))
        before (filedate-to-unixtime filedate)
        ]
    (- now before)))


(defn ldap-object-locked
  [backend]
  (fn [context arguments value]
    (>= (Long. (:lockoutTime value)) 1)))

(defn ldap-object-pwd-last-set-days
  [backend]
  (fn [context arguments value]
    (quot (filedate-diff-from-now-in-seconds (:pwdLastSet value)) 86400000)))

(defn ldap-object-pwd-last-set-date
  [backend]
  (fn [context arguments value]
    (filedate-to-date  (:pwdLastSet value))
   ;; (java.util.Date. (* (+ -11644473590 (quot (Long. (:pwdLastSet value)) 10000000)) 1000)))
    ))

(defn ldap-object-manager-object
  [backend]
  (fn [context arguments value]
    (let [
          {:keys [searchdn filter]} arguments]
      (backend/get-object-by-dn backend
                                (:manager value)
                                ))))

(defn ldap-object-member-objects
  [backend]
  (fn [context arguments value]
    (let [
          {:keys [searchdn filter]} arguments]
      (backend/get-objects-by-dn backend
                                 (:member value)))))

(defn query-ldap-objects
  [backend]
  (fn [context arguments value]
    (let [
          {:keys [searchdn filter]} arguments]

      ;;     (->
      (backend/search-objects backend
                              filter
                              searchdn)
       ;; https://lacinia.readthedocs.io/en/latest/resolve/context.html
       ;;(resolve/with-context {::searchdn searchdn})
    ;   )
    )))

(defn resolver-map
  [component]
  (let [backend (:backend component)]
    {
     :LdapObject/locked (ldap-object-locked backend)
     :LdapObject/pwd-last-set-days (ldap-object-pwd-last-set-days backend)
     :LdapObject/pwd-last-set-date (ldap-object-pwd-last-set-date backend)
     :LdapObject/member-objects (ldap-object-member-objects backend)
     :LdapObject/manager-object (ldap-object-manager-object backend)
     :query/ldap-objects (query-ldap-objects backend)
     }
    ))

(defn load-schema
  [component]
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map component))
      schema/compile))


(defrecord SchemaProvider [schema]

  component/Lifecycle

  (start [this]
    (assoc this :schema (load-schema this)))

  (stop [this]
    (assoc this :schema nil)))

(defn new-schema-provider
  []
  {:schema-provider (-> {}
                        map->SchemaProvider
                        (component/using [:backend]))})
