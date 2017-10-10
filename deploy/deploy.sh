#!/bin/bash

set -e

KUBERNETES_CLUSTER_NAME=ovation
KUBERNETES_APP_NAME=webapi
DEFAULT_ZONE=us-east1-b
PROJECT_ID=ovation-io
NAMESPACE=development

codeship_google authenticate

gcloud container clusters get-credentials $KUBERNETES_CLUSTER_NAME \
  --project $PROJECT_ID \
  --zone $DEFAULT_ZONE


echo "Setting Project ID $PROJECT_ID"
gcloud config set project $PROJECT_ID

echo "Setting default timezone $DEFAULT_ZONE"
gcloud config set compute/zone $DEFAULT_ZONE

echo "Upgrading webapi relase..."
# helm install…
helm upgrade -f deploy-values.yaml webapi ovation-webapi/

#echo "Applying deployment..."
#kubectl --namespace=$NAMESPACE apply -f webapi.Deployment.yaml
#
#echo "Applying service..."
#kubectl --namespace=$NAMESPACE apply -f webapi.Service.yaml
#
#IMAGE=gcr.io/$PROJECT_ID/webapi:development-$CI_TIMESTAMP
#echo "Setting deployment image $IMAGE..."
#kubectl --namespace=$NAMESPACE set image deployment/webapi webapi=$IMAGE
