# Build stage: produce the boot jar with the pinned Gradle/JDK toolchain
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Runtime stage: slim JRE running the jar
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
