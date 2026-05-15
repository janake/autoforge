FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/backend-0.1.0-SNAPSHOT.jar app.jar

# Run the container as a non-root UID where possible (safer than running as root).
# Using an explicit numeric UID avoids depending on useradd being available in the base image.
USER 1000

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
