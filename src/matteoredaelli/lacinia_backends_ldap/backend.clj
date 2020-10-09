(ns matteoredaelli.lacinia-backends-ldap.backend
  (:require
   [com.walmartlabs.lacinia.resolve :as resolve]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clj-ldap.client :as ldap]
   [clojure.string :as str])
  )

(defn ldap-connect
  []
  (->
   ;;(io/resource "backend-ldap-connect.edn")
   (System/getenv "LACINIA_BACKENDS_LDAP_CONFIG")
      slurp
      edn/read-string
      ldap/connect))

(defonce conn (ldap-connect))

(defn search-object-by-dn
  [conn dn searchdn]
  (first (ldap/search conn
                      searchdn
                      {:filter (str "(distinguishedName=" dn ")") })))

(defn search-objects-by-dn
  [conn dn-list searchdn]
  (map #(search-object-by-dn conn
                             %
                             searchdn)
       dn-list
       ))

(defn resolve-ldap-field-manager-object
  [context arguments value]
  (let [
        {:keys [searchdn filter]} arguments]
    (search-object-by-dn conn
                         (:manager value)
                         (::searchdn context))))

(defn resolve-ldap-field-member-objects
  [context arguments value]
  (let [
        {:keys [searchdn filter]} arguments]
    (search-objects-by-dn conn
                          (:member value)
                          (::searchdn context))))

(defn resolve-query-ldap-objects
  [context arguments value]
  (let [
        {:keys [searchdn filter]} arguments]
    (->
     (ldap/search conn
                  searchdn
                  {:filter filter
                   ;; :attributes [:dn :cn :mail :memberOf]
                   })
     ;; https://lacinia.readthedocs.io/en/latest/resolve/context.html
     (resolve/with-context {::searchdn searchdn}))))
