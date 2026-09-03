FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Tests run separately from the production image build. Some integration tests
# use an H2 test database, while the container runs against MySQL.
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/union-portal-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 3638
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
