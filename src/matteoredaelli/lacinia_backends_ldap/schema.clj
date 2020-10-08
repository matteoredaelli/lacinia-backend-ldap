(ns matteoredaelli.lacinia-backends-ldap.schema
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.walmartlabs.lacinia.util :as util]
    [matteoredaelli.lacinia-backends-ldap.backend :as backend]))

(defn schema
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers {:resolve-ldap-field-member-objects backend/resolve-ldap-field-member-objects
                              :resolve-ldap-field-manager-object backend/resolve-ldap-field-manager-object
                              :resolve-query-ldap-objects backend/resolve-query-ldap-objects
                              })
      schema/compile))
