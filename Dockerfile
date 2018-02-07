FROM clojure:lein-2.8.1
MAINTAINER support@ovation.io

ENV PORT 3000
EXPOSE 3000

RUN mkdir /app
WORKDIR /app

COPY project.clj /app
RUN ["lein", "deps"]

COPY . /app
WORKDIR /app

# CMD ["lein", "run"]
ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh
