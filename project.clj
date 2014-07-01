(defproject ovation-api-webservice "0.1.0-SNAPSHOT"
  :description "Ovation REST API"
  :url "http://ovation.io"

  :repositories [["s3-ovation-snapshot-repository" {:url "s3p://maven.ovation.io/snapshot"}]
                  ["s3-ovation-release-repository" {:url "s3p://maven.ovation.io/release"}]]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.incubator "0.1.3"]
                 [compojure "1.1.6"]
                 [ring/ring-json "0.3.1"]
                 [ring-cors "0.1.2"]
                 [org.clojure/core.memoize "0.5.6"]
                 [us.physion/ovation-api "3.0.0-SNAPSHOT"]
                 [us.physion/ovation-logging "3.0.0-SNAPSHOT"]]

  :plugins [[lein-ring "0.8.10"]
            [s3-wagon-private "1.1.2"]
            [lein-midje "3.0.0"]]

  :ring {:handler ovation-api-webservice.handler/app}

  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [midje "1.5.1"]]}})

