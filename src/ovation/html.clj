(ns ovation.html)


(defn apply-policy
  [policy-builder s]
  (let [sb (StringBuilder.)
        handler nil
        renderer (org.owasp.html.HtmlStreamRenderer/create sb handler)
        policy (.build policy-builder renderer)]

       (org.owasp.html.HtmlSanitizer/sanitize s, policy)
       (.toString sb)))

(defn sanitize-note-html
  "HTML-sanitizes text, permitting <span>/<user-mention> tags and attributes"
  [s]
  (let [policy (-> (org.owasp.html.HtmlPolicyBuilder.)
                  (.allowElements (into-array ["span" "user-mention"]))
                  (.allowAttributes (into-array ["uuid"]))
                  (.onElements (into-array ["user-mention"]))
                  (.allowAttributes (into-array ["class" "data-atwho-at-query"]))
                  (.onElements (into-array ["span"])))]

    (apply-policy policy s)))


(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String text
    (replace "&"  "&amp;")
    (replace "<"  "&lt;")
    (replace ">"  "&gt;")
    (replace "\"" "&quot;")
    (replace "'" "&apos;")))
