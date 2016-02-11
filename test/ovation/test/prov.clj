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
      (prov/local ..auth.. ..rt.. [..source-id..]) => [{:_id     ..activityid..
                                                        :name    ..activity..
                                                        :type    "Activity"
                                                        :inputs  [{:_id ..source-id.. :type "Source" :name ..name..}]
                                                        :outputs [{:_id ..rev-id.. :type "Revision" :name ..rev..}]
                                                        :actions []}

                                                       {:_id       ..revid..
                                                        :name      ..rev..
                                                        :type      "Revision"
                                                        :origins   [{:_id ..activityid.. :type "Activity :name ..activity"}]
                                                        :activties []}]

      (provided
        (core/get-entities ..auth.. [..source-id..] ..rt..) => [{:_id        ..source-id..
                                                                 :type       "Source"
                                                                 :attributes {:name ..name..}}]

        (links/get-link-targets ..auth.. ..source-id.. k/ACTIVITIES-REL ..rt..) => [{:_id        ..activityid..
                                                                                     :type       "Activity"
                                                                                     :attributes {:name ..activity..}}]

        (links/get-link-targets ..auth.. ..source-id.. k/ORIGINS-REL ..rt..) => []

        (links/get-link-targets ..auth.. ..activityid.. k/OUTPUTS-REL ..rt..) => [{:_id        ..revid..
                                                                                   :type       "Revision"
                                                                                   :attributes {:name ..rev..}}]
        (links/get-link-targets ..auth.. ..activityid.. k/ACTIONS-REL ..rt..) => []
        (links/get-link-targets ..auth.. ..activityid.. k/INPUTS-REL ..rt..) => []

        (links/get-link-targets ..auth.. ..revid.. k/ACTIVITIES-REL ..rt..) => []
        (links/get-link-targets ..auth.. ..revid.. k/ORIGINS-REL ..rt..) => [{:_id        ..activityid..
                                                                              :type       "Activity"
                                                                              :attributes {:name ..activity..}}]
        ))

    (fact "generates rev provenance upstream and downstream"
      ;; upstream-source -> origin activity -> rev -> activity -> downstream rev
      (prov/local ..auth.. ..rt.. [..revid..]) => [{:_id     ..activityid..
                                                    :name    ..activity..
                                                    :type    "Activity"
                                                    :inputs  [{:_id ..source-id.. :type "Source" :name ..name..}]
                                                    :outputs [{:_id ..downstream-rev-id.. :type "Revision" :name ..downstream-rev..}]
                                                    :actions []}

                                                   {:_id       ..downstream-revid..
                                                    :name      ..downstreaml..
                                                    :type      "Revision"
                                                    :origins   [{:_id ..activityid.. :type "Activity" :name ..activity..}]
                                                    :activties []}

                                                   {:_id     ..originid..
                                                    :name    ..origin..
                                                    :type    "Activity"
                                                    :inputs  [{:_id ..upstream-rev-id.. :type "Revision" :name ..upstreamid..}]
                                                    :outputs [{:_id ..revid.. :type "Revision" :name ..rev..}]
                                                    :actions []}

                                                   {:_id ..upstream-rev-id.. :type "Revision" :name ..upstreamid..}]

      (provided
        (core/get-entities ..auth.. [..revid..] ..rt..) => [{:_id        ..revid..
                                                             :type       "Revision"
                                                             :attributes {:name ..rev..}}]

        (links/get-link-targets ..auth.. ..revid.. k/ACTIVITIES-REL ..rt..) => [{:_id        ..activityid..
                                                                                 :type       "Activity"
                                                                                 :attributes {:name ..activity..}}]

        (links/get-link-targets ..auth.. ..revid.. k/ORIGINS-REL ..rt..) => [{:_id        ..originid..
                                                                              :type       "Activity"
                                                                              :attributes {:name ..origin..}}]


        (links/get-link-targets ..auth.. ..activityid.. k/OUTPUTS-REL ..rt..) => [{:_id        ..downstream-rev-id..
                                                                                   :type       "Revision"
                                                                                   :attributes {:name ..downstream-rev..}}]

        (links/get-link-targets ..auth.. ..activityid.. k/ACTIONS-REL ..rt..) => []
        (links/get-link-targets ..auth.. ..activityid.. k/INPUTS-REL ..rt..) => [{:_id        ..revid..
                                                                                  :type       "Revision"
                                                                                  :attributes {:name ..rev..}}]



        (links/get-link-targets ..auth.. ..upstream-rev-id.. k/ORIGINS-REL ..rt..) => []
        (links/get-link-targets ..auth.. ..upstream-rev-id.. k/ACTIVITIES-REL ..rt..) => [{:_id        ..originid..
                                                                                           :type       "Activity"
                                                                                           :attributes {:name ..origin..}}]


        (links/get-link-targets ..auth.. ..downstream-rev-id.. k/ORIGINS-REL ..rt..) => [{:_id        ..originid..
                                                                                          :type       "Activity"
                                                                                          :attributes {:name ..origin..}}]
        (links/get-link-targets ..auth.. ..downstream-rev-id.. k/ACTIVITIES-REL ..rt..) => []


        (links/get-link-targets ..auth.. ..originid.. k/INPUTS-REL ..rt..) => [{:_id        ..upstream-rev-id..
                                                                                :type       "Revision"
                                                                                :attributes {:name ..upstream-rev..}}]

        (links/get-link-targets ..auth.. ..originid.. k/ACTIONS-REL ..rt..) => []
        (links/get-link-targets ..auth.. ..originid.. k/OUTPUTS-REL ..rt..) => [{:_id        ..revid..
                                                                                 :type       "Revision"
                                                                                 :attributes {:name ..rev..}}]))
    )


  (facts "`global`"
    (fact "generates project provenance"
      (prov/global ..auth.. ..rt.. [..project..]) => [{:_id     ..ida1..
                                                       :name    ..namea1..
                                                       :type    "Activity"
                                                       :inputs  [{:_id ..idi1.. :type ..tpi1.. :name ..namei1..}
                                                                 {:_id ..idi2.. :type ..tpi2.. :name ..namei2..}]
                                                       :actions [{:_id ..idia1.. :type ..tpia1.. :name ..nameia1..}
                                                                 {:_id ..idia2.. :type ..tpia2.. :name ..nameia2..}]
                                                       :outputs [{:_id ..ido1.. :type ..tpo1.. :name ..nameo1..}
                                                                 {:_id ..ido2.. :type ..tpo2.. :name ..nameo2..}]}

                                                      {:_id     ..ida2..
                                                       :name    ..namea2..
                                                       :type    "Activity"
                                                       :inputs  [{:_id ..idi3.. :type ..tpi3.. :name ..namei3..}
                                                                 {:_id ..idi4.. :type ..tpi4.. :name ..namei4..}]
                                                       :actions [{:_id ..idia3.. :type ..tpia3.. :name ..nameia3..}
                                                                 {:_id ..idia4.. :type ..tpia4.. :name ..nameia4..}]
                                                       :outputs [{:_id ..ido3.. :type ..tpo3.. :name ..nameo3..}
                                                                 {:_id ..ido4.. :type ..tpo4.. :name ..nameo4..}]}]
      (provided
        (links/get-link-targets ..auth.. ..project.. k/ACTIVITIES-REL ..rt..) => [{:_id        ..ida1..
                                                                                   :type       "Activity"
                                                                                   :attributes {:name ..namea1..}}
                                                                                  {:_id        ..ida2..
                                                                                   :type       "Activity"
                                                                                   :attributes {:name ..namea2..}}]

        (links/get-link-targets ..auth.. ..ida1.. k/INPUTS-REL ..rt..) => [{:_id ..idi1.. :type ..tpi1.. :attributes {:name ..namei1..}}
                                                                           {:_id ..idi2.. :type ..tpi2.. :attributes {:name ..namei2..}}]
        (links/get-link-targets ..auth.. ..ida1.. k/OUTPUTS-REL ..rt..) => [{:_id ..ido1.. :type ..tpo1.. :attributes {:name ..nameo1..}}
                                                                            {:_id ..ido2.. :type ..tpo2.. :attributes {:name ..nameo2..}}]
        (links/get-link-targets ..auth.. ..ida1.. k/ACTIONS-REL ..rt..) => [{:_id ..idia1.. :type ..tpia1.. :attributes {:name ..nameia1..}}
                                                                            {:_id ..idia2.. :type ..tpia2.. :attributes {:name ..nameia2..}}]

        (links/get-link-targets ..auth.. ..ida2.. k/INPUTS-REL ..rt..) => [{:_id ..idi3.. :type ..tpi3.. :attributes {:name ..namei3..}}
                                                                           {:_id ..idi4.. :type ..tpi4.. :attributes {:name ..namei4..}}]
        (links/get-link-targets ..auth.. ..ida2.. k/OUTPUTS-REL ..rt..) => [{:_id ..ido3.. :type ..tpo3.. :attributes {:name ..nameo3..}}
                                                                            {:_id ..ido4.. :type ..tpo4.. :attributes {:name ..nameo4..}}]
        (links/get-link-targets ..auth.. ..ida2.. k/ACTIONS-REL ..rt..) => [{:_id ..idia3.. :type ..tpia3.. :attributes {:name ..nameia3..}}
                                                                            {:_id ..idia4.. :type ..tpia4.. :attributes {:name ..nameia4..}}]))))
