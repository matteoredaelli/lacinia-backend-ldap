(ns matteoredaelli.lacinia-backend-ldap.schema
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.resolve :as resolve]
    [com.stuartsierra.component :as component]
    [matteoredaelli.lacinia-backend-ldap.backend :as backend]))

(defn filedate-seconds-to-date
  [seconds]
  (java.util.Date. (* (+ -11644473590 (quot (Long. seconds) 10000000)) 1000)))

(defn ldap-object-locked
  [backend]
  (fn [context arguments value]
    (>= (Long. (:lockoutTime value)) 1)))

(defn ldap-object-pwd-last-set-date
  [backend]
  (fn [context arguments value]
    (java.util.Date. (* (+ -11644473590 (quot (Long. (:pwdLastSet value)) 10000000)) 1000))))

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
