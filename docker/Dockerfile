#FROM openjdk:8-alpine3.9
FROM maven:3.6.3-ibmjava-8-alpine

COPY . /app
WORKDIR /app

RUN apk add mysql-client

RUN mvn -T 1C clean install -Dmaven.artifact.threads=20 --batch-mode

ENTRYPOINT ["tail", "-f", "/dev/null"]