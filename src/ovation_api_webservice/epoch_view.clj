(ns ovation-api-webservice.epoch-view)

(defn index-epoch [params]
  (let [
        status (if-not (contains? params :api-key)
                 (num 401)
                 (num 200)
               )
        body (if (= 200 status)
               (str "[{
    \"type\" : \"{ENTITY_NAME}\",
    \"access\" : \"read|write\",
    \"_id\": \"6f7cca39-3158-4998-bae6-3edd074b77dc\",
    \"_rev\": \"1-12306b14b5a0f99a18cb2100c2dce6d8\",
    \"attributes\" : {
        \"start\": \"2012-08-28T16:46:29.000Z\",
        \"end\": \"2012-08-28T18:58:43.000Z\",
        \"startZone\": \"UTC\",
        \"endZone\": \"UTC\",
        \"protocol_parameters\" : {
            \"DNATemplateType\" : \"WET_DNA\",
            \"Type.Chemistry.1.Description\" : \"The PCR reactions contain primers designed to amplify the target sequence and SYBR® Green I dye to detect double-stranded DNA.\",
            \"InstrumentTypeId\" : \"spyder\",
            \"CHIP_CENTER_y\" : \"1283\",
        },
        \"device_parameters\" : {},
    },
    \"links\" : {
        \"owner\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02\",
            \"count\" : 1
        },
        \"parent\" : {
            \"href\" : \"/api/v1/entities/a53a6be0-6823-4f0f-aa4f-44171dfaa7c4\",
            \"count\" : 1
        },
        \"experiments\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/experiments\",
            \"count\" : 0
        },
        \"epochs\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/epochs\",
            \"count\" : 1
        },
        \"protocol\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/protocol\",
            \"count\" : 0
        },
        \"measurements\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/measurements\",
            \"count\" : 0
        },
        \"analysis_records\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/analyses\",
            \"count\" : 0
        }
    },
    
    \"named_links\" : {
        \"input_sources\" : {
            \"Naprox_11-0030\" : {
                \"href\" : \"/api/v1/entities/a1b862e3-ccf5-4bc1-a2b4-7ca5ce20fb65\",
                \"count\" : 1
            },
            \"Naprox_11-0002\" : {
                \"href\" : \"/api/v1/entities/0f9ee7e9-3a7b-41fa-922b-c4dded245f36\",
                \"count\" : 1
            }
        },
        \"output_sources\" : {},
    }
}]"
               )
               (str "Please log in to get your epochs")
             )
       ]
    {:status status
     :body body}
  )
)

(defn get-epoch [params]
  (let [
    status (if (or (not (contains? params :api-key)) (not (contains? params :id)))
             (num 401)
             (num 200)
           )
    body (if (= 200 status)
           (str "[{
    \"type\" : \"{ENTITY_NAME}\",
    \"access\" : \"read|write\",
    \"_id\": \"PUT-ID-HERE\",
    \"_rev\": \"1-12306b14b5a0f99a18cb2100c2dce6d8\",
    \"attributes\" : {
        \"start\": \"2012-08-28T16:46:29.000Z\",
        \"end\": \"2012-08-28T18:58:43.000Z\",
        \"startZone\": \"UTC\",
        \"endZone\": \"UTC\",
        \"protocol_parameters\" : {
            \"DNATemplateType\" : \"WET_DNA\",
            \"Type.Chemistry.1.Description\" : \"The PCR reactions contain primers designed to amplify the target sequence and SYBR® Green I dye to detect double-stranded DNA.\",
            \"InstrumentTypeId\" : \"spyder\",
            \"CHIP_CENTER_y\" : \"1283\",
        },
        \"device_parameters\" : {},
    },
    \"links\" : {
        \"owner\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02\",
            \"count\" : 1
        },
        \"parent\" : {
            \"href\" : \"/api/v1/entities/a53a6be0-6823-4f0f-aa4f-44171dfaa7c4\",
            \"count\" : 1
        },
        \"experiments\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/experiments\",
            \"count\" : 0
        },
        \"epochs\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/epochs\",
            \"count\" : 1
        },
        \"protocol\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02/protocol\",
            \"count\" : 0
        },
        \"measurements\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/measurements\",
            \"count\" : 0
        },
        \"analysis_records\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/analyses\",
            \"count\" : 0
        }
    },
    
    \"named_links\" : {
        \"input_sources\" : {
            \"Naprox_11-0030\" : {
                \"href\" : \"/api/v1/entities/a1b862e3-ccf5-4bc1-a2b4-7ca5ce20fb65\",
                \"count\" : 1
            },
            \"Naprox_11-0002\" : {
                \"href\" : \"/api/v1/entities/0f9ee7e9-3a7b-41fa-922b-c4dded245f36\",
                \"count\" : 1
            }
        },
        \"output_sources\" : {},
    }
}]"
               )
           (str "Please log in to get your epochs")
         )
       ]
    {:status status
     :body body}
  )
)

(defn create-epoch [params]
  "TODO"
)

(defn update-epoch [params]
  "TODO"
)

(defn delete-epoch [params]
  "TODO"
)


