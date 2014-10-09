(ns ovation-rest.test.links
  (:use midje.sweet)
  (:import (us.physion.ovation.domain DtoBuilder URIs)
           (java.util UUID))
  (:require [ovation-rest.links :as links]
            [ovation-rest.util :as util]
            [ovation-rest.entity :as entity]))


(facts "About links"
  (fact "GET /links/:rel returns links from entity"
    (links/get-link ...apikey... ...entity... ...rel...) => ...links...
    (provided
      (entity/get-entity ...apikey... ...entity...) => ...entity...
      (links/get-entities ...entity... ...rel...) => ...links...
      (util/into-seq (into [] ...links...)) => ...links...))

  (fact "POST /links/:rel adds a link with inverse"
    (links/create-link ...api... ...id... ...rel... {:target_id   ...target...
                                                     :inverse_rel ...inverse...}) => [...entity...]
    (provided
      (entity/get-entity ...api... ...id...) => ...entity...
      (links/add-link ...entity... ...rel... ...target... :inverse ...inverse...) => true))

  (fact "POST /links/:rel adds a link without inverse"
    (links/create-link ...api... ...id... ...rel... {:target_id   ...target...}) => [...entity...]
    (provided
      (entity/get-entity ...api... ...id...) => ...entity...
      (links/add-link ...entity... ...rel... ...target... :inverse nil) => true))


  (fact "DELETE /links/:rel/:link-id deletes a link"
    (links/delete-link ...api... ...id... ...rel... ...target...) => {:success true}
    (provided
      (entity/get-entity ...api... ...id...) => ...entity...
      (links/remove-link ...entity... ...rel... ...target...) => true)))


(facts "About named_links"
  )

