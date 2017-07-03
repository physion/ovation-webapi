(ns ovation.test.groups
  (:use midje.sweet)
  (:require [ovation.groups :as groups]
            [ovation.util :as util]
            [ovation.authz :as authz]
            [org.httpkit.fake :refer [with-fake-http]]
            [ovation.core :as core]))
