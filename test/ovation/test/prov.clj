(ns ovation.test.prov
  (:use midje.sweet)
  (:require [ovation.prov :as prov]
            [ovation.constants :as k]
            [ovation.links :as links]
            [ovation.core :as core]))


(facts "About provenance"
  (facts "`local`"

    (fact "generates source provenance to downstream revisions"
      ;; source -> activity -> rev
      (prov/local ..ctx.. ..db.. [..source-id..]) => [{:_id        ..source-id..
                                                       :name       ..name..
                                                       :type       "Source"
                                                       :origins    []
                                                       :activities [{:_id ..activityid.. :name ..activity.. :type "Activity"}]}

                                                      {:_id     ..activityid..
                                                       :name    ..activity..
                                                       :type    "Activity"
                                                       :inputs  [{:_id ..source-id.. :type "Source" :name ..name..}]
                                                       :outputs [{:_id ..revid.. :type "Revision" :name ..rev..}]
                                                       :actions []
                                                       :operators []}

                                                      {:_id        ..revid..
                                                       :name       ..rev..
                                                       :type       "Revision"
                                                       :origins    [{:_id ..activityid.. :type "Activity" :name ..activity..}]
                                                       :activities []}]

      (provided
        (core/get-entities ..ctx.. ..db.. [..source-id..]) => [{:_id        ..source-id..
                                                                :type       "Source"
                                                                :attributes {:name ..name..}}]

        (links/get-link-targets ..ctx.. ..db.. ..source-id.. k/ACTIVITIES-REL) => [{:_id        ..activityid..
                                                                                    :type       "Activity"
                                                                                    :attributes {:name ..activity..}}]

        (links/get-link-targets ..ctx.. ..db.. ..source-id.. k/ORIGINS-REL) => []

        (links/get-link-targets ..ctx.. ..db.. ..activityid.. k/OUTPUTS-REL) => [{:_id        ..revid..
                                                                                  :type       "Revision"
                                                                                  :attributes {:name ..rev..}}]
        (links/get-link-targets ..ctx.. ..db.. ..activityid.. k/ACTIONS-REL) => []
        (links/get-link-targets ..ctx.. ..db.. ..activityid.. k/OPERATORS-REL) => []
        (links/get-link-targets ..ctx.. ..db.. ..activityid.. k/INPUTS-REL) => [{:_id        ..source-id..
                                                                                 :attributes {:name ..name..}
                                                                                 :type       "Source"}]

        (links/get-link-targets ..ctx.. ..db.. ..revid.. k/ACTIVITIES-REL) => []
        (links/get-link-targets ..ctx.. ..db.. ..revid.. k/ORIGINS-REL) => [{:_id        ..activityid..
                                                                             :type       "Activity"
                                                                             :attributes {:name ..activity..}}]
        ))

    (fact "generates rev provenance upstream and downstream"
      ;; upstream-source -> origin activity -> rev -> activity -> downstream rev
      (prov/local ..ctx.. ..db.. [..revid..]) => [{:_id        ..revid..
                                                   :name       ..rev..
                                                   :type       "Revision"
                                                   :origins    [{:_id ..originid.. :type "Activity" :name ..origin..}]
                                                   :activities [{:_id ..activityid.. :type "Activity" :name ..activity..}]}

                                                  {:_id     ..originid..
                                                   :name    ..origin..
                                                   :type    "Activity"
                                                   :inputs  [{:_id ..upstream-rev-id.. :type "Revision" :name ..upstream-rev..}]
                                                   :outputs [{:_id ..revid.. :type "Revision" :name ..rev..}]
                                                   :actions []
                                                   :operators []}

                                                  {:_id        ..upstream-rev-id..
                                                   :type       "Revision"
                                                   :name       ..upstream-rev..
                                                   :origins    []
                                                   :activities [{:_id ..originid.. :name ..origin.. :type "Activity"}]}


                                                  {:_id     ..activityid..
                                                   :name    ..activity..
                                                   :type    "Activity"
                                                   :inputs  [{:_id ..revid.. :type "Revision" :name ..rev..}]
                                                   :outputs [{:_id ..downstream-rev-id.. :type "Revision" :name ..downstream-rev..}]
                                                   :actions []
                                                   :operators []}

                                                  {:_id        ..downstream-rev-id..
                                                   :name       ..downstream-rev..
                                                   :type       "Revision"
                                                   :origins    [{:_id ..activityid.. :type "Activity" :name ..activity..}]
                                                   :activities []}

                                                  ]

      (provided
        (core/get-entities ..ctx.. ..db.. [..revid..]) => [{:_id        ..revid..
                                                            :type       "Revision"
                                                            :attributes {:name ..rev..}}]

        (links/get-link-targets ..ctx.. ..db.. ..revid.. k/ACTIVITIES-REL) => [{:_id        ..activityid..
                                                                                :type       "Activity"
                                                                                :attributes {:name ..activity..}}]

        (links/get-link-targets ..ctx.. ..db.. ..revid.. k/ORIGINS-REL) => [{:_id        ..originid..
                                                                             :type       "Activity"
                                                                             :attributes {:name ..origin..}}]


        (links/get-link-targets ..ctx.. ..db.. ..activityid.. k/OUTPUTS-REL) => [{:_id        ..downstream-rev-id..
                                                                                  :type       "Revision"
                                                                                  :attributes {:name ..downstream-rev..}}]

        (links/get-link-targets ..ctx.. ..db.. ..activityid.. k/ACTIONS-REL) => []
        (links/get-link-targets ..ctx.. ..db.. ..activityid.. k/OPERATORS-REL) => []
        (links/get-link-targets ..ctx.. ..db.. ..activityid.. k/INPUTS-REL) => [{:_id        ..revid..
                                                                                 :type       "Revision"
                                                                                 :attributes {:name ..rev..}}]



        (links/get-link-targets ..ctx.. ..db.. ..upstream-rev-id.. k/ORIGINS-REL) => []
        (links/get-link-targets ..ctx.. ..db.. ..upstream-rev-id.. k/ACTIVITIES-REL) => [{:_id        ..originid..
                                                                                          :type       "Activity"
                                                                                          :attributes {:name ..origin..}}]


        (links/get-link-targets ..ctx.. ..db.. ..downstream-rev-id.. k/ORIGINS-REL) => [{:_id        ..activityid..
                                                                                         :type       "Activity"
                                                                                         :attributes {:name ..activity..}}]
        (links/get-link-targets ..ctx.. ..db.. ..downstream-rev-id.. k/ACTIVITIES-REL) => []


        (links/get-link-targets ..ctx.. ..db.. ..originid.. k/INPUTS-REL) => [{:_id        ..upstream-rev-id..
                                                                               :type       "Revision"
                                                                               :attributes {:name ..upstream-rev..}}]

        (links/get-link-targets ..ctx.. ..db.. ..originid.. k/ACTIONS-REL) => []
        (links/get-link-targets ..ctx.. ..db.. ..originid.. k/OPERATORS-REL) => []
        (links/get-link-targets ..ctx.. ..db.. ..originid.. k/OUTPUTS-REL) => [{:_id        ..revid..
                                                                                :type       "Revision"
                                                                                :attributes {:name ..rev..}}]))
    )


  (facts "`global`"
    (fact "generates project provenance"
      (prov/global ..ctx.. ..db.. [..project..]) => [{:_id     ..ida1..
                                                      :name    ..namea1..
                                                      :type    "Activity"
                                                      :inputs  [{:_id ..idi1.. :type ..tpi1.. :name ..namei1..}
                                                                {:_id ..idi2.. :type ..tpi2.. :name ..namei2..}]
                                                      :actions [{:_id ..idia1.. :type ..tpia1.. :name ..nameia1..}
                                                                {:_id ..idia2.. :type ..tpia2.. :name ..nameia2..}]
                                                      :outputs [{:_id ..ido1.. :type ..tpo1.. :name ..nameo1..}
                                                                {:_id ..ido2.. :type ..tpo2.. :name ..nameo2..}]
                                                      :operators [{:_id ..idu1.. :type ..tpu1.. :name ..nameu1..}]}

                                                     {:_id     ..ida2..
                                                      :name    ..namea2..
                                                      :type    "Activity"
                                                      :inputs  [{:_id ..idi3.. :type ..tpi3.. :name ..namei3..}
                                                                {:_id ..idi4.. :type ..tpi4.. :name ..namei4..}]
                                                      :actions [{:_id ..idia3.. :type ..tpia3.. :name ..nameia3..}
                                                                {:_id ..idia4.. :type ..tpia4.. :name ..nameia4..}]
                                                      :outputs [{:_id ..ido3.. :type ..tpo3.. :name ..nameo3..}
                                                                {:_id ..ido4.. :type ..tpo4.. :name ..nameo4..}]
                                                      :operators [{:_id ..idu2.. :type ..tpu2.. :name ..nameu2..}]}]
      (provided
        (links/get-link-targets ..ctx.. ..db.. ..project.. k/ACTIVITIES-REL) => [{:_id        ..ida1..
                                                                                  :type       "Activity"
                                                                                  :attributes {:name ..namea1..}}
                                                                                 {:_id        ..ida2..
                                                                                  :type       "Activity"
                                                                                  :attributes {:name ..namea2..}}]

        (links/get-link-targets ..ctx.. ..db.. ..ida1.. k/INPUTS-REL) => [{:_id ..idi1.. :type ..tpi1.. :attributes {:name ..namei1..}}
                                                                          {:_id ..idi2.. :type ..tpi2.. :attributes {:name ..namei2..}}]
        (links/get-link-targets ..ctx.. ..db.. ..ida1.. k/OUTPUTS-REL) => [{:_id ..ido1.. :type ..tpo1.. :attributes {:name ..nameo1..}}
                                                                           {:_id ..ido2.. :type ..tpo2.. :attributes {:name ..nameo2..}}]
        (links/get-link-targets ..ctx.. ..db.. ..ida1.. k/ACTIONS-REL) => [{:_id ..idia1.. :type ..tpia1.. :attributes {:name ..nameia1..}}
                                                                           {:_id ..idia2.. :type ..tpia2.. :attributes {:name ..nameia2..}}]
        (links/get-link-targets ..ctx.. ..db.. ..ida1.. k/OPERATORS-REL) => [{:_id ..idu1.. :type ..tpu1.. :attributes {:name ..nameu1..}}]

        (links/get-link-targets ..ctx.. ..db.. ..ida2.. k/INPUTS-REL) => [{:_id ..idi3.. :type ..tpi3.. :attributes {:name ..namei3..}}
                                                                          {:_id ..idi4.. :type ..tpi4.. :attributes {:name ..namei4..}}]
        (links/get-link-targets ..ctx.. ..db.. ..ida2.. k/OUTPUTS-REL) => [{:_id ..ido3.. :type ..tpo3.. :attributes {:name ..nameo3..}}
                                                                           {:_id ..ido4.. :type ..tpo4.. :attributes {:name ..nameo4..}}]
        (links/get-link-targets ..ctx.. ..db.. ..ida2.. k/ACTIONS-REL) => [{:_id ..idia3.. :type ..tpia3.. :attributes {:name ..nameia3..}}
                                                                           {:_id ..idia4.. :type ..tpia4.. :attributes {:name ..nameia4..}}]
        (links/get-link-targets ..ctx.. ..db.. ..ida2.. k/OPERATORS-REL) => [{:_id ..idu2.. :type ..tpu2.. :attributes {:name ..nameu2..}}]))))
