package by.gdev.alert.job.parser.service.order.jsoup;

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
@Component
public class JsoupClient {

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
                if (e.getStatusCode() == 403) {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    continue;
                }
                throw e;
            }
        }
        throw new IOException("Failed after retries: " + url);
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
