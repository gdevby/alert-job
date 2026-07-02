package by.gdev.alert.job.parser.service.order.jsoup;


import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.proxy.ProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Клиент‑обёртка над стандартным {@link Jsoup}, обеспечивающий:
 *  - единый набор HTTP‑заголовков (эмуляция браузера);
 *  - автоматический retry при получении 403;
 *  - корректную обработку ошибок и таймаутов;
 *  - единый стиль запросов для всех HTML‑парсеров.
 *
 * Используется всеми парсерами, которым требуется загрузка HTML‑страниц.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsoupClient {

    private final ProxyService proxyService;

    /**
     * Получает активный прокси с retry‑логикой.
     *
     * @param maxRetries максимальное количество попыток
     * @param retryDelayMs задержка между попытками (мс)
     * @return {@link ProxyCredentials} или null, если прокси не найден
     */
    public ProxyCredentials getProxyWithRetry(int maxRetries, long retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ProxyCredentials proxy = proxyService.getRandomActiveProxy();
                if (proxy != null) {
                    return proxy;
                }

                log.warn("Попытка {}/{}: Нет активных прокси",
                        attempt, maxRetries);

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * attempt);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка получения прокси с попытки {}: {}",
                        attempt, e.getMessage(), e);
            }
        }

        log.warn("Ошибка получения прокси через {} попыток, продолжаем без прокси",
                maxRetries);
        return null;
    }

    /**
     * Выполняет HTTP GET‑запрос и возвращает сырое содержимое ответа
     * в виде строки без какой‑либо обработки.
     * @param url URL ресурса
     * @return строка с сырым телом ответа
     * @throws IOException если запрос завершился ошибкой
     */
    public String getRaw(String url) throws IOException {
        Connection.Response response = baseRequest(url).execute();
        return response.body();
    }

    /**
     * Выполняет GET‑запрос к указанному URL с retry‑логикой.
     * Поведение:
     *  - делает до 3 попыток;
     *  - при {@link HttpStatusException} с кодом 403 делает паузу и повторяет запрос;
     *  - при других ошибках выбрасывает исключение;
     *  - при исчерпании попыток выбрасывает {@link IOException}.
     *
     * @param url URL страницы
     * @return загруженный {@link Document}
     * @throws IOException если запрос не удался после всех попыток
     */
    public Document get(String url) throws IOException {
        for (int i = 0; i < 3; i++) {
            try {
                return baseRequest(url).get();
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 502 || e.getStatusCode() == 503 || e.getStatusCode() == 504) {
                    log.warn("JsoupClient {} for URL {} — игнорируем", e.getStatusCode(), url);
                    return null;
                }

                if (e.getStatusCode() == 403) {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    continue;
                }
                throw e;
            }
        }
        throw new IOException("Failed after retries: " + url);
    }

    public Document get(String url, ProxyCredentials proxy) throws IOException {

        HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());

        BasicCredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials(
                new AuthScope(proxy.getHost(), proxy.getPort()),
                new UsernamePasswordCredentials(
                        proxy.getUsername(),
                        proxy.getPassword().toCharArray()
                )
        );

        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(creds);

        CloseableHttpClient client = HttpClients.custom()
                .setProxy(proxyHost)
                .build();

        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.addHeader("Accept", "*/*");
        request.addHeader("Connection", "keep-alive");

        try (CloseableHttpResponse response = client.execute(request, context)) {

            int code = response.getCode();
            if (code == 407) {
                throw new IOException("Proxy auth failed (407)");
            }

            String html;
            try {
                html = EntityUtils.toString(response.getEntity());
            } catch (org.apache.hc.core5.http.ParseException e) {
                throw new IOException("Failed to parse HTTP response", e);
            }

            return Jsoup.parse(html, url);
        }

    }




    /**
     * Создаёт базовый {@link Connection} с преднастроенными:
     *  - User-Agent;
     *  - Accept / Accept-Language / Accept-Encoding;
     *  - Cache-Control / Pragma;
     *  - referrer;
     *  - таймаутом;
     *  - разрешением на обработку ошибок и редиректов.
     *
     * Используется для всех HTTP‑запросов внутри клиента.
     *
     * @param url URL страницы
     * @return настроенный {@link Connection}
     */
    private Connection baseRequest(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Connection", "keep-alive")
                .referrer("https://google.com/")
                .timeout(20000)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .followRedirects(true);
    }

}
