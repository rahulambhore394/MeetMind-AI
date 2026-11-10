# Technology Stack

## Backend

- Java 21; Maven; Spring Boot 3.4.1.
- Spring Web MVC, WebSocket/STOMP, Spring Security, Bean Validation, Spring Data JPA/Hibernate, Actuator, Mail, Redis, Kafka.
- PostgreSQL runtime database with H2 runtime dependency; Flyway migrations.
- JWT 0.12.6; Jackson Java Time and Hibernate modules; Micrometer Prometheus.
- Vosk 0.3.45 for offline speech recognition.
- Runtime configuration is in `application.properties` and `application-prod.properties`, including PostgreSQL, Redis, Kafka, JWT, WebRTC ICE servers, transcription, translation, AI providers, SMTP, multipart limits, and Actuator exposure.

## Android

- Kotlin 2.2.10; Android Gradle Plugin 9.2.1; compile SDK 37, target SDK 35, min SDK 28.
- Jetpack Compose Material 3, Navigation Compose, Lifecycle, AndroidX Security Crypto, WorkManager.
- Retrofit 2.11.0 with Kotlin serialization 1.6.3; OkHttp 4.12.0.
- WebRTC 111.0.0; Firebase BOM 33.1.2 with Messaging and Analytics.
- Java source/target compatibility 11; Compose and Kotlin serialization compiler plugins.

## Runtime shape

The backend exposes HTTP and STOMP endpoints and consumes/publishes Kafka events. The Android app uses Retrofit/OkHttp against the backend base URL, STOMP/WebSocket for realtime features, WebRTC for meetings, FCM for notifications, and foreground services for recording.
