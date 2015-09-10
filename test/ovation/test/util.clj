(ns ovation.test.util
  (:import (java.util UUID)
           (java.net URI))
  (:use midje.sweet)
  (:require [ovation.util :as util]
            [ovation.version :refer [version]]))

(facts "about UUID parsing"
       (fact "parses UUID string with dashes"
             (util/parse-uuid "f9bc2337-116d-45de-a878-85dc4df90e89") => (java.util.UUID/fromString "f9bc2337-116d-45de-a878-85dc4df90e89"))

       (fact "parses UUID string without dashes"
             (util/parse-uuid "f9bc2337116d45dea87885dc4df90e89") => (java.util.UUID/fromString "f9bc2337-116d-45de-a878-85dc4df90e89")))


(facts "about URI creation"
  (fact "creates URI from UUID string"
    (let [id (str (UUID/randomUUID))]
      (util/create-uri id) => (URI. (format "%s://%s/%s"  "ovation"  "entities"  id))))
  (fact "creates URI from UUID"
    (let [id (UUID/randomUUID)]
      (util/create-uri id) => (URI. (format "%s://%s/%s"  "ovation"  "entities"  id))))
  (fact "passes through URI"
    (let [uri (util/create-uri (UUID/randomUUID))]
      (util/create-uri uri) => uri)))

(facts "About prefixed-path"
  (fact "adds prefix"
    (util/prefixed-path "my/path") => (str "/api/" version "/my/path"))
  (fact "adds prefix to absolute path"
    (util/prefixed-path "/my/path") => (str "/api/" version "/my/path"))
  (fact "skips already-prefixed path"
    (util/prefixed-path (str "/api/" version "/my/path")) => (str "/api/" version "/my/path")))
