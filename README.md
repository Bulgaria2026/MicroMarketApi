# MicroMarketApi

MicroMarketApi is a Spring Boot application for the MicroMarket platform.

## Requirements

- Java 25+
- Gradle
- Docker and Docker Compose
- PostgreSQL 18+ if not using Docker

## Configuration

The application requires environment variables to run. You can manage these using a local `.env` file.

1. Copy the example file to create your local environment file:
   ```bash
   cp .env.example .env
   ```
2. Update the values in `.env` to match your local setup.

---

## Running the Application

Choose the launch configuration that matches your current task.

### 1. Local Backend Development

Use this mode if you are writing Java code. You will run the database in Docker and the application via Gradle for faster feedback loops.

**Start the database:**

```bash
docker compose -f docker-compose.database.yaml up -d
```

**Run the application:**

```bash
./gradlew bootRun
```

The application will use the profile defined in `SPRING_PROFILES_ACTIVE`.

### 2. Frontend Development

Use this mode if you are developing a frontend and only need the backend to be available as a service without managing the Java environment.

**Start the backend services:**

```bash
docker compose -f docker-compose.development.yaml up -d
```

### 3. Production

Use this mode to deploy the backend in a production-ready configuration.

**Start the production:**

```bash
docker compose up -d
```

---

## API Documentation and Access

Once the application is running, you can access the API and its documentation at the following endpoints:

- **Base API URL:** `http://localhost:8080/api/v1`
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

## Testing

To run the automated test suite, execute:

```bash
./gradlew test
```
