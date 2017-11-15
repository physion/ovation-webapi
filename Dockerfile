FROM clojure:lein-2.8.1
MAINTAINER support@ovation.io

#RUN apt-get -y update

ENV PORT 3000
EXPOSE 3000

RUN mkdir -p /app
COPY . /app
WORKDIR /app

RUN ["lein", "deps"]

CMD ["lein", "run"]
