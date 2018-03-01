#!/bin/bash

NAMESPACE=$1

set -e

RELEASE_NAME=webapi-$NAMESPACE
DEFAULT_ZONE=us-east1-b
KUBERNETES_CLUSTER_NAME=ovation

codeship_google authenticate
export GOOGLE_APPLICATION_CREDENTIALS=/keyconfig.json

gcloud container clusters get-credentials $KUBERNETES_CLUSTER_NAME \
  --project $GOOGLE_PROJECT_ID \
  --zone $DEFAULT_ZONE


echo "Setting Project ID $PROJECT_ID"
gcloud config set project $GOOGLE_PROJECT_ID

echo "Setting default timezone $DEFAULT_ZONE"
gcloud config set compute/zone $DEFAULT_ZONE

## Install helm
curl https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get | bash
helm init --upgrade #--force-upgrade

# Install helm-secrets
echo "Installing helm-secrets"
helm plugin install https://github.com/futuresimple/helm-secrets

echo "Upgrading webapi release..."

echo "NAMESPACE = $NAMESPACE"
echo "RELEASE_NAME = $RELEASE_NAME"
echo "CI_TIMESTAMP = $CI_TIMESTAMP"

# Ensure kube-lego is available
helm upgrade --install kube-lego-${NAMESPACE} stable/kube-lego\
    --namespace ${NAMESPACE} \
    --set config.LEGO_URL=https://acme-v01.api.letsencrypt.org/directory \
    --set config.LEGO_EMAIL=dev@ovation.io \
    --set config.LEGO_DEFAULT_INGRESS_CLASS=gce \
    --set rbac.create=true

# Update dependencies
helm repo add cos https://centerforopenscience.github.io/helm-charts/
helm dependencies update ./deploy/ovation-webapi/


helm-wrapper upgrade --install --namespace=${NAMESPACE} --timeout 600 --wait \
    --set image.tag=${NAMESPACE}-${CI_TIMESTAMP} \
    -f ./deploy/values/${NAMESPACE}/secrets.yaml \
    ${RELEASE_NAME} \
    ./deploy/ovation-webapi/
