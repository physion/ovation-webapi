(ns ovation-api-webservice.experiment-view)

(defn index-experiment [params]
  (let [
        status (if-not (contains? params :api-key)
                 (num 401)
                 (num 200)
               )
        body (if (= 200 status)
               (str "[{
    \"type\" : \"Experiment\",
    \"access\" : \"write\",
    \"_id\": \"a53a6be0-6823-4f0f-aa4f-44171dfaa7c4\",
    \"_rev\": \"2-52190b6c478c0d99fc69f3260e85968c\",
    \"attributes\" : {
        \"start\": \"2012-08-28T16:46:29.000Z\",
        \"startZone\": \"UTC\",
        \"protocolParameters\": {},
        \"deviceParameters\": {},
        \"purpose\": \"A7_HYU85_corrected\"
    },
    
    \"links\" : {
        \"owner\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02\",
            \"count\" : 1
        },
        \"experiments\" : {
            \"href\" : \"/api/v1/entities/a53a6be0-6823-4f0f-aa4f-44171dfaa7c4/experiments\",
            \"count\" : 0
        },
        \"projects\" : {
            \"href\" : \"/api/v1/entities/a53a6be0-6823-4f0f-aa4f-44171dfaa7c4/projects\",
            \"count\" : 1
        },
        \"protocol\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/protocol\",
            \"count\" : 0
        },
        \"equipment_setup\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/equipment_setup\",
            \"count\" : 0
        },
        \"epoch_groups\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/epoch_groups\",
            \"count\" : 0
        },
        \"epochs\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/epochs\",
            \"count\" : 0
        },
        \"analysis_records\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/analyses\",
            \"count\" : 0
        }
    }
}]")
               (str "Please log in to get your experiments")
             )
       ]
    {:status status
     :body body}
  )
)

(defn get-experiment [params]
  (let [
    status (if (or (not (contains? params :api-key)) (not (contains? params :id)))
             (num 401)
             (num 200)
           )
    body (if (= 200 status)
           (str "[{
    \"type\" : \"Experiment\",
    \"access\" : \"write\",
    \"_id\": \"PUT-ID-HERE\",
    \"_rev\": \"2-52190b6c478c0d99fc69f3260e85968c\",
    \"attributes\" : {
        \"start\": \"2012-08-28T16:46:29.000Z\",
        \"startZone\": \"UTC\",
        \"protocolParameters\": {},
        \"deviceParameters\": {},
        \"purpose\": \"A7_HYU85_corrected\"
    },
    
    \"links\" : {
        \"owner\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02\",
            \"count\" : 1
        },
        \"experiments\" : {
            \"href\" : \"/api/v1/entities/a53a6be0-6823-4f0f-aa4f-44171dfaa7c4/experiments\",
            \"count\" : 0
        },
        \"projects\" : {
            \"href\" : \"/api/v1/entities/a53a6be0-6823-4f0f-aa4f-44171dfaa7c4/projects\",
            \"count\" : 1
        },
        \"protocol\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/protocol\",
            \"count\" : 0
        },
        \"equipment_setup\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/equipment_setup\",
            \"count\" : 0
        },
        \"epoch_groups\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/epoch_groups\",
            \"count\" : 0
        },
        \"epochs\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/epochs\",
            \"count\" : 0
        },
        \"analysis_records\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/analyses\",
            \"count\" : 0
        }
    }
}]")
           (str "Please log in to get your experiments")
         )
       ]
    {:status status
     :body body}
  )
)

(defn create-experiment [params]
  "TODO"
)

(defn update-experiment [params]
  "TODO"
)

(defn delete-experiment [params]
  "TODO"
)


