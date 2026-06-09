package by.gdev.alert.job.parser.service.playwright;

import by.gdev.alert.job.parser.proxy.service.ProxyService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Менеджер Playwright, отвечающий за:
 *  - создание экземпляра {@link Playwright};
 *  - запуск браузера Chromium с детальной конфигурацией;
 *  - настройку прокси;
 *  - создание антидетект‑контекста;
 *  - корректное закрытие всех ресурсов Playwright.
 *
 * Используется всеми Playwright-парсерами.
 */
@Slf4j
@Component
public class PlaywrightManager {

    /**
     * Сервис для получения активных HTTP‑прокси.
     * Используется при включённом режиме useProxy.
     */
    @Autowired
    private ProxyService proxyService;

    /**
     * Создаёт новый экземпляр {@link Playwright}.
     *
     * @return объект Playwright
     * @throws RuntimeException если движок не удалось инициализировать
     */
    public Playwright createPlaywright() {
        Playwright playwright = null;
        try {
            playwright = Playwright.create();

        } catch (PlaywrightException e) {
            log.error("Playwright initialization error", e);
            throw new RuntimeException("Failed to initialize Playwright", e);
        } catch (Exception e) {
            log.error("Unexpected error during Playwright initialization", e);
            throw e;
        }
        return playwright;
    }

    /**
     * Создаёт браузер Chromium с полной конфигурацией.
     *
     * @param playwright движок Playwright
     * @param proxy прокси (может быть null)
     * @param headless режим headless:
     *                 true  — браузер работает без UI;
     *                 false — браузер отображается на экране.
     * @param useProxy использовать ли прокси:
     *                 true  — включить прокси;
     *                 false — работать напрямую.
     * @param site сайт‑источник (для логирования)
     *
     * @return запущенный браузер Chromium
     *
     * Параметры браузера:
     *  - {@code setHeadless(headless)} — включает/выключает UI.
     *  - {@code setSlowMo(120)} — замедляет выполнение команд (удобно для отладки).
     *  - {@code --start-maximized} — открывает окно браузера в максимальном размере.
     *  - {@code --disable-infobars} — скрывает баннеры Chrome "Chrome is being controlled by automated test software".
     *  - {@code --disable-notifications} — отключает всплывающие уведомления.
     *  - {@code --window-size=1366,768} — задаёт размер окна.
     *  - {@code --no-default-browser-check} — отключает проверку "сделать Chrome браузером по умолчанию".
     *  - {@code --no-first-run} — отключает экран первого запуска Chrome.
     *
     * Параметры прокси:
     *  - {@code new Proxy("http://host:port")} — адрес HTTP‑прокси.
     *  - {@code setUsername()} — логин прокси.
     *  - {@code setPassword()} — пароль прокси.
     */
    public Browser createBrowser(Playwright playwright, ProxyCredentials proxy, boolean headless, boolean useProxy,  SiteName site){
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(120)
                .setArgs(Arrays.asList(
                        "--start-maximized",
                        "--disable-infobars",
                        "--disable-notifications",
                        "--window-size=1366,768",
                        "--no-default-browser-check",
                        "--no-first-run"
                ));

        ProxyCredentials usedProxy = null;
        if (useProxy) {
            usedProxy = proxy != null ? proxy : proxyService.getRandomActiveProxy();
            launchOptions.setProxy(new Proxy("http://" + usedProxy.getHost() + ":" + usedProxy.getPort())
                    .setUsername(usedProxy.getUsername())
                    .setPassword(usedProxy.getPassword()));
        }

        Browser browser = null;
        try {
            browser = playwright.chromium().launch(launchOptions);
            log.debug("Браузер для {} успешно запущен (прокси: {})", site, useProxy ? "включен" : "выключен");
        } catch (Exception e) {
            log.error("Ошибка запуска браузера для {}: {}", site, e.getMessage(), e);
            throw e;
        }
        
        return browser;
    }

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
     * Закрывает все Playwright‑ресурсы:
     *  - страницу;
     *  - контекст;
     *  - браузер;
     *  - движок Playwright.
     *
     * Каждый этап закрытия логируется.
     *
     * @param page страница (может быть null)
     * @param context контекст браузера (может быть null)
     * @param browser браузер (может быть null)
     * @param playwright движок Playwright (может быть null)
     * @param site сайт‑источник (для логов)
     */
    public void closeResources(Page page, BrowserContext context, Browser browser, Playwright playwright, SiteName site) {
        // Закрываем Page
        if (page != null) {
            try {
                if (!page.isClosed()) {
                    try {
                        page.waitForLoadState(LoadState.NETWORKIDLE,
                                new Page.WaitForLoadStateOptions().setTimeout(1200));
                    } catch (Exception ignored) {
                        // Игнорируем ошибки ожидания загрузки
                    }
                    page.close();
                    log.debug("Page закрыт для {}", site);
                }
            } catch (Exception e) {
                log.error("Ошибка закрытия page в {}: {}", site, e.getMessage(), e);
            }
        }

        // Закрываем BrowserContext
        if (context != null) {
            try {
                if (!context.pages().isEmpty()) {
                    log.warn("Context для {} содержит {} незакрытых страниц, закрываем принудительно",
                            site, context.pages().size());
                    for (Page p : context.pages()) {
                        try {
                            if (!p.isClosed()) {
                                p.close();
                            }
                        } catch (Exception e) {
                            log.error("Ошибка закрытия страницы в context: {}", e.getMessage());
                        }
                    }
                }
                context.close();
                log.debug("Context закрыт для {}", site);
            } catch (Exception e) {
                log.error("Ошибка закрытия context в {}: {}", site, e.getMessage(), e);
            }
        }

        // Закрываем Browser - КРИТИЧЕСКИ ВАЖНО
        if (browser != null) {
            try {
                if (browser.isConnected()) {
                    // Принудительно закрываем все контексты
                    if (!browser.contexts().isEmpty()) {
                        log.warn("Browser для {} содержит {} незакрытых контекстов, закрываем принудительно",
                                site, browser.contexts().size());
                        for (BrowserContext ctx : browser.contexts()) {
                            try {
                                ctx.close();
                            } catch (Exception e) {
                                log.error("Ошибка закрытия контекста в browser: {}", e.getMessage());
                            }
                        }
                    }
                    browser.close();
                    log.debug("Browser закрыт для {}", site);
                } else {
                    log.debug("Browser для {} уже отключен", site);
                }
            } catch (Exception e) {
                log.error("Ошибка закрытия browser в {}: {}", site, e.getMessage(), e);
                // Пытаемся принудительно завершить процесс браузера
                try {
                    if (browser.isConnected()) {
                        log.warn("Попытка принудительного закрытия browser для {}", site);
                        browser.close();
                    }
                } catch (Exception ex) {
                    log.error("Не удалось принудительно закрыть browser: {}", ex.getMessage());
                }
            }
        }

        // Закрываем Playwright - КРИТИЧЕСКИ ВАЖНО
        if (playwright != null) {
            try {
                playwright.close();
                log.debug("Playwright закрыт для {}", site);
            } catch (PlaywrightException e) {
                log.error("Playwright instance close error для {}: {}", site, e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error closing Playwright instance для {}: {}", site, e.getMessage(), e);
            }
        }
    }

    /**
     * Создаёт новый {@link BrowserContext} с антидетект‑настройками.
     *
     * @param browser браузер
     * @param proxy прокси (может быть null)
     * @param useProxy использовать ли прокси
     * @param site сайт‑источник (для логов)
     * @return новый контекст браузера
     *
     * Параметры контекста:
     *  - {@code setViewportSize(1366, 768)} — размер окна.
     *  - {@code setUserAgent(...)} — подмена User‑Agent.
     *  - {@code setLocale("ru-RU")} — локаль браузера.
     *  - {@code setDeviceScaleFactor(1.0)} — DPI‑масштаб.
     *  - {@code setIsMobile(false)} — отключает мобильный режим.
     *  - {@code setHasTouch(false)} — отключает touch‑события.
     *
     * Антидетект‑скрипты:
     *  - подмена navigator.webdriver;
     *  - подмена window.chrome;
     *  - подмена navigator.plugins;
     *  - подмена navigator.languages;
     *  - подмена hardwareConcurrency;
     *  - подмена deviceMemory.
     *
     * Особые настройки:
     *  - FREELANCEHUNT → timezone = Europe/Berlin.
     */
    public BrowserContext createBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy, SiteName site) {
        BrowserContext context;
        Browser.NewContextOptions options;
        if (useProxy){
            ProxyCredentials usedProxy = proxy != null ? proxy : proxyService.getRandomActiveProxy();
            options = new Browser.NewContextOptions()
                    .setViewportSize(1366, 768)
                    .setUserAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/123.0.0.0 Safari/537.36"
                    )
                    .setLocale("ru-RU")
                    .setDeviceScaleFactor(1.0)
                    .setIsMobile(false)
                    .setHasTouch(false)
                    .setProxy(new Proxy("http://" + usedProxy.getHost() + ":" + usedProxy.getPort())
                            .setUsername(usedProxy.getUsername())
                            .setPassword(usedProxy.getPassword()));
        }
        else {
            options = new Browser.NewContextOptions()
                    .setViewportSize(1366, 768)
                    .setUserAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/123.0.0.0 Safari/537.36"
                    )
                    .setLocale("ru-RU")
                    .setDeviceScaleFactor(1.0)
                    .setIsMobile(false)
                    .setHasTouch(false);
        }

        if (SiteName.FREELANCEHUNT.equals(site)){
            options.setTimezoneId("Europe/Berlin");
        }

        context = browser.newContext(options);
        context.addInitScript(
                // webdriver = undefined
                "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });" +

                        // window.chrome — реалистичный объект
                        "Object.defineProperty(window, 'chrome', {" +
                        "  get: () => ({" +
                        "    runtime: {}," +
                        "    app: { isInstalled: false }," +
                        "    webstore: { onInstallStageChanged: {}, onDownloadProgress: {} }" +
                        "  })" +
                        "});" +

                        // Реалистичные плагины
                        "Object.defineProperty(navigator, 'plugins', {" +
                        "  get: () => [" +
                        "    { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer', description: 'Portable Document Format' }," +
                        "    { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai', description: '' }," +
                        "    { name: 'Native Client', filename: 'internal-nacl-plugin', description: '' }" +
                        "  ]" +
                        "});" +

                        // Локали
                        "Object.defineProperty(navigator, 'languages', { get: () => ['en-US','ru-RU','en','ru'] });" +

                        // CPU
                        "Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });" +

                        // RAM
                        "Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });"
        );
        return context;
    }
}
