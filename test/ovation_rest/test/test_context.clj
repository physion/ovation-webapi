(ns ovation-rest.test.test-context
  (:use midje.sweet)
  (:require [ovation-rest.context :as context]))


;(facts "about context creation"
;       (fact "creates context from API key"
;             (let [api-key (System/getenv "API_KEY")]
;               (context/make-context api-key)) => truthy))
