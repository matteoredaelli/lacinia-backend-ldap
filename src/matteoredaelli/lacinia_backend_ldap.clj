(ns matteoredaelli.lacinia-backend-ldap
  (:require [matteoredaelli.lacinia-backend-ldap.system :as system]
            [com.stuartsierra.component :as component]
            ))

(defn ^:private my-system
  "Creates a new system suitable for testing, and ensures that
  the HTTP port won't conflict with a default running system."
  []
  (-> (system/new-system)
      (assoc-in [:server :port] 8889)))

(def ^:dynamic ^:private *system*)

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating your server...")
  (binding [*system* (component/start-system (my-system))]))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
