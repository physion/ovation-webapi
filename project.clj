(defproject ovation-webapi "1.0.0-SNAPSHOT"
  :description "Ovation REST API"
  :url "http://ovation.io"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; Compojure API and middleware
                 [metosin/compojure-api "0.22.2"]
                 [metosin/ring-swagger-ui "2.1.2"]
                 [ring-cors "0.1.7"]

                 ;; HTTP and CouchDB
                 [http-kit "2.1.18"]
                 [org.clojure/data.codec "0.1.0"]
                 [com.ashafa/clutch "0.4.0"]

                 ;; New Relic agent (JAR)
                 [com.newrelic.agent.java/newrelic-agent "3.11.0"]


                 ;; Logging
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-logging-config "1.9.12"]
                 [org.slf4j/slf4j-api "1.7.7"]
                 [org.slf4j/slf4j-log4j12 "1.7.7"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [ring.middleware.logger "0.5.0"]
                 [ring.middleware.conditional "0.1.0"]

                 ;; Other
                 [clj-time "0.9.0"]
                 [org.clojure/data.json "0.2.6"]

                 ;; Raygun.io
                 [thegreatape/ring-raygun "0.1.0"]
                 ]

  :plugins [[lein-clojars "0.9.1"]
            [lein-elastic-beanstalk "0.2.8-SNAPSHOT"]
            [lein-awsuberwar "0.1.3"]]

  :ring {:handler ovation.handler/app}

  :resource-paths ["resources"]

  ;; For New Relic, we need to bundle newrelic.yml and newrelic.jar
  :war-resources-path "war_resources"

  :aws {:beanstalk {:stack-name   "64bit Amazon Linux running Tomcat 7"
                    :environments [{:name "webapi-development"
                                    :env  {"OVATION_IO_HOST_URI" "https://dev.ovation.io"}}

                                   {:name "webapi-production"
                                    :env  {"OVATION_IO_HOST_URI" "https://ovation.io"}}

                                   {:name "webapi-development-2"}]}}

  :profiles {
             :ovation-web-api {:ring         {:handler ovation.handler/app}
                               :reload-paths ["src"]}
             :dev             {:dependencies [[javax.servlet/servlet-api "2.5"]
                                              [ring-mock "0.1.5"]
                                              [midje "1.7.0"]
                                              [org.clojure/data.json "0.2.5"]
                                              [http-kit.fake "0.2.1"]
                                              [ring-serve "0.1.2"]]
                               :plugins      [[lein-midje "3.1.3"]
                                              [lein-ring "0.8.10"]]}
             :jenkins         {:aws          {:access-key ~(System/getenv "AWS_ACCESS_KEY")
                                              :secret-key ~(System/getenv "AWS_SECRET_KEY")}
                               :local-repo   ".repository"}}

  :aliases {"server" ["with-profile" "ovation-web-api" "ring" "server"]})

