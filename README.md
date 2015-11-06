# Ovation REST API

[![Dependencies Status](https://jarkeeper.com/physion/ovation-webapi/status.svg)](https://jarkeeper.com/physion/ovation-webapi)

HTTP API for the Ovation Scientific Data Layer

## Prerequisites

You will need 

* [Leiningen][1] 2.3.4+
* Java 1.8+

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

Or from a REPL:

    (use 'ring.util.serve)
    (require 'ovation.handler)
    (serve ovation.handler/app)

## License

[Eclipse Public License](https://www.eclipse.org/legal/epl-v10.html)

Copyright © 2014-2015 Physion LLC
