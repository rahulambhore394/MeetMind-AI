FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/meetmind-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", "-jar", "app.jar"]

