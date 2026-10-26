# ==============================================================================
# Multi-stage Docker build for Spring Boot 3 with Eclipse Temurin Java 21
# ==============================================================================

# Build Stage
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Pre-fetch dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and package
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy built artifact
COPY --from=builder /app/target/*.jar app.jar

# Dynamic PORT environment variable for Render / Cloud hosting
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
