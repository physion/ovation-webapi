(ns ovation-rest.test.test-util
  (:import (java.util UUID))
  (:use midje.sweet)
  (:require [ovation-rest.util :as util]))

(facts "about UUID parsing"
       (fact "parses UUID string with dashes"
             (util/parse-uuid "f9bc2337-116d-45de-a878-85dc4df90e89") => (java.util.UUID/fromString "f9bc2337-116d-45de-a878-85dc4df90e89"))

       (fact "parses UUID string without dashes"
             (util/parse-uuid "f9bc2337116d45dea87885dc4df90e89") => (java.util.UUID/fromString "f9bc2337-116d-45de-a878-85dc4df90e89")))


(facts "about entity link munging"
       (fact "replaces scheme and host"
             (util/to-web-uri "https://server.com/api/" "ovation://entities/123-abc") => "https://server.com/api/entities/123-abc")
       (fact "optionally replaces port"
             (util/to-web-uri "https://server.com:123/api/" "ovation://entities/123-abc") => "https://server.com:123/api/entities/123-abc")
       (fact "does not replace query parameters"
             (util/to-web-uri "https://server.com:123/api/" "ovation://views/123-abc?q=ovation://entities/456-def") => "https://server.com:123/api/views/123-abc?q=ovation://entities/456-def")
       (fact "does not munge array parameters"
             (util/to-web-uri "https://server.com:123/api/" "ovation://views/123-abc?q=[%22ovation://entities/456-def%22]") => "https://server.com:123/api/views/123-abc?q=[%22ovation://entities/456-def%22]"))

(facts "about entity link unmunging"
       (fact "replaces scheme and host"
             (util/to-ovation-uri "https://server.com/api/entities/123-abc" "https://server.com/api/") => "ovation://entities/123-abc")
       (fact "optionally replaces port"
             (util/to-ovation-uri "https://server.com:123/api/entities/123-abc" "https://server.com:123/api/") => "ovation://entities/123-abc")
       (fact "does not replace query parameters"
             (util/to-ovation-uri "https://server.com:123/api/views/123-abc?q=ovation://entities/456-def" "https://server.com:123/api/") => "ovation://views/123-abc?q=ovation://entities/456-def")
       (fact "does not replace query array parameters"
             (util/to-ovation-uri '"https://server.com:123/api/views/123-abc?q=[%22ovation://entities/456-def%22]" "https://server.com:123/api/") => "ovation://views/123-abc?q=[%22ovation://entities/456-def%22]"))


(facts "about host context"
       (fact "gives whole context path"
             (util/host-context ...request...) => "https://server.com/api/v1"
             (provided
               (util/host-from-request ...request...) => "https://server.com/"
               (#'ovation-rest.util/request-context ...request...) => "/api/v1/"))
       (fact "optionally dir-ups context path"
             (util/host-context ...request... :remove-levels 1) => "https://server.com/api"
             (provided
               (util/host-from-request ...request...) => "https://server.com/"
               (#'ovation-rest.util/request-context ...request...) => "/api/v1/")))
