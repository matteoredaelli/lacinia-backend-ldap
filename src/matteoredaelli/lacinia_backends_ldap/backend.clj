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


(defn resolve-ldap-field-member-objects-v2
  [context arguments value]
  (let [
        {:keys [searchdn filter]} arguments]
    (map #(search-object-by-dn conn
                               %
                               (::searchdn context))
         (:member value)
         )))

(defn resolve-ldap-field-member-objects-v1
  [context arguments value]
  (let [
        {:keys [searchdn filter]} arguments]
    ;;(clojure.pprint/pprint (::searchdn context))
    (map #(first (ldap/search conn
                              (::searchdn context)
                              {:filter (str "(distinguishedName=" % ")")
                               ;; :attributes [:dn :cn :mail :memberOf]
                               }))
         (:member value)
         )))

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


(defn find-users-by-key-value
  [context arguments value]
  (let [
        {:keys [searchdn key value]} arguments]
    (ldap/search conn
                 searchdn
                 {:filter (str "(" key "=" value ")")
                  ;; :attributes [:dn :cn :mail :memberOf]
                  })))
