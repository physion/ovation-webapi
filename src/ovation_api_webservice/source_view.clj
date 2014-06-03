(ns ovation-api-webservice.source-view)

(defn index-source [params]
  (let [
        status (if-not (contains? params :api-key)
                 (num 401)
                 (num 200)
               )
        body (if (= 200 status)
               (str "[{
    \"type\" : \"Source\",
    \"access\" : \"write\",
    \"_id\": \"0f9ee7e9-3a7b-41fa-922b-c4dded245f36\",
    \"_rev\": \"1-de5bb5f8ff628274b6fe0fe6251b6e49\",
    \"attributes\" : {
        \"version\": \"2.1.5\",
        \"label\": \"Naprox_11-0002\",
        \"identifier\": \"Naprox_11-0002\"
    },
    \"links\" : {
        \"owner\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02\",
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
        },
        \"children\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/children\",
            \"count\" : 0
        }
    }
}]")
               (str "Please log in to get your sources")
             )
       ]
    {:status status
     :body body}
  )
)

(defn get-source [params]
  (let [
    status (if (or (not (contains? params :api-key)) (not (contains? params :id)))
             (num 401)
             (num 200)
           )
    body (if (= 200 status)
           (str "[{
    \"type\" : \"Source\",
    \"access\" : \"write\",
    \"_id\": \"PUT-ID-HERE\",
    \"_rev\": \"1-de5bb5f8ff628274b6fe0fe6251b6e49\",
    \"attributes\" : {
        \"version\": \"2.1.5\",
        \"label\": \"Naprox_11-0002\",
        \"identifier\": \"Naprox_11-0002\"
    },
    \"links\" : {
        \"owner\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02\",
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
        },
        \"children\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/children\",
            \"count\" : 0
        }
    }
}]")
           (str "Please log in to get your sources")
         )
       ]
    {:status status
     :body body}
  )
)

(defn create-source [params]
  "TODO"
)

(defn update-source [params]
  "TODO"
)

(defn delete-source [params]
  "TODO"
)


