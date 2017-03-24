(ns ovation.test.router
  (:use midje.sweet)
  (:require [ovation.routes :as r]
            [ovation.util :as util]
            [ovation.request-context :as request-context]))

(against-background [..ctx.. =contains=> {::request-context/org    ..org..
                                          ::request-context/routes ..rt..}]
  (facts "About routes"
    (fact "`relationship-route`"
      (fact "provides relationship route for doc and name"
        (r/relationship-route ..ctx.. ..doc.. ..name..) => ..route..
        (provided
          ..doc.. =contains=> {:_id ..id..}
          (util/entity-type-name ..doc..) => ..type..
          (..rt.. (keyword (format "get-%s-links" ..type..)) {:org ..org.. :id ..id.. :rel ..name..}) => ..route..)))

    (facts "`target-route`"
      (fact "provides targets route for doc and name"
        (r/targets-route ..ctx.. ..doc.. ..name..) => ..route..
        (provided
          ..doc.. =contains=> {:_id ..id..}
          (util/entity-type-name ..doc..) => ..type..
          (..rt.. (keyword (format "get-%s-link-targets" ..type..)) {:org ..org.. :id ..id.. :rel ..name..}) => ..route..)))

    (facts "`self-route`"
      (fact "provides self entity route"
        (let [type "mytype"]
          (r/self-route ..ctx.. ..doc..) => ..route..
          (provided
            ..doc.. =contains=> {:_id ..id..}
            (util/entity-type-name ..doc..) => type
            (..rt.. (keyword (format "get-%s" type)) {:org ..org.. :id ..id..}) => ..route..)))
      (fact "provides self route for Relationship"
        (let [type util/RELATION_TYPE]
          (r/self-route ..ctx.. ..doc..) => ..route..
          (provided
            ..doc.. =contains=> {:_id ..id.. :source_id ..src..}
            (util/entity-type-name ..doc..) => type
            (..rt.. (keyword (format "get-%s" type)) {:org ..org.. :id ..id..}) => ..route..))))))
