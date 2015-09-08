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
        records (core/create-entities auth [doc] routes)
        {input-links   :links
         input-updates :updates} (links/add-links auth records INPUTS (:inputs analysis) routes :strict true :required-target-types [REQUIRED_INPUT_TYPE])
        {output-links   :links
         output-updates :updates} (links/add-links auth records OUTPUTS (:outputs analysis) routes :strict true :required-target-types [REQUIRED_OUTPUT_TYPE])
        links (core/create-values auth routes (concat input-links output-links))
        updates (core/update-entities auth (concat input-updates output-updates) routes)]

    {:links links
     :analyses records
     :updates updates}))
