(ns ovation.test.breadcrumbs
  (:use midje.sweet)
  (:require [ovation.constants :as k]
            [ovation.breadcrumbs :as b]
            [ovation.links :as links]))


(facts "About parents"
  (facts "all entities"
    (fact "is parents"
      (b/get-parents ..auth.. ..rt.. {:_id ..id..}) => ..parents..
      (provided
        (links/get-link-targets ..auth.. ..id.. "parents" ..rt..) => ..parents..))))

(facts "About breadcrumbs"
  (against-background [(links/get-link-targets ..auth.. ..file1.. k/PARENTS-REL ..rt..) => [{:_id ..folder1..} {:_id ..folder2..}]
                       (links/get-link-targets ..auth.. ..file2.. k/PARENTS-REL ..rt..) => [{:_id ..folder2..}]
                       (links/get-link-targets ..auth.. ..folder1.. k/PARENTS-REL ..rt..) => [{:_id ..project1..}]
                       (links/get-link-targets ..auth.. ..folder2.. k/PARENTS-REL ..rt..) => [{:_id ..project1..} {:_id ..project2..}]]
    (fact "calculates file breadcrumbs"
      (b/get-breadcrumbs ..auth.. ..rt.. [..file1.. ..file2..]) => {..file1.. [{..folder1.. [{..project1.. []}]}
                                                                               {..folder2.. [{..project1.. []} {..project2.. []}]}]
                                                                    ..file2.. [{..folder2.. [{..project1.. []} {..project2.. []}]}]})))
