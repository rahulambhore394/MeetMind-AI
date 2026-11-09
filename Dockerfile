# Multi-stage Dockerfile for MeetMind AI backend (Spring Boot / Maven / Java 21)
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy Maven files and source
COPY meetmind-backend/pom.xml ./meetmind-backend/pom.xml
COPY meetmind-backend/src ./meetmind-backend/src

# Build JAR skipping unit tests for fast and reliable cloud build
RUN mvn -f meetmind-backend/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/meetmind-backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", "-jar", "app.jar"]
