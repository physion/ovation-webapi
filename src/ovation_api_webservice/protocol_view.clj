(ns ovation-api-webservice.protocol-view)

(defn index-protocol [params]
  (let [
        status (if-not (contains? params :api-key)
                 (num 401)
                 (num 200)
                 )
        body (if (= 200 status)
               (str "[{
    \"type\" : \"Protocol\",
    \"access\" : \"write\",
    \"_id\": \"05a616f8-7d71-4129-b124-c29093fb732e\",
    \"_rev\": \"3-6db2238391395141e4827130a1ececa0\",
    \"attributes\" : {
        \"version\": \"2.0.0\",
        \"name\": \"SNP variation\",
        \"protocolDocument\": \"This is the protocol document\"
    },
    \"links\" : {
        \"owner\" : {
            \"href\" : \"/api/v1/entities/1dbbba70-08c7-0131-2b72-22000aa62e2d\",
            \"count\" : 1
        },
        \"experiments\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/experiments\",
            \"count\" : 0
        },
        \"projects\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/projects\",
            \"count\" : 1
        },
        \"analysis_records\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/analyses\",
            \"count\" : 0
        }
    }
}]"
               )
               (str "Please log in to get your Protocols")
             )
       ]
    {:status status
     :body body}
  )
)