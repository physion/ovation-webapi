#!/bin/bash

curl -L -O https://github.com/technomancy/leiningen/raw/2.4.3/bin/lein
chmod u+x lein
./lein with-profile ci beanstalk deploy webapi-development
