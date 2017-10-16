#!/bin/bash

NAMESPACE=$1

set -e

RELEASE_NAME=webapi-$NAMESPACE
DEFAULT_ZONE=us-east1-b
KUBERNETES_CLUSTER_NAME=ovation

codeship_google authenticate

gcloud container clusters get-credentials $KUBERNETES_CLUSTER_NAME \
  --project $GOOGLE_PROJECT_ID \
  --zone $DEFAULT_ZONE


echo "Setting Project ID $PROJECT_ID"
gcloud config set project $GOOGLE_PROJECT_ID

echo "Setting default timezone $DEFAULT_ZONE"
gcloud config set compute/zone $GOOGLE_PROJECT_ID

# Install helm
curl https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get | bash
helm init --upgrade

# Install helm-secrets
echo "Installing helm-secrets"
helm plugin install https://github.com/futuresimple/helm-secrets

echo "Upgrading webapi release..."

echo "NAMESPACE = $NAMESPACE"
echo "RELEASE_NAME = $RELEASE_NAME"
echo "CI_TIMESTAMP = $CI_TIMESTAMP"

#helm-wrapper install --dry-run --debug --namespace=${NAMESPACE} --timeout 600 --wait \
#    --set image.tag=${NAMESPACE}-${CI_TIMESTAMP} \
#    --set ingress.staticIPAddressName=${NAMESPACE}-webapi-static-ip \
#    -f ./deploy/values/${NAMESPACE}/secrets.yaml \
#    ./deploy/ovation-webapi/

helm-wrapper upgrade --install --namespace=${NAMESPACE} --timeout 600 --wait \
    --set image.tag=${NAMESPACE}-${CI_TIMESTAMP} \
    --set ingress.staticIPAddressName=${NAMESPACE}-webapi-static-ip \
    -f ./deploy/values/${NAMESPACE}/secrets.yaml \
    ${RELEASE_NAME} \
    ./deploy/ovation-webapi/
