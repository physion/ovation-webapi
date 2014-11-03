(ns ovation-rest.test.dao
  (:use midje.sweet)
  (:import (java.util UUID))
  (:require [ovation-rest.dao :as dao]
            [ovation-rest.util :as util]))

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
