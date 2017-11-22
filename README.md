# Ovation REST API

[ ![Codeship Status for physion/ovation-webapi](https://codeship.com/projects/5f378b10-5e1d-0133-8441-3a8f5e5e1065/status?branch=master)](https://codeship.com/projects/111263) [![Dependencies Status](https://jarkeeper.com/physion/ovation-webapi/status.svg)](https://jarkeeper.com/physion/ovation-webapi)

HTTP API for the Ovation Scientific Data Layer

## Prerequisites

You will need 

* [Leiningen][1] 2.3.4+
* Java 1.8+
* Kubernetes
* Helm (`brew install kubernetes-helm`)
* helm-secrets (`helm plugin install https://github.com/futuresimple/helm-secrets`)

[1]: https://github.com/technomancy/leiningen

## Usage

### Authentication
Calls to the Ovation API are authenticated by Bearer token:

 ```
 Authorization: Bearer <api key>
 ```

You can get your API key from your account Profile > Token.


## Testing

To test from the command line, run:

    docker-compose run repl lein midje
    
Or from a REPL (with sweet, sweet autotest):

    (use 'midje.repl)
    (autotest)

Limit test to directory

    (midje.repl/autotest :dirs "test/ovation/test/db")

## Running

To start a web server for the application on port 3000:

    docker-compose up

Or from a REPL:
    
    ;; start system
    (require 'ovation.user)
    (ovation.user/go)
    
    ;; stop system
    (ovation.user/stop)
    
    ;; reset/refresh repl
    (ovation.user/reset)
    
    
## REPL

To start a REPL:

    docker-compose run web lein repl
   
or to start a headless nREPL server that can be used from, e.g. IntelliJ:

    docker-compose up
    
the nREPL port is fixed in docker-compose.yml, but nREPL saves the port in `.nrepl-port`.

Connect to REPL

    lein repl :connect 0.0.0.0:59789

Reload code

    (clojure.tools.namespace.repl/refresh)

## License

[Eclipse Public License](https://www.eclipse.org/legal/epl-v10.html)

Copyright © 2014-2015 Physion LLC
