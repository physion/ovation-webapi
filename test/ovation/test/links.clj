(ns ovation.test.links
  (:use midje.sweet)
  (:require [ovation.links :as links]
            [ovation.couch :as couch]
            [ovation.util :as util]
            [ovation.auth :as auth]
            [ovation.core :as core])
  (:import (us.physion.ovation.data EntityDao$Views)
           (java.util UUID)))

(facts "About links"
  (facts "`get-link-targets`"
    (let [doc1 {:attributes {:label ..label1..}}
          doc2 {:attributes {:label ..label2..}}
          doc3 {:attributes {}}]
      (against-background [(couch/get-view ..db.. EntityDao$Views/LINKS {:key          [..id.. ..rel..]
                                                                         :reduce       false
                                                                         :include_docs true}) => [{:doc doc1} {:doc doc2} {:doc doc3}]
                           (couch/get-view ..db.. EntityDao$Views/LINKS {:key          [..id.. ..rel.. ..name..]
                                                                         :reduce       false
                                                                         :include_docs true}) => [{:doc doc1}]
                           (auth/can? anything :auth/update anything) => true]

        (fact "gets entity rel targets"
          (links/get-link-targets ..db.. ..id.. ..rel..) => [doc1 doc2 doc3])

        (fact "gets named entity rel targets"
          (links/get-link-targets ..db.. ..id.. ..rel.. :name ..name..) => [doc1])

        (fact "filters by label"
          (links/get-link-targets ..db.. ..id.. ..rel.. :label ..label1..) => [doc1])))))

(facts "`add-link`"
  (let [doc {:_id       (str (UUID/randomUUID))
             :attrbutes {}
             :links     {:_collaboration_roots []}}
        target-id (str (UUID/randomUUID))]

    (against-background [(couch/db ..auth..) => ..db..
                         (core/get-entities ..auth.. [target-id]) => [{:type  "not-a-root"
                                                                     :links {:_collaboration_roots []}}]]


      (fact "creates link document"
        (links/add-link ..auth.. doc ..id.. ..rel.. target-id :inverse-rel ..inverse..) => (contains {:_id         (format "%s--%s-->%s" (:_id doc) ..rel.. target-id)
                                                                                                      :user_id     ..id..
                                                                                                      :source_id   (:_id doc)
                                                                                                      :target_id   target-id
                                                                                                      :rel         ..rel..
                                                                                                      :inverse_rel ..inverse..}))

      (fact "creates link document without inverse"
        (links/add-link ..auth.. doc ..id.. ..rel.. target-id) => (contains {:_id       (format "%s--%s-->%s" (:_id doc) ..rel.. target-id)
                                                                             :user_id   ..id..
                                                                             :source_id (:_id doc)
                                                                             :target_id target-id
                                                                             :rel       ..rel..}))
      (fact "creates named link document"
        (links/add-link ..auth.. doc ..id.. ..rel.. target-id :inverse-rel ..inverse.. :name ..name..) => (contains {:_id         (format "%s--%s>%s-->%s" (:_id doc) ..rel.. ..name.. target-id)
                                                                                                                     :user_id     ..id..
                                                                                                                     :source_id   (:_id doc)
                                                                                                                     :target_id   target-id
                                                                                                                     :name        ..name..
                                                                                                                     :inverse_rel ..inverse..
                                                                                                                     :rel         ..rel..}))

      (fact "updates entity :links"
        (let [rel "some-rel"
              link-path (util/join-path ["" "entities" (:_id doc) "links" rel])]
          (links/add-link ..auth.. doc ..id.. rel target-id) => (contains (assoc-in doc [:links (keyword rel)] link-path))))

      (fact "updates entity :named_links"
        (let [rel "some-rel"
              relname "my-name"
              link-path (util/join-path ["" "entities" (:_id doc) "named_links" rel relname])
              result (links/add-link ..auth.. doc ..id.. rel target-id :name relname)
              expected (assoc-in doc [:named_links (keyword rel) (keyword relname)] link-path)]

          result => (contains expected)))

      (fact "fails if not can? :update"
        (links/add-link ..auth.. doc ..id.. ..rel.. target-id) => (throws Exception)
        (provided
          (auth/can? ..id.. :auth/update anything) => false))

      (fact "updates source _collaboration_roots"
        (let [rel "some-rel"
              link-path (util/join-path ["" "entities" (:_id doc) "links" rel])]
          (links/add-link ..auth.. doc ..id.. rel target-id) => (contains (-> doc
                                                                            (assoc-in [:links (keyword rel)] link-path)
                                                                            (assoc-in [:links :_collaboration_roots] [..roots..])))
          (provided
            (core/get-entities ..auth.. [target-id]) => [{:type "Project" :links {:_collaboration_roots [..roots..]}}])))

      (fact "updates target _collaboration_roots"
        (let [rel "some-rel"
              target {:type "Project" :links {:_collaboration_roots [..roots..]}}]
          (links/add-link ..auth.. doc ..id.. rel target-id) => (contains (-> target
                                                                            (assoc-in [:links :_collaboration_roots] [..roots..])))
          (provided
            (core/get-entities ..auth.. [target-id]) => [target]))))))

(facts "`delete-link`"
  (future-fact "removes link")
  (future-fact "updates entity _collaboration_roots")
  (future-fact "fails if not can? :delete"))
