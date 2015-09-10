(ns ovation.test.links
  (:use midje.sweet)
  (:require [ovation.links :as links]
            [ovation.dao :as dao]))


(facts "About links"
  (fact "GET /links/:rel returns links from entity"
    (links/get-link ...apikey... ...entity... ...rel...) => ...links...
    (provided
      (dao/get-entity ...apikey... ...entity...) => ...entity...
      (links/get-entities ...entity... ...rel...) => ...links...
      (dao/into-seq ...apikey... (into [] ...links...)) => ...links...))

  (fact "POST /links adds a link with inverse"
    (links/create-link ...api... ...id... {:target_id   ...target...
                                           :rel         ...rel...
                                           :inverse_rel ...inverse...}) => {:success true}
    (provided
      (dao/get-entity ...api... ...id...) => ...entity...
      (links/add-link ...entity... ...rel... ...target... :inverse ...inverse...) => true))

  (fact "POST /links adds a link without inverse"
    (links/create-link ...api... ...id... {:target_id ...target...
                                           :rel       ...rel...}) => {:success true}
    (provided
      (dao/get-entity ...api... ...id...) => ...entity...
      (links/add-link ...entity... ...rel... ...target... :inverse nil) => true))

  (fact "DELETE /links/:rel/:target deletes a link"
    (links/delete-link ...api... ...id... ...rel... ...target...) => {:success true}
    (provided
      (dao/get-entity ...api... ...id...) => ...entity...
      (links/remove-link ...entity... ...rel... ...target...) => true)))


(facts "About named_links"
  (fact "GET /named_links/:rel/:name returns named links from entity"
    (links/get-named-link ...api... ...id... ...rel... ...named...) => ...entities...
    (provided
      (dao/get-entity ...api... ...id...) => ...entity...
      (links/get-named-entities ...entity... ...rel... ...named...) => ...links...
      (dao/into-seq ...api...  (into [] ...links...)) => ...entities...))

  (fact "POST /named_links adds a link with inverse"
    (links/create-named-link ...api... ...id... {:target_id   ...target...
                                                 :inverse_rel ...inverse...
                                                 :rel         ...rel...
                                                 :name        ...name...}) => [...entity...]
    (provided
      (dao/get-entity ...api... ...id...) => ...entity...
      (links/add-named-link ...entity... ...rel... ...name... ...target... :inverse ...inverse...) => true))

  (fact "DELETE /named_links/:rel/:name/:target deletes a link"
    (links/delete-named-link ...api... ...id... ...rel... ...named... ...target...) => {:success true}
    (provided
      (dao/get-entity ...api... ...id...) => ...entity...
      (links/remove-named-link ...entity... ...rel... ...named... ...target...) => true)))

