(ns ovation.test.router
  (:use midje.sweet)
  (:require [ovation.routes :as r]
            [ovation.util :as util]))

(facts "About routes"
  (fact "`relationship-route`"
    (fact "provides relationship route for doc and name"
      (r/relationship-route ..rt.. ..doc.. ..name..) => ..route..
      (provided
        ..doc.. =contains=> {:_id ..id..}
        (util/entity-type-name ..doc..) => ..type..
        (..rt.. (keyword (format "get-%s-links" ..type..)) {:id ..id.. :rel ..name..}) => ..route..)))

  (facts "`target-route`"
    (fact "provides targets route for doc and name"
      (r/targets-route ..rt.. ..doc.. ..name..) => ..route..
      (provided
        ..doc.. =contains=> {:_id ..id..}
        (util/entity-type-name ..doc..) => ..type..
        (..rt.. (keyword (format "get-%s-link-targets" ..type..)) {:id ..id.. :rel ..name..}) => ..route..)))

  (facts "`self-route`"
    (fact "provides self entity route"
      (r/self-route ..rt.. ..doc..) => ..route..
      (provided
        ..doc.. =contains=> {:_id ..id..}
        (util/entity-type-name ..doc..) => ..type..
        (..rt.. (keyword (format "get-%s" ..type..)) {:id ..id..}) => ..route..)))
  )
