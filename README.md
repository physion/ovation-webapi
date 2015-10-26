# Ovation REST API

HTTP API for the Ovation Scientific Data Layer

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

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
