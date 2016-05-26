(ns ovation.test.html
  (:require [midje.sweet :refer :all]
            [ovation.html :as html]))


(facts "About sanitize-html"
  (fact "allows <span><user-mention></span>"
    (html/sanitize-note-html "<body><span class=\"atwho-inserted\" data-atwho-at-query=\"@\"><user-mention uuid=\"0bf35ba9-4ac5-4eb2-a5ab-6131f169408f\">@rens Methratta</user-mention></span><body>") => "<span class=\"atwho-inserted\" data-atwho-at-query=\"&#64;\"><user-mention uuid=\"0bf35ba9-4ac5-4eb2-a5ab-6131f169408f\">&#64;rens Methratta</user-mention></span>"))

(facts "About escape-html"
  (fact "escapes <script>"
    (html/escape-html "<script attr=\"foo\">some script</script>") => "&lt;script attr=&quot;foo&quot;&gt;some script&lt;/script&gt;"))
