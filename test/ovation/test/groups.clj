(ns ovation.test.groups
  (:use midje.sweet)
  (:require [ovation.groups :as groups]
            [ovation.util :as util]
            [ovation.request-context :as request-context]
            [clojure.core.async :as async]))


(facts "About `get-group-projects`"
  (future-fact "Gets projects by group->projects UUIDs"))
