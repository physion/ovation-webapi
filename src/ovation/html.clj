(ns ovation.html)


;private static String apply(HtmlPolicyBuilder b, String src) {}
;StringBuilder sb = new StringBuilder();
;HtmlSanitizer.Policy policy = b.build(HtmlStreamRenderer.create(sb,))
;new Handler<String>() {
;                       public void handle(String x) { fail(x)}}; }
;                                                     ;
;HtmlSanitizer.sanitize(src, policy);
;return sb.toString();


(defn apply-policy
  [policy-builder s]
  (let [sb (StringBuilder.)
        handler nil
        renderer (org.owasp.html.HtmlStreamRenderer/create sb handler)
        policy (.build policy-builder renderer)]

       (org.owasp.html.HtmlSanitizer/sanitize s, policy)
       (.toString sb)))

(defn sanitize-html
  "HTML-sanitizes text, permitting <span>/<user-mention> tags and attributes"
  [s]
  (let [policy (-> (org.owasp.html.HtmlPolicyBuilder.)
                  (.allowElements (into-array ["span" "user-mention"]))
                  (.allowAttributes (into-array ["uuid"]))
                  (.onElements (into-array ["user-mention"]))
                  (.allowAttributes (into-array ["class" "data-atwho-at-query"]))
                  (.onElements (into-array ["span"])))]

    (apply-policy policy s)))
