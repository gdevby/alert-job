# HTTP launcher for Camoufox browser instances (sidecar for notification).

ARG PLAYWRIGHT_VERSION=1.62.0

FROM mcr.microsoft.com/playwright/python:v${PLAYWRIGHT_VERSION}-jammy

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY config/camufox/requirements.txt /tmp/requirements-camoufox.txt
RUN pip3 install --no-cache-dir -r /tmp/requirements-camoufox.txt \
    && python3 -m playwright install-deps \
    && python3 -m camoufox fetch \
    && rm -f /tmp/requirements-camoufox.txt

COPY config/camufox/camoufox_launcher.py /opt/camoufox/camoufox_launcher.py

ENV CAMOUFOX_HTTP_BIND=0.0.0.0
ENV CAMOUFOX_WS_HOST=camoufox-launcher

EXPOSE 8888

HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
  CMD curl -fsS http://localhost:8888/health || exit 1

WORKDIR /opt/camoufox
CMD ["python3", "/opt/camoufox/camoufox_launcher.py"]
