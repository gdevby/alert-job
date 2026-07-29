# Shared image for Spring Boot microservices (layered JAR).

# Stage 1: unpack the JAR into layers (dependencies, loader, application).
FROM eclipse-temurin:17-jre-jammy AS extractor

# JAR file name is passed from Maven (dockerfile-maven-plugin).
ARG JAR_FILE
# Working directory for extraction.
WORKDIR /workspace
# Copy the built JAR from the module target/ directory.
COPY target/${JAR_FILE} application.jar
# Split the fat JAR into layers for better image layer caching on rebuild.
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# Stage 2: final runtime image.
FROM eclipse-temurin:17-jre-jammy

# Service port (8011, 8012, 8015, etc.) — build-arg from pom.xml.
ARG SERVER_PORT=8080
# Persist the port in an environment variable for HEALTHCHECK.
ENV SERVER_PORT=${SERVER_PORT}
# JVM flags: respect container limits and faster random startup.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Create a non-root user and install curl for health checks.
RUN groupadd -r app \
    && useradd -r -m -d /home/app -g app app \
    && apt-get update \
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
# Grant file ownership to the app user.
RUN chown -R app:app /app

# Do not run as root.
USER app
# Document the port for orchestrator / compose.
EXPOSE ${SERVER_PORT}

# Readiness check via Spring Actuator.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

# Start via Spring Boot 3 layered classloader.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
