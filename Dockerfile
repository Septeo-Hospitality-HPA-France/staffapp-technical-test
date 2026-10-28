# Build stage: compiles the application with the Maven wrapper, no local Java or Maven needed.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first, so they stay cached as long as pom.xml does not change
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q -B dependency:go-offline

COPY src src
# Tests need Docker (Testcontainers), not available during an image build: run them with the "test" compose service
RUN ./mvnw -q -B -DskipTests package

# Run stage: only the JRE and the jar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
USER app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
