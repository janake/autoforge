FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy a built Spring Boot fat jar into the image as gateway.jar
COPY gateway.jar ./gateway.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/gateway.jar"]

