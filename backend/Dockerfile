FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/union-portal-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 3638
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
