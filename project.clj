(defproject ovation-webapi "1.0.0-SNAPSHOT"
  :description "Ovation REST API"
  :url "http://ovation.io"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]

                 ;; To manage compojure's outdated deps
                 [commons-codec "1.10" :exclusions [[org.clojure/clojure]]]

                 ;; Compojure API and middleware
                 [metosin/compojure-api "0.23.1"]
                 [metosin/ring-swagger-ui "2.1.3"]
                 [ring-cors "0.1.7"]
                 [thegreatape/ring-raygun "0.1.0"]
                 [buddy/buddy-auth "0.9.0"]


                 ;; HTTP and CouchDB
                 [http-kit "2.1.19"]
                 [org.clojure/data.codec "0.1.0"]
                 [com.ashafa/clutch "0.4.0"]

                 ;; New Relic agent (JAR)
                 [com.newrelic.agent.java/newrelic-agent "3.26.1"]
                 [yleisradio/new-reliquary "1.0.0"]


                 ;; Logging
                 [com.taoensso/timbre "4.3.1"]
                 [potemkin "0.4.3"]
                 [ring-logger-timbre "0.7.5"]

                 ;; Other
                 [org.clojure/data.json "0.2.6"]]


  :plugins [[lein-elastic-beanstalk "0.2.8-SNAPSHOT"]]

  :ring {:handler ovation.handler/app}

  :resource-paths ["resources"]

  ;; For New Relic, we need to bundle newrelic.yml and newrelic.jar
  :war-resources-path "war-resources"

  :aws {:beanstalk {:stack-name   "64bit Amazon Linux running Tomcat 7"
                    :environments [{:name "webapi-development"
                                    :env  {"OVATION_IO_HOST_URI" "https://dev.ovation.io"}}

                                   {:name "webapi-production"
                                    :env  {"OVATION_IO_HOST_URI" "https://services.ovation.io"}}

                                   {:name "webapi-clinical"
                                    :env {"OVATION_IO_HOST_URI" "https://clinical.ovation.io"}}]}}

  :profiles {
             :ovation-web-api {:ring         {:handler ovation.handler/app}
                               :reload-paths ["src"]}
             :dev             {:dependencies [[javax.servlet/servlet-api "2.5"]
                                              [ring-mock "0.1.5"]
                                              [midje "1.8.1"]
                                              [http-kit.fake "0.2.2"]
                                              [ring-serve "0.1.2"]]
                               :plugins      [[lein-midje "3.2"]
                                              [lein-ring "0.9.7"]]}
             :jmx             {:jvm-opts ["-Dcom.sun.management.jmxremote"
                                          "-Dcom.sun.management.jmxremote.ssl=false"
                                          "-Dcom.sun.management.jmxremote.authenticate=false"
                                          "-Dcom.sun.management.jmxremote.port=43210"]}
             :ci              {:aws {:access-key ~(System/getenv "AWS_ACCESS_KEY")
                                     :secret-key ~(System/getenv "AWS_SECRET_KEY")}}}

  :aliases {"server" ["with-profile" "ovation-web-api" "ring" "server"]})

