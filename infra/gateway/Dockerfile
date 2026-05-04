FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /workspace/target/api-gateway-*.jar ./gateway.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/gateway.jar"]
