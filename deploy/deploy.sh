#!/bin/bash

NAMESPACE=$1

set -e

RELEASE_NAME=webapi-$NAMESPACE
DEFAULT_ZONE=us-east1-b

codeship_google authenticate

gcloud container clusters get-credentials $KUBERNETES_CLUSTER_NAME \
  --project $GOOGLE_CLOUD_PROJECT_ID \
  --zone $DEFAULT_ZONE


echo "Setting Project ID $PROJECT_ID"
gcloud config set project $GOOGLE_CLOUD_PROJECT_ID

echo "Setting default timezone $DEFAULT_ZONE"
gcloud config set compute/zone $GOOGLE_CLOUD_PROJECT_ID

# Install helm
curl https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get | bash
helm init --upgrade

# Install helm-secrets
helm plugin install https://github.com/futuresimple/helm-secrets

echo "Upgrading webapi relase..."
# helm install…
helm-secrets upgrade --install --namespace=$NAMESPACE --timeout 600 --wait \
    --set image.tag=$NAMESPACE-$CI_TIMESTAMP \
    --set ingress.staticIPAddressName=$NAMESPACE-webapi-static-ip \
    --set config.CLOUDANT_DB_URL=$CLOUDANT_DB_URL
    --set config.OVATION_IO_HOST_URI=$OVATION_IO_HOST_URI
    --set config.GOOGLE_CLOUD_PROJECT_ID=$GOOGLE_CLOUD_PROJECT_ID
    -f ./deploy/values/$NAMESPACE/secrets.yaml \
    $RELEASE_NAME \
    ./deploy/ovation-webapi/
