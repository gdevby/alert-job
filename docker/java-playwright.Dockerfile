# Parser service: Playwright browsers + layered Spring Boot JAR.
# Runs as pwuser (not root): Playwright browsers require a writable home/cache directory.
FROM eclipse-temurin:17-jre-jammy AS extractor

ARG JAR_FILE
WORKDIR /workspace
COPY target/${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM mcr.microsoft.com/playwright/java:v1.45.1-jammy

ARG SERVER_PORT=8017
ENV SERVER_PORT=${SERVER_PORT}
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./
RUN chown -R pwuser:pwuser /app

USER pwuser

EXPOSE ${SERVER_PORT}

HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
