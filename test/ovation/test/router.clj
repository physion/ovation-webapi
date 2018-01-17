(ns ovation.test.router
  (:use midje.sweet)
  (:require [ovation.routes :as routes]
            [ovation.util :as util]
            [ovation.constants :as c]
            [ovation.request-context :as request-context]))

(defn rt
  [& args]
  args)

(against-background [..ctx.. =contains=> {::request-context/org    ..org..
                                          ::request-context/routes rt}
                     ..doc.. =contains=> {:_id ..id..}
                     (util/entity-type-name ..doc..) => ..type..]
  (facts "About routes"

    (facts "relationship-route"
      (fact "provides relationship route for doc and name"
        (routes/relationship-route ..ctx.. ..doc.. ..name..) => [(keyword (format "get-%s-links" ..type..)) {:id ..id.., :org ..org.., :rel ..name..}]))

    (facts "target-route"
      (fact "provides targets route for doc and name"
        (routes/targets-route ..ctx.. ..doc.. ..name..) => [(keyword (format "get-%s-link-targets" ..type..)) {:org ..org.. :id ..id.. :rel ..name..}]))

    (facts "self-route"
      (fact "provides self entity route"
        (let [type "mytype"]
          (routes/self-route ..ctx.. ..doc..) => [(keyword (format "get-%s" type)) {:org ..org.. :id ..id..}]
          (provided
            (util/entity-type-name ..doc..) => type)))

      (fact "provides self route for Relationship"
        (let [type c/RELATION-TYPE]
          (routes/self-route ..ctx.. ..doc..) => [(keyword (format "get-%s" type)) {:org ..org.. :id ..id..}]
          (provided
            (util/entity-type-name ..doc..) => type))))))
