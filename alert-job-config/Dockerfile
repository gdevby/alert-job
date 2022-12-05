FROM eclipse-temurin:17-jre-ubi9-minimal as build
LABEL maintainer="Robert Makrytski <snajper.ro@yandex.ru>"
VOLUME /tmp
FROM eclipse-temurin:17-jre-ubi9-minimal
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/myservice/myservice.jar
ENTRYPOINT ["java", "-jar", "/usr/share/myservice/myservice.jar"]
HEALTHCHECK --retries=10 --interval=1s --timeout=1s CMD curl -f http://alert-job-config:8011/actuator/health || exit 1
