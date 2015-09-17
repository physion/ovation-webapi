(ns ovation.test.revisions
  (:use midje.sweet)
  (:require [ovation.revisions :as rev]
            [slingshot.slingshot :refer [throw+]]))


(facts "About Revisions"
  (facts "creation"
    (facts "`create-revision`"
      (facts "from a revision"
        (future-fact "creates a new revision from parent")
        (future-fact "appends to parent's previous chain")
        (future-fact "sets resource attribute"))
      (facts "from a file"
        (future-fact "creates a new revision")
        (future-fact "sets previous chain to []")
        (future-fact "adds revisions<->resource relationship")
        (future-fact "sets resource attribute"))))

  (facts "HEAD"
    (facts "with a single Revision"
      (future-fact "gets single Revision as HEAD"))
    (facts "with a Revision chain having a single HEAD"
      (future-fact "gets HEAD Revision"))
    (facts "with a Revision chain having multipel HEADs"
      (future-fact "gets all HEAD Revisions"))
    (facts "with a single HEAD revision but a chain with branches"
      (future-fact "gets the single HEAD Revision"))))
