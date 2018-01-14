FROM clojure:lein-2.8.1
MAINTAINER support@ovation.io

#RUN apt-get -y update

ENV PORT 3000
EXPOSE 3000

RUN mkdir -p /app
COPY . /app
WORKDIR /app

RUN ["lein", "deps"]

# CMD ["lein", "run"]
ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh
