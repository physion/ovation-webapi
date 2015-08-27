(ns ovation.test.router
  (:use midje.sweet)
  (:require [ovation.route-helpers :as r]
            [ovation.util :as util]))

(facts "About routes"
  (fact "`relationship-route`"
    (fact "provides relationship route for doc and name"
      (r/relationship-route ..rt.. ..doc.. ..name..) => ..route..
      (provided
        ..doc.. =contains=> {:type ..type..}
        (..rt.. (keyword (format "%s-relationships-%s" ..type.. ..name..))) => ..route..)))

  (facts "`target-route`"
    (fact "provides targets route for doc and name"
      (r/targets-route ..rt.. ..doc.. ..name..) => ..route..
      (provided
        ..doc.. =contains=> {:type ..type..}
        (..rt.. (keyword (format "%s-%s" ..type.. ..name..))) => ..route..)))

  (facts "`self-route`"
    (fact "provides self entity route"
      (r/self-route ..rt.. ..doc..) => ..route..
      (provided
        ..doc.. =contains=> {:type ..type..}
        (..rt.. (keyword ..type..)) => ..route..))))
