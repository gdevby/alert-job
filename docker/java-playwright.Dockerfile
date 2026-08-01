# parser-alert-job / notification-alert-job image: Playwright + layered Spring Boot JAR.

# Must be before any FROM — used in stage 2 base image tag (build-arg from pom.xml / Jenkins).
ARG PLAYWRIGHT_VERSION=1.61.0

# Stage 1: unpack the JAR into layers (same logic as java-service.Dockerfile).
FROM eclipse-temurin:17-jre-jammy AS extractor

# JAR file name is passed from Maven (dockerfile-maven-plugin).
ARG JAR_FILE
# Working directory for extraction.
WORKDIR /workspace
# Copy the built JAR from the module target/ directory.
COPY target/${JAR_FILE} application.jar
# Split the fat JAR into layers for better image layer caching on rebuild.
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# Stage 2: runtime with pre-installed Playwright browsers.
FROM mcr.microsoft.com/playwright/java:v${PLAYWRIGHT_VERSION}-jammy

# Service port (8017 parser, 8019 notification) — build-arg from pom.xml.
ARG SERVER_PORT=8017
# Persist the port in an environment variable for HEALTHCHECK.
ENV SERVER_PORT=${SERVER_PORT}
# JVM flags: respect container limits and faster random startup.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Temporarily root — required for apt-get install curl.
USER root
# curl for health checks; clean apt cache to keep the layer small.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Application directory inside the container.
WORKDIR /app
# External dependencies layer (changes infrequently).
COPY --from=extractor /workspace/extracted/dependencies/ ./
# Spring Boot loader layer (changes infrequently).
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
# SNAPSHOT dependencies layer (if any).
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
# Application code layer (changes on every deploy).
COPY --from=extractor /workspace/extracted/application/ ./
# Grant ownership to pwuser: Playwright writes browser cache to the home directory.
RUN chown -R pwuser:pwuser /app

# Run as pwuser (not root): required by Playwright for Chromium.
USER pwuser

# Document the port for orchestrator / compose.
EXPOSE ${SERVER_PORT}

# Readiness check via Spring Actuator (longer start_period due to Playwright).
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

# Start via Spring Boot 3 layered classloader.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
