(ns ovation-api-webservice.util)

(defn ctx [api_key]
  (do 
    (. (. (. (. us.physion.ovation.api.web.Server make (new java.net.URI "https://dev.ovation.io") api_key) toBlockingObservable) first) getContext)
  )
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

(defn mung-strings [s]
  (.replaceAll (new java.lang.String s) "ovation://" "https://localhost:3000/")
)
