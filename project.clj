(defproject ovation-webapi "0.1.0-SNAPSHOT"
            :description "Ovation REST API"
            :url "http://ovation.io"

            :repositories [["s3-ovation-snapshot-repository" {:url "s3p://maven.ovation.io/snapshot"}]
                           ["s3-ovation-release-repository" {:url "s3p://maven.ovation.io/release"}]]

            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/core.incubator "0.1.3"]

                           ;; Compojure API and middleware
                           [metosin/compojure-api "0.19.0"]
                           [metosin/ring-swagger-ui "2.1.8-M1"]
                           [ring-cors "0.1.4"]

                           ;; HTTP and CouchDB
                           [http-kit "2.1.18"]
                           [org.clojure/data.codec "0.1.0"]
                           [com.ashafa/clutch "0.4.0"]
                           [org.clojure/core.memoize "0.5.6"]

                           ;; New Relic agent (JAR)
                           [com.newrelic.agent.java/newrelic-agent "3.11.0"]

                           ;; Ovation API
                           [us.physion/ovation-api "3.0.7"]

                           ;; Deprecated
                           ;[com.google.guava/guava "13.0.1"]
                           ;[clojurewerkz/urly "1.0.0"]
                           ;[pathetic "0.5.1"]

                           ;; Logging
                           [org.clojure/tools.logging "0.3.1"]
                           [clj-logging-config "1.9.12"]
                           [org.slf4j/slf4j-api "1.7.7"]
                           [org.slf4j/slf4j-log4j12 "1.7.7"]
                           [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                              javax.jms/jms
                                                              com.sun.jmdk/jmxtools
                                                              com.sun.jmx/jmxri]]
                           ]

            :plugins [[lein-clojars "0.9.1"]
                      [lein-ring "0.8.10"]
                      [s3-wagon-private "1.1.2"]
                      [lein-midje "3.1.3"]
                      [lein-elastic-beanstalk "0.2.8-SNAPSHOT"]]

            :ring {:handler ovation.handler/app}

            ;; For New Relic, we need to bundle newrelic.yml and newrelic.jar
            :war-resources-path "war_resources"

            :aws {:beanstalk {:stack-name   "64bit Amazon Linux running Tomcat 7"
                              :environments [{:name "webapi-development"
                                              :env  {"OVATION_IO_HOST_URI" "https://dev.ovation.io"
                                                     "LOGGING_HOST"        "logging-host"}}

                                             {:name "webapi-production"
                                              :env  {"OVATION_IO_HOST_URI" "https://ovation.io"
                                                     "LOGGING_HOST"        "logging-host"}}]}}

            :profiles {
                       :ovation-web-api {:ring         {:handler ovation.handler/app}
                                         :reload-paths ["src"]}
                       :dev             {:dependencies [[javax.servlet/servlet-api "2.5"]
                                                        [ring-mock "0.1.5"]
                                                        [midje "1.6.3"]
                                                        [org.clojure/data.json "0.2.5"]
                                                        [http-kit.fake "0.2.1"]
                                                        [ring-serve "0.1.2"]]}
                       :jenkins         {:aws          {:access-key ~(System/getenv "AWS_ACCESS_KEY")
                                                        :secret-key ~(System/getenv "AWS_SECRET_KEY")}
                                         :repositories [["s3-ovation-snapshot-repository" {:url        "s3p://maven.ovation.io/snapshot"
                                                                                           :username   :env/AWS_ACCESS_KEY
                                                                                           :passphrase :env/AWS_SECRET_KEY}]
                                                        ["s3-ovation-release-repository" {:url        "s3p://maven.ovation.io/release"
                                                                                          :username   :env/AWS_ACCESS_KEY
                                                                                          :passphrase :env/AWS_SECRET_KEY}]]
                                         :local-repo   ".repository"}}

            :aliases {"server" ["with-profile" "ovation-web-api" "ring" "server"]})

