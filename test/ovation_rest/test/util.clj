(ns ovation-rest.test.util
  (:import (java.util UUID)
           (us.physion.ovation.domain URIs))
  (:use midje.sweet)
  (:require [ovation-rest.util :as util]
            [ovation-rest.dao :as dao]))

(facts "about UUID parsing"
       (fact "parses UUID string with dashes"
             (util/parse-uuid "f9bc2337-116d-45de-a878-85dc4df90e89") => (java.util.UUID/fromString "f9bc2337-116d-45de-a878-85dc4df90e89"))

       (fact "parses UUID string without dashes"
             (util/parse-uuid "f9bc2337116d45dea87885dc4df90e89") => (java.util.UUID/fromString "f9bc2337-116d-45de-a878-85dc4df90e89")))


(facts "about URI creation"
  (fact "creates URI from UUID string"
    (let [id (str (UUID/randomUUID))]
      (util/create-uri id) => (URIs/create id)))
  (fact "creates URI from UUID"
    (let [id (UUID/randomUUID)]
      (util/create-uri id) => (URIs/create id)))
  (fact "passes through URI"
    (let [uri (util/create-uri (UUID/randomUUID))]
      (util/create-uri uri) => uri)))

(facts "about Users"
  (fact "gets username for URI"
    (dao/username-from-user-uri ...api... ...uri...) => ...user...
    (provided
      (util/get-entity-id ...uri...) => ...id...
      (dao/get-entity ...api... ...id...) => ...entity...
      (#'ovation-rest.dao/get-username ...entity...) => ...user...)))
