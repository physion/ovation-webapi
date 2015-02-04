(ns ovation.test.context
  (:use midje.sweet)
  (:require [ovation.context :as context]))


(facts "about context creation"
       (fact "creates context with default host"
             (let [api-key "api-key"]
               (context/make-context api-key) => ...ctx...
               (provided
                 (context/make-server "https://dev.ovation.io" api-key) => ...dsc...
                 (#'ovation.context/get-context-from-dsc ...dsc...) => ...ctx...)))

       (with-state-changes [(after :facts (System/clearProperty "OVATION_IO_HOST_URI"))]
                           (fact "creates context with OVATION_IO_HOST_URI"
                                 (let [api-key "api-key"
                                       host "https://host.com"]
                                   (do
                                     (System/setProperty "OVATION_IO_HOST_URI" host)
                                     (context/make-context api-key)) => ...ctx...
                                   (provided
                                     (context/make-server host api-key) => ...dsc...
                                     (#'ovation.context/get-context-from-dsc ...dsc...) => ...ctx...)))))

(facts "about context caching"
       (prerequisite
         (context/make-server "https://dev.ovation.io" ...apikey...) => ...dsc...
         (#'ovation.context/get-context-from-dsc ...dsc...) => ...ctx...)

       (fact "should cache a single data context"
             (context/cached-context ...apikey...) => ...ctx...
             (context/cached-context ...apikey...) => ...ctx...))
