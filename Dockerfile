FROM clojure:openjdk-17-lein-buster as builder

COPY . /usr/src

WORKDIR /usr/src

RUN lein uberjar

FROM openjdk:17-slim

WORKDIR /opt/logbeard

COPY --from=builder /usr/src/target/relaggregator-0.1.0-SNAPSHOT-standalone.jar /opt/logbeard/logbeard.jar

ENTRYPOINT ["java", "-jar", "logbeard.jar"]
