FROM maven:3.6.3-jdk-11-slim AS build
WORKDIR /app
COPY . /app/
RUN mvn clean package

FROM amazoncorretto:11.0.20-alpine3.18
COPY --from=build /app/gateway-ha/target/gateway-ha-1.9.0-jar-with-dependencies.jar /app/
COPY --from=build /app/gateway-ha/gateway-ha-config.yml /app/
ENTRYPOINT ["java", "-jar", "/app/gateway-ha-1.9.0-jar-with-dependencies.jar", "server", "/app/gateway-ha-config.yml"]
