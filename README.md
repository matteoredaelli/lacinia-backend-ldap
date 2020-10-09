# lacinia-backends-ldap

LDAP / Active directory backend library for lacinia

## Usage

export LACINIA_BACKENDS_LDAP_CONFIG=/path/to/resource/lacinia-backends-ldap-config.edn


Build a deployable jar of this library:

	$ clojure -A:jar -M:jar
	$ clojure -A:uberjar -M:uberjar  ;; (it will include lib dependencies

Install it locally:

	$ clojure -A:install -M:install

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables:

	$ clojure -A:deploy -M:deploy

## ROADMAP

- [ ] support for multiple LDAP servers

## License

Copyright © 2020 Matteo

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
