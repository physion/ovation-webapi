#!/bin/bash

VERISON=3.1.5
CATALYZE_CLI=catalyze_${VERISON}_linux_amd64

if [ ! -d "$CATALYZE_CLI" ]; then
    # Install catalyze-cli
    wget https://github.com/catalyzeio/cli/releases/download/$VERISON/$CATALYZE_CLI.tar.gz
    tar xzvf $CATALYZE_CLI.tar.gz
fi


# Associate catalyze environment
$CATALYZE_CLI/catalyze  --username=$CATALYZE_USER  --password=$CATALYZE_PASSWORD associate $CATALYZE_ENVIRONMENT app

# Deploy
git fetch --unshallow || true
git push catalyze ${CI_COMMIT_ID}:master
