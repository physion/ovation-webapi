(ns ovation.test.context
  (:use midje.sweet)
  (:require [ovation.context :as context]))


(facts "about context creation"
       (fact "creates context with default host"
             (let [api-key "mykey"
                   auth-host (if-let [host (or (System/getProperty "OVATION_IO_HOST_URI") (System/getenv "OVATION_IO_HOST_URI"))]
                               host
                               "https://dev.ovation.io")]
               (context/make-context api-key) => ...ctx...
               (provided
                 (context/make-server auth-host api-key) => ...dsc...
                 (#'ovation.context/get-context-from-dsc ...dsc...) => ...ctx...)))

       (with-state-changes [(after :facts (System/clearProperty "OVATION_IO_HOST_URI"))]
                           (fact "creates context with OVATION_IO_HOST_URI"
                                 (let [api-key "mykey"
                                       host "https://host.com"]
                                   (do
                                     (System/setProperty "OVATION_IO_HOST_URI" host)
                                     (context/make-context api-key)) => ...ctx...
                                   (provided
                                     (context/make-server host api-key) => ...dsc...
                                     (#'ovation.context/get-context-from-dsc ...dsc...) => ...ctx...)))))

