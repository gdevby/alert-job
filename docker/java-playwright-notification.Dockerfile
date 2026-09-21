# notification-alert-job: Playwright + Camoufox launcher + layered Spring Boot JAR.

ARG PLAYWRIGHT_VERSION=1.61.0

# --- Camoufox layer (cached when requirements unchanged) ---
FROM mcr.microsoft.com/playwright/java:v${PLAYWRIGHT_VERSION}-jammy AS camoufox-layer

USER root
COPY target/docker/requirements.txt /tmp/requirements-camoufox.txt
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip \
    && pip3 install --no-cache-dir --target=/opt/camoufox-python -r /tmp/requirements-camoufox.txt \
    && rm -rf /var/lib/apt/lists/* /tmp/requirements-camoufox.txt

# --- JAR layers ---
FROM eclipse-temurin:17-jre-jammy AS extractor

ARG JAR_FILE
WORKDIR /workspace
COPY target/${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# --- Runtime ---
FROM mcr.microsoft.com/playwright/java:v${PLAYWRIGHT_VERSION}-jammy

ARG SERVER_PORT=8019
ENV SERVER_PORT=${SERVER_PORT}
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=80.0 -XX:+CrashOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV PYTHONPATH=/opt/camoufox-python

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl netcat-openbsd python3 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=camoufox-layer /opt/camoufox-python /opt/camoufox-python
COPY target/docker/camoufox_launcher.py /opt/camoufox/camoufox_launcher.py
COPY target/docker/notification-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

WORKDIR /app
COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./
RUN chown -R pwuser:pwuser /app /opt/camoufox /opt/camoufox-python /entrypoint.sh

USER pwuser

EXPOSE ${SERVER_PORT}

HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["/entrypoint.sh"]
