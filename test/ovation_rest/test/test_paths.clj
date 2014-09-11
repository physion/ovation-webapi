(ns ovation-rest.test.test-paths
  (:use midje.sweet)
  (:import (us.physion.ovation.domain DtoBuilder URIs)
           (java.util UUID))
  (:require [ovation-rest.paths :as paths]))

(facts "about paths"
       (facts "about path splitting"
              (fact "splits an absolute path without trailing /"
                    (paths/split "/abc/def/ghi") => ["abc" "def" "ghi"])
              (fact "splits an absolute path with a trailing /"
                    (paths/split "/abc/def/ghi/") => ["abc" "def" "ghi"])
              (fact "split a relative path with a trailing /"
                    (paths/split "abc/def/ghi") => ["abc" "def" "ghi"]))
       (facts "about path joining"
              (fact "joins a vector of paths"
                    (paths/join ["abc" "def"]) => "abc/def")
              (fact "joins a vector of paths with trailing /"
                    (paths/join ["abc" "def" ""]) => "abc/def/")))
