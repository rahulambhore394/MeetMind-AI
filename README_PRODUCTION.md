# MeetMind AI - Production Deployment Guide

## 1. Prerequisites
- Docker and Docker Compose
- PostgreSQL 15+ (if not using Docker)
- Redis 7+ (if not using Docker)
- Kafka 3+ (if not using Docker)
- Java 21+
- Android SDK 37+

## 2. Environment Variables
The following environment variables should be set in the production environment:

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `SPRING_REDIS_HOST` | Redis host |
| `SPRING_REDIS_PORT` | Redis port |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `JWT_SECRET` | 256-bit Base64 encoded secret for JWT |
| `JWT_EXPIRATION` | Token expiration in milliseconds |
| `SPRING_FLYWAY_ENABLED` | Set to `true` for production migrations |

## 3. Backend Deployment (Docker)
1. Build the backend jar: `./mvnw clean package -DskipTests`
2. Start the services: `docker-compose up -d --build`

## 4. Security Hardening
- CORS origins are restricted to `http://localhost:3000` and Android emulator by default. Update `SecurityConfig.java` for your production domain.
- Actuator endpoints are protected with `ROLE_ADMIN`. Ensure you configure a user with this role in your production security setup.
- Database passwords and JWT secrets must be changed from defaults using environment variables.

## 5. Observability
- Metrics are available at `/actuator/prometheus`.
- Health checks: `/actuator/health`.
- Structured logs are stored in `logs/meetmind.log` when running with `prod` profile.

## 6. Android Release
- Change `BASE_URL` in `app/build.gradle.kts` release build type.
- Enable `isMinifyEnabled` and configure ProGuard.
- Use `SecureTokenProvider` (implemented) which uses Android Keystore.
