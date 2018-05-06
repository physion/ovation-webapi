#!/bin/bash

docker-compose -f docker-compose.yml build

NAMESPACE=$1

set -e

RELEASE_NAME=webapi-$NAMESPACE

## Install helm
#curl https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get | bash
#helm init --upgrade #--force-upgrade

# Install helm-secrets
#echo "Installing helm-secrets"
#helm plugin install https://github.com/futuresimple/helm-secrets

echo "Upgrading webapi release..."

echo "NAMESPACE = $NAMESPACE"
echo "RELEASE_NAME = $RELEASE_NAME"
echo "IMAGE_TAG = latest

# Update dependencies
helm repo add cos https://centerforopenscience.github.io/helm-charts/
helm dependencies update ./deploy/ovation-webapi/


helm-wrapper upgrade --install --debug --namespace=${NAMESPACE} --timeout 600 --wait \
    --set image.tag=latest \
    -f ./deploy/values/${NAMESPACE}/secrets.yaml \
    ${RELEASE_NAME} \
    ./deploy/ovation-webapi/
