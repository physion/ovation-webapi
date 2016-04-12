#!/bin/bash

# Install catalyze-cli
wget https://github.com/catalyzeio/cli/releases/download/3.1.5/catalyze_3.1.5_linux_amd64.tar.gz
tar xzvf catalyze_3.1.5_linux_amd64.tar.gz

# Associate catalyze environment
catalyze_3.1.5_linux_amd64/catalyze associate $CATALYZE_ENVIRONMENT app --username $CATALYZE_USER  --password $CATALYZE_PASSWORD

# Deploy
git fetch --unshallow || true
git push catalyze ${CI_COMMIT_ID}:master
