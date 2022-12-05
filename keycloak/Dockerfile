FROM quay.io/keycloak/keycloak:20.0.1
# Must add json files before import, volume is not mounted yet
ADD ./import /opt/keycloak/data/import
# Import all files
RUN /opt/keycloak/bin/kc.sh import --dir=/opt/keycloak/data/import/ --override true; exit 0
# Start image
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "-v"]
HEALTHCHECK --retries=10 --interval=2s --timeout=1s CMD curl -sS http://alert-job-keycloak:8080 || exit 1