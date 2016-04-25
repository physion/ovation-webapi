# Ovation REST API

[ ![Codeship Status for physion/ovation-webapi](https://codeship.com/projects/5f378b10-5e1d-0133-8441-3a8f5e5e1065/status?branch=master)](https://codeship.com/projects/111263) [![Dependencies Status](https://jarkeeper.com/physion/ovation-webapi/status.svg)](https://jarkeeper.com/physion/ovation-webapi)

HTTP API for the Ovation Scientific Data Layer

## Prerequisites

You will need 

* [Leiningen][1] 2.3.4+
* Java 1.8+

[1]: https://github.com/technomancy/leiningen

## Testing

To test from the command line, run:

    lein midje
    
Or from a REPL (with sweet, sweet autotest):

    (use 'midje.repl)
    (autotest)

## Running

To start a web server for the application, run:

    lein ring server

Or from a REPL:

    (use 'ring.server.standalone)
    (require 'ovation.handler)
    (serve ovation.handler/app)

## License

[Eclipse Public License](https://www.eclipse.org/legal/epl-v10.html)

Copyright © 2014-2015 Physion LLC
