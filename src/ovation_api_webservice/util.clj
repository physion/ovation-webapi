(ns ovation-api-webservice.util)

(defn ctx [api_key]
  (.. us.physion.ovation.api.web.Server (make (java.net.URI. "https://dev.ovation.io") api_key) (getContext))
)

(defn get-body-from-request [request]
  (slurp (get request :body))
)

(defn object-to-json [obj]
  (->
    (new com.fasterxml.jackson.databind.ObjectMapper)
    (.registerModule (new com.fasterxml.jackson.datatype.guava.GuavaModule))
    (.registerModule (new com.fasterxml.jackson.datatype.joda.JodaModule))
    (.configure com.fasterxml.jackson.databind.SerializationFeature/WRITE_DATES_AS_TIMESTAMPS false)
    (.writeValueAsString obj)
  )
)

(defn json-to-object [json]
  (->
    (new com.fasterxml.jackson.databind.ObjectMapper)
    (.registerModule (new com.fasterxml.jackson.datatype.guava.GuavaModule))
    (.registerModule (new com.fasterxml.jackson.datatype.joda.JodaModule))
    (.readValue json java.util.Map)
  )
)

(defn entities-to-json [entity_seq]
  (let [
         array (into-array (map (fn [p] (. p toMap)) entity_seq))
       ]
    (object-to-json array)
  )
)

(defn host-from-request [request]
  (let [
        scheme (clojure.string/join "" [(name (get request :scheme)) "://"])
        host (get (get request :headers) "host")
       ]
    (clojure.string/join "" [scheme host "/"])
  )
)

(defn munge-strings [s host]
  (.replaceAll (new java.lang.String s) "ovation://" host)
)

(defn unmunge-strings [s host]
  (.replaceAll (new java.lang.String s) host "ovation://")
)

(defn auth-filter [request f]
  (let [
        params (get request :query-params)
        status (if-not (contains? params "api-key")
                 (num 401)
                 (num 200)
               )
        body (if (= 200 status)
               (str (f (get params "api-key")))
               (str "Please log in to get your Projects")
             )
       ]
    {:status status
     :body (munge-strings body (host-from-request request))}
  )
)
