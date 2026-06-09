package by.gdev.alert.job.parser.service.order.jsoup;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsoupClient {

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
