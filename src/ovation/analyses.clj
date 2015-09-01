(ns ovation.analyses
  (:require [ovation.core :as core]
            [ovation.util :as util]
            [ovation.links :as links]
            [slingshot.slingshot :refer [try+]]
            [ring.util.http-response :refer [created ok accepted no-content not-found throw! unauthorized bad-request]]))

(def ANALYSIS_RECORD_TYPE "AnalysisRecord")
(def REQUIRED_INPUT_TYPE "Revision")
(def REQUIRED_OUTPUT_TYPE "Revision")
(def INPUTS "inputs")
(def OUTPUTS "outputs")


(defn create-analysis-record
  [auth analysis routes]
  (let [doc {:type       ANALYSIS_RECORD_TYPE
             :attributes (if-let [params (:parameters analysis)]
                           {:parameters params}
                           {})}
        records (core/create-entity auth [doc] routes)
        inputs (links/add-links auth records INPUTS (:inputs analysis) routes :strict true :required-target-types [REQUIRED_INPUT_TYPE])
        input-records (util/filter-type ANALYSIS_RECORD_TYPE (core/update-entity auth (:all inputs) routes))
        outputs (links/add-links auth input-records OUTPUTS (:outputs analysis) routes :strict true :required-target-types [REQUIRED_OUTPUT_TYPE])
        updated-records (util/filter-type ANALYSIS_RECORD_TYPE (core/update-entity auth (:all outputs) routes))]

    updated-records))
