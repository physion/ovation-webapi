(defproject ovation-webapi "1.0.0-SNAPSHOT"
  :min-lein-version "2.5.0"
  :description "Ovation REST API"
  :url "http://ovation.io"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]

                 ;; To manage compojure's outdated deps
                 [commons-codec "1.10" :exclusions [[org.clojure/clojure]]]

                 ;; Compojure API and middleware
                 [metosin/compojure-api "1.0.2"]
                 [metosin/ring-swagger-ui "2.1.4-0"]
                 [ring-cors "0.1.7"]
                 [ring-logger "0.7.6"]
                 [buddy/buddy-auth "0.12.0"]
                 [ring/ring-jetty-adapter "1.4.0"]


                 ;; HTTP and CouchDB
                 [http-kit "2.1.19"]
                 [org.clojure/data.codec "0.1.0"]
                 [com.ashafa/clutch "0.4.0"]

                 ;; New Relic agent (JAR)
                 [com.newrelic.agent.java/newrelic-agent "3.28.0"]
                 [yleisradio/new-reliquary "1.0.0"]

                 ;; Raygun
                 [com.mindscapehq/core "1.6.0"]


                 ;; Logging
                 [com.taoensso/timbre "4.3.1"]
                 [potemkin "0.4.3"]
                 [ring-logger-timbre "0.7.5"]

                 ;; Other
                 [org.clojure/data.json "0.2.6"]
                 [com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer "r239"]]
                 ;[clj-tagsoup/clj-tagsoup "0.3.0"]]


  :plugins [[lein-elastic-beanstalk "0.2.8-SNAPSHOT"]]

  :ring {:handler ovation.handler/app}

  :main ovation.main

  :resource-paths ["resources"]

  ;; For EB .ebextensions
  :war-resources-path "war-resources"

  :aws {:beanstalk {:stack-name   "64bit Amazon Linux running Tomcat 7"
                    :environments [{:name "webapi-development"
                                    :env  {"OVATION_IO_HOST_URI" "https://services-staging.ovation.io"}}]}}

  :profiles {:dev      {:dependencies [[ring-mock "0.1.5"]
                                       [midje "1.8.3"]
                                       [http-kit.fake "0.2.2"]
                                       [ring-server "0.4.0"]]
                        :plugins      [[lein-midje "3.2"]
                                       [lein-ring "0.9.7"]]}

             :newrelic {:java-agents [[com.newrelic.agent.java/newrelic-agent "3.28.0"]]
                        :jvm-opts    ["-Dnewrelic.config.file=/app/newrelic/newrelic.yml"]}

             :jmx      {:jvm-opts ["-Dcom.sun.management.jmxremote"
                                   "-Dcom.sun.management.jmxremote.ssl=false"
                                   "-Dcom.sun.management.jmxremote.authenticate=false"
                                   "-Dcom.sun.management.jmxremote.port=43210"]}

             :ci       {:aws {:access-key ~(System/getenv "AWS_ACCESS_KEY")
                              :secret-key ~(System/getenv "AWS_SECRET_KEY")}}})

