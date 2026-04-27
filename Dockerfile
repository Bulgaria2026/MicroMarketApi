# Build stage
FROM docker.io/gradle:9-jdk25 AS build
WORKDIR /app
COPY . .
RUN --mount=type=secret,id=github_username \
    --mount=type=secret,id=github_token \
    chmod +x gradlew && \
    GITHUB_USERNAME="$(cat /run/secrets/github_username)" \
    GITHUB_TOKEN="$(cat /run/secrets/github_token)" \
    ./gradlew clean bootJar --no-daemon

# Runtime stage
FROM docker.io/eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]