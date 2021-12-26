FROM clojure:openjdk-17-lein-buster as builder

COPY . /usr/app

WORKDIR /usr/app

RUN lein uberjar

FROM openjdk:17-slim

WORKDIR /opt/logbeard

COPY --from=builder /usr/app/target/relaggregator-0.1.0-SNAPSHOT-standalone.jar /opt/logbeard/logbeard.jar
COPY --from=builder /usr/app/src/clojure/relaggregator/config.json /opt/logbeard/config.json

ENTRYPOINT ["java", "-jar", "logbeard.jar"]
