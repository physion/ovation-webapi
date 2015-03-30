(ns ovation.test.dao
  (:use midje.sweet)
  (:import (java.util UUID))
  (:require [ovation.dao :as dao]
            [ovation.util :as util]
            [ovation.annotations :as annotations]
            [cemerick.url :as url]))

(facts "About annotation maps"
  (fact "replaces root User URI keys with user names in annotations map"
    (let [id1 (UUID/randomUUID)
          id2 (UUID/randomUUID)
          uri1 (str (util/create-uri id1))
          uri2 (str (util/create-uri id2))]

      (dao/replace-uri-keys-with-usernames ...api... {:tags  {uri1 ...user1...
                                                              uri2 ...user2...}
                                                      :notes {uri1 ...user1...
                                                              uri2 ...user2...}}) => {:tags  {...name1... ...user1...
                                                                                              ...name2... ...user2...}
                                                                                      :notes {...name1... ...user1...
                                                                                              ...name2... ...user2...}}
      (provided
        (dao/username-from-user-uri ...api... uri1) => ...name1...
        (dao/username-from-user-uri ...api... uri2) => ...name2...)))

  (fact "replaces root User URI keys with user names in dto :annotations map"
    (let [id1 (UUID/randomUUID)
          id2 (UUID/randomUUID)
          uri1 (str (util/create-uri id1))
          uri2 (str (util/create-uri id2))]

      (dao/replace-annotation-keys ...api... {:annotations {:tags  {uri1 ...user1...
                                                                    uri2 ...user2...}
                                                            :notes {uri1 ...user1...
                                                                    uri2 ...user2...}}}) => {:annotations {:tags  {...name1... ...user1...
                                                                                                                   ...name2... ...user2...}
                                                                                                           :notes {...name1... ...user1...
                                                                                                                   ...name2... ...user2...}}}
      (provided
        (dao/username-from-user-uri ...api... uri1) => ...name1...
        (dao/username-from-user-uri ...api... uri2) => ...name2...))))


(facts "About Entity-to-Map conversion"
  (fact "Calls transformation pipeline"
    (dao/convert-entity-to-map ...api... ...entity...) => ...map...
    (provided
      (dao/entity-to-dto ...entity...) => ...dto...
      (dao/remove-private-links ...dto...) => ...cleaned...
      (dao/links-to-rel-path ...cleaned...) => ...rellinks...
      (annotations/add-annotation-links ...rellinks...) => ...annotationlinks...
      (dao/dissoc-annotations ...annotationlinks...) => ...map...)))

(facts "About DTO link modifications"
  (fact "`remove-hidden-links` removes '_...' links"
    (dao/remove-private-links {:_id   ...id...
                              :links {"_collaboration_links" #{...hidden...}
                                      :link1                 ...link1...}}) => {:_id   ...id...
                                                                                :links {:link1 ...link1...}})
  (fact "`add-self-link` adds a self link to a DTO"
    (dao/add-self-link ...link... {:links {:foo ...foo...}}) => {:links {:foo ...foo...
                                                                        :self ...link...}}))
