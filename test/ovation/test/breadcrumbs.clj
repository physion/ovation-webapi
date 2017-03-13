(ns ovation.test.breadcrumbs
  (:use midje.sweet)
  (:require [ovation.constants :as k]
            [ovation.breadcrumbs :as b]
            [ovation.links :as links]
            [ovation.core :as core]))


(facts "About parents"
  (facts "all entities"
    (fact "is parents"
      (b/get-parents ..ctx.. ..db.. ..id..) => ..parents..
      (provided
        (links/get-link-targets ..ctx.. ..db.. ..id.. "parents") => ..parents..))))

(facts "About breadcrumbs"
  (against-background [(b/get-parents ..ctx.. ..db.. ..file1..) => [{:_id ..folder1..} {:_id ..folder2..}]
                       (b/get-parents ..ctx.. ..db.. ..file2..) => [{:_id ..folder2..}]
                       (b/get-parents ..ctx.. ..db.. ..folder1..) => [{:_id ..project1..}]
                       (b/get-parents ..ctx.. ..db.. ..folder2..) => [{:_id ..project1..} {:_id ..project2..}]
                       (b/get-parents ..ctx.. ..db.. ..project1..) => []
                       (b/get-parents ..ctx.. ..db.. ..project2..) => []
                       (core/get-entities ..ctx.. ..db.. #{..file1.. ..folder1.. ..folder2.. ..file2.. ..project1.. ..project2..}) => [{:_id        ..file1..
                                                                                                                                        :type       k/FILE-TYPE
                                                                                                                                        :attributes {:name ..filename1..}}
                                                                                                                                       {:_id        ..file2..
                                                                                                                                        :type       k/FILE-TYPE
                                                                                                                                        :attributes {:name ..filename2..}}
                                                                                                                                       {:_id        ..folder1..
                                                                                                                                        :type       k/FOLDER-TYPE
                                                                                                                                        :attributes {:name ..foldername1..}}
                                                                                                                                       {:_id        ..folder2..
                                                                                                                                        :type       k/FOLDER-TYPE
                                                                                                                                        :attributes {:name ..foldername2..}}
                                                                                                                                       {:_id        ..project1..
                                                                                                                                        :type       k/PROJECT-TYPE
                                                                                                                                        :attributes {:name ..projectname1..}}
                                                                                                                                       {:_id        ..project2..
                                                                                                                                        :type       k/PROJECT-TYPE
                                                                                                                                        :attributes {:name ..projectname2..}}]]

    (fact "calculates file breadcrumbs"
      (b/get-breadcrumbs ..ctx.. ..db.. ..org.. [..file1.. ..file2..]) => {..file1.. [[{:type k/FILE-TYPE :id ..file1.. :name ..filename1..}
                                                                                       {:type k/FOLDER-TYPE :id ..folder1.. :name ..foldername1..}
                                                                                       {:type k/PROJECT-TYPE :id ..project1.. :name ..projectname1..}]
                                                                                      [{:type k/FILE-TYPE :id ..file1.. :name ..filename1..}
                                                                                       {:type k/FOLDER-TYPE :id ..folder2.. :name ..foldername2..}
                                                                                       {:type k/PROJECT-TYPE :id ..project1.. :name ..projectname1..}]
                                                                                      [{:type k/FILE-TYPE :id ..file1.. :name ..filename1..}
                                                                                       {:type k/FOLDER-TYPE :id ..folder2.. :name ..foldername2..}
                                                                                       {:type k/PROJECT-TYPE :id ..project2.. :name ..projectname2..}]]
                                                                           ..file2.. [[{:type k/FILE-TYPE :id ..file2.. :name ..filename2..}
                                                                                       {:type k/FOLDER-TYPE :id ..folder2.. :name ..foldername2..}
                                                                                       {:type k/PROJECT-TYPE :id ..project1.. :name ..projectname1..}]
                                                                                      [{:type k/FILE-TYPE :id ..file2.. :name ..filename2..}
                                                                                       {:type k/FOLDER-TYPE :id ..folder2.. :name ..foldername2..}
                                                                                       {:type k/PROJECT-TYPE :id ..project2.. :name ..projectname2..}]]})))
