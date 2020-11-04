(ns matteoredaelli.lacinia-backend-ldap.schema
  (:import java.net.InetAddress java.net.Inet4Address java.net.Inet6Address)
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.resolve :as resolve]
    [com.stuartsierra.component :as component]
    [matteoredaelli.lacinia-backend-ldap.backend :as backend]))

(defn get-ip-address
  [dns-name]
  (try
     (.getHostAddress dns-name)
     (catch Exception e "")))

(defn get-ip-addresses
  [host]
  (try
    (let [addresses (InetAddress/getAllByName host)]
      (map get-ip-address addresses))
    (catch Exception e [])))

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

(defn ldap-ip-addresses
  [backend]
  (fn [context args value]
    (if (:dNSHostName value)
      (get-ip-addresses (:dNSHostName value))
      [])))

(defn ldap-object-locked
  [backend]
  (fn [_context _args value]
    (if (:lockoutTime value)
      (>= (Long. (:lockoutTime value)) 1)
      false)))

(defn ldap-object-pwd-last-set-days
  [backend]
  (fn [_context _args value]
    (quot (filedate-diff-from-now-in-seconds (:pwdLastSet value)) 86400000)))

(defn ldap-object-pwd-last-set-date
  [backend]
  (fn [_context _args value]
    (filedate-to-date  (:pwdLastSet value))
   ;; (java.util.Date. (* (+ -11644473590 (quot (Long. (:pwdLastSet value)) 10000000)) 1000)))
    ))

(defn ldap-object-direct-reports-objects
  [backend]
  (fn [context _args value]
    (let [
          {:keys [::system]} context]
      (backend/get-objects-by-dn backend
                                 system
                                 (:directReports value)))))

(defn ldap-object-manager-object
  [backend]
  (fn [context _args value]
    (let [
          {:keys [::system ]} context]
      (backend/get-object-by-dn backend
                                system
                                (:manager value)
                                ))))

(defn ldap-object-member-objects
  [backend]
  (fn [context _args value]
    (let [
          {:keys [::system]} context]
      (backend/get-objects-by-dn backend
                                 system
                                 (:member value)))))

(defn ldap-object-member-of-objects
  [backend]
  (fn [context _args value]
    (let [
          {:keys [::system]} context]
      (backend/get-objects-by-dn backend
                                 system
                                 (:memberOf value)))))

(defn ldap-object-members-flat-objects
  [backend]
  (fn [context _args value]
    (let [
          {:keys [::system ::searchdn]} context
          filter (str "(&(objectclass=user)(memberOf:1.2.840.113556.1.4.1941:=" (:distinguishedName value) "))")
          ]
      (backend/search-objects backend
                               system
                               filter
                               searchdn))))

(defn ldap-object-member-of-flat-objects
  [backend]
  (fn [context _args value]
    (let [
          {:keys [::system ::searchdn]} context
          filter (str "(member:1.2.840.113556.1.4.1941:=" (:distinguishedName value) ")")
          ]
      (backend/search-objects backend
                               system
                               filter
                               searchdn))))



(defn query-ldap-objects
  [backend]
  (fn [context args value]
    (let [
          {:keys [system searchdn filter]} args]
      (->
       (backend/search-objects backend
                               system
                               filter
                               searchdn)
       ;; https://lacinia.readthedocs.io/en/latest/resolve/context.html
       (resolve/with-context {::system system
                              ::searchdn  searchdn
                              }))
    )))


(defn query-ldap-empty-groups
  [backend]
  (fn [context args value]
    (let [
          {:keys [system searchdn filter]} args]
      (->
       (backend/search-objects backend
                               system
                               "(&(objectClass=group)(!(member=*)))"
                               searchdn)
       ;; https://lacinia.readthedocs.io/en/latest/resolve/context.html
       (resolve/with-context {::system system
                              ::searchdn  searchdn
                              }))
      )))

(defn resolver-map
  [component]
  (let [backend (:ldap-backend component)]
    {
     :LdapObject/locked (ldap-object-locked backend)
     :LdapObject/direct-reports-objects (ldap-object-direct-reports-objects backend)
     :LdapObject/ip-addresses (ldap-ip-addresses backend)
     :LdapObject/pwd-last-set-days (ldap-object-pwd-last-set-days backend)
     :LdapObject/pwd-last-set-date (ldap-object-pwd-last-set-date backend)
     :LdapObject/member-objects (ldap-object-member-objects backend)
     :LdapObject/member-of-objects (ldap-object-member-of-objects backend)
     :LdapObject/member-of-flat-objects (ldap-object-member-of-flat-objects backend)
     :LdapObject/members-flat-objects (ldap-object-members-flat-objects backend)
     :LdapObject/manager-object (ldap-object-manager-object backend)
     :query/ldap-empty-groups (query-ldap-empty-groups backend)
     :query/ldap-objects (query-ldap-objects backend)
     }
    ))

(defn get-schema
  [component]
  (-> (io/resource "ldap-schema.edn")
      slurp
      edn/read-string))

(defn load-schema
  [component]

  (-> (get-schema component)
      (util/attach-resolvers (resolver-map component))
      schema/compile))


(defrecord SchemaProvider [schema]

  component/Lifecycle

  (start [this]
    (assoc this :ldap-schema (load-schema this)))

  (stop [this]
    (assoc this :ldap-schema nil)))

(defn new-schema-provider
  []
  {:schema-provider (-> {}
                        map->SchemaProvider
                        (component/using [:ldap-backend]))})
