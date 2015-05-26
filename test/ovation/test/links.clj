(ns ovation.test.links
  (:use midje.sweet)
  (:require [ovation.links :as links]
            [ovation.couch :as couch])
  (:import (us.physion.ovation.data EntityDao$Views)))

(facts "About links"
  (facts "`get-link-targets`"
    (let [doc1 {:attributes {:label ..label1..}}
          doc2 {:attributes {:label ..label2..}}
          doc3 {:attributes {}}]
      (against-background [(couch/get-view ..db.. EntityDao$Views/LINKS {:startkey     [..id.. ..rel.. nil]
                                                                         :endkey       [..id.. ..rel.. {}]
                                                                         :reduce       false
                                                                         :include_docs true}) => [{:doc doc1} {:doc doc2} {:doc doc3}]]

        (fact "gets entity rel targets"
          (links/get-link-targets ..db.. ..id.. ..rel..) => [doc1 doc2 doc3])

        (fact "filters by label"
          (links/get-link-targets ..db.. ..id.. ..rel.. :label ..label1..) => [doc1]))))

  (facts "`add-link`"
    (future-fact "creates link document")
    (future-fact "updates entity :links")
    (future-fact "updates entity _collaboration_roots")
    (future-fact "fails if not can? :update"))

  (facts "`delete-link`"
    (future-fact "removes link")
    (future-fact "updates entity _collaboration_roots")
    (future-fact "fails if not can? :delete")))

;(facts "About links"
;  (fact "GET /links/:rel returns links from entity"
;    (links/get-link ...apikey... ...entity... ...rel...) => ...links...
;    (provided
;      (dao/get-entity ...apikey... ...entity...) => ...entity...
;      (links/get-entities ...entity... ...rel...) => ...links...
;      (dao/into-seq ...apikey... (into [] ...links...)) => ...links...))
;
;  (fact "POST /links adds a link with inverse"
;    (links/create-link ...api... ...id... {:target_id   ...target...
;                                           :rel         ...rel...
;                                           :inverse_rel ...inverse...}) => {:success true}
;    (provided
;      (dao/get-entity ...api... ...id...) => ...entity...
;      (links/add-link ...entity... ...rel... ...target... :inverse ...inverse...) => true))
;
;  (fact "POST /links adds a link without inverse"
;    (links/create-link ...api... ...id... {:target_id ...target...
;                                           :rel       ...rel...}) => {:success true}
;    (provided
;      (dao/get-entity ...api... ...id...) => ...entity...
;      (links/add-link ...entity... ...rel... ...target... :inverse nil) => true))
;
;  (fact "DELETE /links/:rel/:target deletes a link"
;    (links/delete-link ...api... ...id... ...rel... ...target...) => {:success true}
;    (provided
;      (dao/get-entity ...api... ...id...) => ...entity...
;      (links/remove-link ...entity... ...rel... ...target...) => true)))


;(facts "About named_links"
;  (fact "GET /named_links/:rel/:name returns named links from entity"
;    (links/get-named-link ...api... ...id... ...rel... ...named...) => ...entities...
;    (provided
;      (dao/get-entity ...api... ...id...) => ...entity...
;      (links/get-named-entities ...entity... ...rel... ...named...) => ...links...
;      (dao/into-seq ...api...  (into [] ...links...)) => ...entities...))
;
;  (fact "POST /named_links adds a link with inverse"
;    (links/create-named-link ...api... ...id... {:target_id   ...target...
;                                                 :inverse_rel ...inverse...
;                                                 :rel         ...rel...
;                                                 :name        ...name...}) => [...entity...]
;    (provided
;      (dao/get-entity ...api... ...id...) => ...entity...
;      (links/add-named-link ...entity... ...rel... ...name... ...target... :inverse ...inverse...) => true))
;
;  (fact "DELETE /named_links/:rel/:name/:target deletes a link"
;    (links/delete-named-link ...api... ...id... ...rel... ...named... ...target...) => {:success true}
;    (provided
;      (dao/get-entity ...api... ...id...) => ...entity...
;      (links/remove-named-link ...entity... ...rel... ...named... ...target...) => true)))

