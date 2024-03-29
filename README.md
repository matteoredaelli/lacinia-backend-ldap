# lacinia-backends-ldap

LDAP / Active directory backend library for lacinia

## Usage

Create a configuration file and set the environment

	export LACINIA_BACKEND_LDAP_CONFIG=/path/to/resource/lacinia-backend-ldap-config.edn


Add this library to your project: see https://clojars.org/matteoredaelli/lacinia-backend-ldap

## Sources

Run the project directly:

	$ clojure -m matteoredaelli.lacinia-backend-ldap

Build a deployable jar of this library:

    $ clojure -X:uberjar :jar lacinia-backend-ldap.jar
    $ java -cp lacinia-backend-ldap.jar clojure.main -m matteoredaelli.lacinia-backend-ldap

or

    $ clojure -X:uberjar :jar lacinia-backend-ldap.jar  :main-class matteoredaelli.lacinia-backend-ldap
    $ java -jar lacinia-backend-ldap.jar clojure.main
	
Install it locally:

	$ clojure -A:install -M:install   (old way, tobe updated)

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables:

	$ clojure -A:deploy -M:deploy  (old way, tobe updated)

## ROADMAP

- [X] support for multiple LDAP servers

## License

Copyright © 2020 Matteo

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
