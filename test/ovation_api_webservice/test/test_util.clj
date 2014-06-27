(ns ovation-api-webservice.test.test-util
  (:import (java.util UUID))
  (:use midje.sweet)
  (:require [ovation-api-webservice.util :as util]))

(facts "about UUID parsing"
       (fact "parses UUID string with dashes"
             (util/parse-uuid "f9bc2337-116d-45de-a878-85dc4df90e89") => (java.util.UUID/fromString "f9bc2337-116d-45de-a878-85dc4df90e89"))

       (fact "parses UUID string without dashes"
             (util/parse-uuid "f9bc2337116d45dea87885dc4df90e89") => (java.util.UUID/fromString "f9bc2337-116d-45de-a878-85dc4df90e89")))
