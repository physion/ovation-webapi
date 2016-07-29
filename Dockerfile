FROM clojure
MAINTAINER support@ovation.io

COPY . /usr/src/app
WORKDIR /usr/src/app

ENV PORT 8080
EXPOSE 8080

RUN ["lein", "deps"]

CMD ["lein", "ring", "server-headless"]
