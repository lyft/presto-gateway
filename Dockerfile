FROM maven:3.6.3-jdk-8 AS builder
WORKDIR /app 
COPY . /app
RUN sed -n 's,.*<version>\(.*\)</version>.*,\1,p' /app/gateway-ha/pom.xml | head -1 > /app/VERSION 
RUN mvn clean install

FROM openjdk:8
WORKDIR /app
COPY --from=builder /app/VERSION /app/VERSION
ENV VERSION=$(cat /app/VERSION)
COPY --from=builder /app/gateway-ha/target/gateway-ha-${VERSION}-jar-with-dependencies.jar /app/gateway-ha-jar-with-dependencies.jar
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["entrypoint.sh"]

