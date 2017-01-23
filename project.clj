(defproject ovation-webapi "1.0.0-SNAPSHOT"
  :min-lein-version "2.5.0"
  :description "Ovation REST API"
  :url "http://ovation.io"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.395"]

                 ;; To manage compojure's outdated deps
                 [commons-codec "1.10" :exclusions [[org.clojure/clojure]]]

                 ;; Compojure API and middleware
                 [metosin/compojure-api "1.1.10"]
                 [ring-cors "0.1.8"]
                 [ring-logger "0.7.6"]
                 [buddy/buddy-auth "1.1.0"]
                 [ring/ring-jetty-adapter "1.5.0"]


                 ;; HTTP and CouchDB
                 [http-kit "2.2.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [com.ashafa/clutch "0.4.0"]

                 ;; New Relic agent (JAR)
                 [com.newrelic.agent.java/newrelic-agent "3.34.0"]
                 [yleisradio/new-reliquary "1.0.0"]
                 [com.climate/clj-newrelic "0.2.1"]

                 ;; Raygun
                 [com.mindscapehq/core "2.1.0"]


                 ;; Logging
                 [com.taoensso/timbre "4.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [potemkin "0.4.3"]
                 [ring-logger-timbre "0.7.5"]

                 ;; Other
                 [org.clojure/data.json "0.2.6"]
                 [com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer "20160628.1" :exclusions [com.google.guava/guava]]
                 [environ "1.1.0"]

                 ;; Graph
                 [ubergraph "0.2.2"]]


  :plugins [[lein-elastic-beanstalk "0.2.8-SNAPSHOT"]]

  :ring {:handler ovation.handler/app
         :main    ovation.main}

  :main ovation.main

  :resource-paths ["resources"]

  ;; For EB .ebextensions
  ;:war-resources-path "war-resources"

  :aws {:beanstalk {:stack-name   "64bit Amazon Linux running Tomcat 8"
                    :environments [{:name "ovation-webapi-staging"}]}}

  :profiles {:dev      {:dependencies [[ring-mock "0.1.5"]
                                       [midje "1.8.3"]
                                       [http-kit.fake "0.2.2"]
                                       [ring-server "0.4.0"]]
                        :plugins      [[lein-midje "3.2.1"]
                                       [lein-ring "0.10.0"]]}

             :uberjar  {:aot [ovation.main]}

             :newrelic {:java-agents [[com.newrelic.agent.java/newrelic-agent "3.28.0"]]
                        :jvm-opts    ["-Dnewrelic.config.file=/app/newrelic/newrelic.yml"]}

             :jmx      {:jvm-opts ["-Dcom.sun.management.jmxremote"
                                   "-Dcom.sun.management.jmxremote.ssl=false"
                                   "-Dcom.sun.management.jmxremote.authenticate=false"
                                   "-Dcom.sun.management.jmxremote.port=43210"]}

             :ci       {:aws {:access-key ~(System/getenv "AWS_ACCESS_KEY")
                              :secret-key ~(System/getenv "AWS_SECRET_KEY")}}})

