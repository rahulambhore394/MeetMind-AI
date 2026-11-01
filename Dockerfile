# Dockerfile for MeetMind AI backend (Spring Boot)
# -------------------------------------------------
# Build stage using Gradle wrapper
FROM gradle:7.6-jdk17 AS builder
WORKDIR /app
# Copy Gradle wrapper files and source code
COPY gradlew .
COPY gradle ./gradle
COPY meetmind-backend ./meetmind-backend
# Build the bootJar (skip tests for faster build)
RUN ./gradlew :meetmind-backend:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the built jar from builder stage
COPY --from=builder /app/meetmind-backend/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
