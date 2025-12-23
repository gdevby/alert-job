package by.gdev.alert.job.parser.cloudflare;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class CloudflareDetector {

    public static boolean hasCloudflareProtection(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .ignoreHttpErrors(true)
                    .execute();

            int status = response.statusCode();

            //Если статус 403 и сервер Cloudflare → защита
            String serverHeader = response.header("Server");
            if (status == 403 && serverHeader != null && serverHeader.toLowerCase().contains("cloudflare")) {
                return true;
            }

            // Проверка спец. заголовков
            if (response.hasHeader("cf-ray") || response.hasHeader("cf-cache-status")) {
                return true;
            }

            // Проверка HTML (если он есть)
            String body = response.body().toLowerCase();
            if (body.contains("attention required") && body.contains("cloudflare")) {
                return true;
            }

            return false;
        } catch (Exception e) {
            System.err.println("Ошибка при проверке: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        String testUrl = "https://freelancehunt.com/projects/skill/animatsiya/91.html";
        boolean protectedByCloudflare = hasCloudflareProtection(testUrl);
        System.out.println("Cloudflare защита " + testUrl + ": " + protectedByCloudflare);
    }
}
