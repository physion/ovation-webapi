(ns ovation-rest.test.annotations
  (:import (java.util UUID)
           (us.physion.ovation.domain URIs))
  (:use midje.sweet)
  (:require [ovation-rest.annotations :as a]
            [ovation-rest.util :as util]
            [ovation-rest.dao :as dao]))


(facts "About annotation links"
  (fact "adds annotation links to entity"
    (a/add-annotation-links {:_id   "123"
                             :links {:foo "bar"}}) => {:_id   "123"
                                                       :links {:foo             "bar"
                                                               :properties      "/api/v1/entities/123/annotations/properties"
                                                               :tags            "/api/v1/entities/123/annotations/tags"
                                                               :notes           "/api/v1/entities/123/annotations/notes"
                                                               :timeline-events "/api/v1/entities/123/annotations/timeline-events"}}))


(facts "About annotation maps"
  (fact "replaces User URI keys with user names"
    (let [id1 (UUID/randomUUID)
          id2 (UUID/randomUUID)
          uri1 (str (util/create-uri id1))
          uri2 (str (util/create-uri id2))]

      (a/replace-uri-keys-with-usernames ...api... {uri1 ...user1...
                                                    uri2 ...user2...}) => {...name1... ...user1...
                                                                           ...name2... ...user2...}
      (provided
        (dao/username-from-user-uri ...api... uri1) => ...name1...
        (dao/username-from-user-uri ...api... uri2) => ...name2...))))
