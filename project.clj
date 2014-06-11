(defproject ovation-api-webservice "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://ovation.io"

  :repositories [["s3-ovation-snapshot-repository" {:url "s3://maven.ovation.io/snapshot"}]
                  ["s3-ovation-release-repository" {:url "s3://maven.oation.io/release"}]]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.incubator "0.1.3"]
                 [compojure "1.1.6"]
                 [ring-cors "0.1.2"]
                 [us.physion/ovation-api "3.0.0-SNAPSHOT"]]
  
  :plugins [[lein-ring "0.8.10"]
            [lein-localrepo "0.5.3"]
            [lein-maven-s3-wagon "0.2.3"]]
  
  :ring {:handler ovation-api-webservice.handler/app}
  
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})