(ns ovation-rest.test.entity
  (:use midje.sweet)
  (:import (us.physion.ovation.domain DtoBuilder URIs)
           (java.util UUID))
  (:require [ovation-rest.util :as util]
            [ovation-rest.interop :refer [clojurify]]
            [ovation-rest.entity :as entity]))



;(facts "about entity handlers"
;       (facts "index-resource"
;              (fact "gets projects"
;                    (entity/index-resource ...apikey... "projects" ...hosturl...) => ...result...
;                    (provided
;                      (util/ctx ...apikey...) => ...ctx...
;                      (#'ovation-rest.entity/get-projects ...ctx...) => ...entities...
;                      (util/into-seq ...entities... ...hosturl...) => ...result...)))
;       (facts "get-view"
;              (fact "gets view results"
;                    (entity/get-view ...apikey... ...requesturl... ...hosturl...) => ...result...
;                    (provided
;                      (util/ctx ...apikey...) => ...ctx...
;                      (util/to-ovation-uri ...requesturl... ...hosturl...) => ...viewuri...
;                      (#'ovation-rest.entity/get-view-results ...ctx... ...viewuri...) => ...entities...
;                      (util/into-seq ...entities... ...hosturl...) => ...result...))))
