# MicroMarketApi

MicroMarketApi is a Spring Boot application for the MicroMarket platform.

## Requirements

- Java 21+
- Gradle
- Docker and Docker Compose
- PostgreSQL 18+ if not using Docker

## Configuration

The application reads its configuration from environment variables or a local `.env` file.

### Environment variables

Copy [.env.example](.env.example) to `.env` in the project root and configure the values for your environment:

```bash
cp .env.example .env
```

Then update the variables as needed.

## Database

A local PostgreSQL container is defined in [docker-compose.database.yaml](docker-compose.database.yaml).

Start it with:

```bash
docker compose -f docker-compose.database.yaml up -d
```

## Running the application

### With Gradle

```bash
./gradlew bootRun
```

The application runs with the profile defined by `SPRING_PROFILES_ACTIVE` and connects to the PostgreSQL database configured in the environment.

### With Docker

Use [docker-compose.development.yaml](docker-compose.development.yaml) for to run the backend for frontend development or [docker-compose.yaml](docker-compose.yaml) for production.

## API documentation

After starting the application, the API should be available at:

```text
http://localhost:8080/api/v1
```

You can also use the Swagger UI for API information at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Testing

Run the test suite with:

```bash
./gradlew test
```
