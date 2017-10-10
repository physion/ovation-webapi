#!/bin/bash

set -e

RELEASE_NAME=webapi
DEFAULT_ZONE=us-east1-b

codeship_google authenticate

gcloud container clusters get-credentials $KUBERNETES_CLUSTER_NAME \
  --project $PROJECT_ID \
  --zone $DEFAULT_ZONE


echo "Setting Project ID $PROJECT_ID"
gcloud config set project $GOOGLE_CLOUD_PROJECT_ID

echo "Setting default timezone $DEFAULT_ZONE"
gcloud config set compute/zone $GOOGLE_CLOUD_PROJECT_ID

echo "Upgrading webapi relase..."
NAMESPACE=$CI_BRANCH
# helm install…
helm upgrade --install --namespace=$NAMESPACE -f deploy-values.yaml \
    --set image.tag=$NAMESPACE-$CI_TIMESTAMP \
    --set ingress.staticIPAddressName=$NAMESPACE-webapi-static-ip \
    --set config.CLOUDANT_DB_URL=$CLOUDANT_DB_URL
    --set config.OVATION_IO_HOST_URI=$OVATION_IO_HOST_URI
    --set config.GOOGLE_CLOUD_PROJECT_ID=$GOOGLE_CLOUD_PROJECT_ID
    --set config.ELASTICSEARCH_URL=$ELASTICSEARCH_URL
    --set secret.CLOUDANT_PASSWORD=$CLOUDANT_PASSWORD
    --set secret.CLOUDANT_USERNAME=$CLOUDANT_USERNAME
    --set secret.JWT_SECRET=$JWT_SECRET
    --set secret.RAYGUN_API_KEY=$RAYGUN_API_KEY
    $RELEASE_NAME \
    ./deploy/ovation-webapi/
