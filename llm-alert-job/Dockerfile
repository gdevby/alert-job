FROM eclipse-temurin:17.0.12_7-jre
LABEL maintainer="Robert Makrytski <snajper.ro@yandex.ru>"
VOLUME /tmp

# Установка ВСЕХ зависимостей для Playwright
RUN apt-get update && \
    # Пробуем оба варианта libasound
    (apt-get install -y --no-install-recommends libasound2t64 || apt-get install -y --no-install-recommends libasound2) && \
    apt-get install -y --no-install-recommends \
    # Основные
    libglib2.0-0 \
    libnss3 \
    libnspr4 \
    libdbus-1-3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libatspi2.0-0 \
    libx11-6 \
    libxcomposite1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libxcb1 \
    libxkbcommon0 \
    libpango-1.0-0 \
    libcairo2 \
    libx11-xcb1 \
    libxcursor1 \
    libgtk-3-0 \
    libpangocairo-1.0-0 \
    libcairo-gobject2 \
    libgdk-pixbuf-2.0-0 \
    # Из сообщения Playwright
    libgstreamer1.0-0 \
    libatomic1 \
    libxslt1.1 \
    libwoff1 \
    libvpx9 \
    libevent-2.1-7t64 \
    libopus0 \
    libgstreamer-plugins-base1.0-0 \
    libgstreamer-gl1.0-0 \
    libgstreamer-plugins-bad1.0-0 \
    libwebpdemux2 \
    libharfbuzz-icu0 \
    libenchant-2-2 \
    libsecret-1-0 \
    libhyphen0 \
    libmanette-0.2-0 \
    libflite1 \
    libgles2 \
    gstreamer1.0-libav \
    # Дополнительно
    fonts-liberation \
    fonts-ipafont-gothic \
    fonts-wqy-zenhei \
    fonts-thai-tlwg \
    fonts-kacst \
    fonts-freefont-ttf \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/myservice/myservice.jar
ENTRYPOINT ["java", "-jar", "/usr/share/myservice/myservice.jar"]