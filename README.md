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

### JWT Keys

The application uses RSA key pairs to sign and verify JWT tokens.

**Development:** No configuration needed. Ephemeral keys are generated automatically at startup. Note that restarting the application invalidates all previously issued tokens since a new key pair is generated each time.

**Production:** A persistent key pair must be provided. The application will refuse to start without one.

Generate the keys:

```bash
./scripts/generate-jwt-keys.sh
```

This creates `secrets/jwt/app.key` and `secrets/jwt/app.pub`. The Docker Compose production setup mounts this directory automatically.

For local development with the `prod` profile, configure the keys in your `.env` file:

```properties
jwt.public.key=file:./secrets/jwt/app.pub
jwt.private.key=file:./secrets/jwt/app.key
```

### Stripe (payments)

Checkout requires a Stripe test-mode key and a running webhook listener. See [docs/stripe-dev.md](docs/stripe-dev.md) for the full setup.

### GitHub Packages (email templates)

The build pulls `com.noserbulgaria.micromarket:email-templates` from GitHub Packages, which requires auth even for public artifacts. Create a classic PAT with only the `read:packages` scope and add it to `~/.gradle/gradle.properties`:

```properties
gpr.user=<your-github-login>
gpr.key=ghp_xxxxxxxxxxxxxxxxxxxx
```

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

Use this mode to deploy the full stack in a production-ready configuration.

**Generate JWT keys** (if not already done):

```bash
./scripts/generate-jwt-keys.sh
```

**Start the production:**

```bash
docker compose up -d
```

---

## API Documentation and Access

Once the application is running, you can access the API and its documentation at the following endpoints:

- **Base API URL:** `http://localhost:8080/api/v1`
- **Swagger UI:** `http://localhost:8080/api/v1/swagger-ui/index.html` (dev profile only)

## Testing

To run the automated test suite, execute:

```bash
./gradlew test
```
