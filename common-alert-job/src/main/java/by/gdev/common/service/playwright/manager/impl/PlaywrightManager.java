package by.gdev.common.service.playwright.manager.impl;

import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.playwright.manager.BrowserLaunchOptions;
import by.gdev.common.service.playwright.manager.PlaywrightBrowserManager;
import by.gdev.common.service.proxy.ProxyService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class PlaywrightManager implements PlaywrightBrowserManager {

    @Autowired
    private ProxyService proxyService;

    @Override
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


    @Override
    public Browser createBrowser(Playwright playwright, BrowserLaunchOptions options, SiteName site) {
        return createBrowser(
                playwright,
                options.proxy(),
                options.headless(),
                options.useProxy(),
                site
        );
    }

    @Override
    public BrowserContext createBrowserContext(Browser browser, BrowserLaunchOptions options, SiteName site) {
        return createBrowserContext(
                browser,
                options.proxy(),
                options.useProxy(),
                site
        );
    }

    public Browser createBrowser(Playwright playwright, ProxyCredentials proxy, boolean headless, boolean useProxy, SiteName site) {
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(120)
                .setArgs(Arrays.asList(
                        "--disable-blink-features=AutomationControlled",
                        "--start-maximized",
                        "--disable-infobars",
                        "--disable-notifications",
                        "--no-default-browser-check",
                        "--no-first-run",
                        "--disable-features=IsolateOrigins,site-per-process",
                        "--disable-site-isolation-trials",
                        "--ignore-certificate-errors"
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

    public ProxyCredentials getProxyWithRetry(int maxRetries, long retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ProxyCredentials proxy = proxyService.getRandomActiveProxy();
                if (proxy != null) {
                    return proxy;
                }
                log.warn("Попытка {}/{}: Нет активных прокси", attempt, maxRetries);
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * attempt);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка получения прокси с попытки {}: {}", attempt, e.getMessage(), e);
            }
        }
        log.warn("Ошибка получения прокси через {} попыток, продолжаем без прокси", maxRetries);
        return null;
    }

    public void closeResources(Page page, BrowserContext context, Browser browser, Playwright playwright, SiteName site) {
        if (page != null) {
            try {
                if (!page.isClosed()) {
                    try {
                        page.waitForLoadState(LoadState.NETWORKIDLE,
                                new Page.WaitForLoadStateOptions().setTimeout(1200));
                    } catch (Exception ignored) {}
                    page.close();
                    log.debug("Page закрыт для {}", site);
                }
            } catch (Exception e) {
                log.error("Ошибка закрытия page в {}: {}", site, e.getMessage(), e);
            }
        }
        if (context != null) {
            try {
                if (!context.pages().isEmpty()) {
                    log.warn("Context для {} содержит {} незакрытых страниц, закрываем принудительно",
                            site, context.pages().size());
                    for (Page p : context.pages()) {
                        try {
                            if (!p.isClosed()) p.close();
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
        if (browser != null) {
            try {
                if (browser.isConnected()) {
                    if (!browser.contexts().isEmpty()) {
                        log.warn("Browser для {} содержит {} незакрытых контекстов, закрываем принудительно",
                                site, browser.contexts().size());
                        for (BrowserContext ctx : browser.contexts()) {
                            try { ctx.close(); }
                            catch (Exception e) { log.error("Ошибка закрытия контекста в browser: {}", e.getMessage()); }
                        }
                    }
                    browser.close();
                    log.debug("Browser закрыт для {}", site);
                } else {
                    log.debug("Browser для {} уже отключен", site);
                }
            } catch (Exception e) {
                log.error("Ошибка закрытия browser в {}: {}", site, e.getMessage(), e);
                try {
                    if (browser.isConnected()) browser.close();
                } catch (Exception ex) {
                    log.error("Не удалось принудительно закрыть browser: {}", ex.getMessage());
                }
            }
        }
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


    public BrowserContext createBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy, SiteName site) {
        String chromeVersion = browser.version();
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/" + chromeVersion + " Safari/537.36";

        Browser.NewContextOptions options;
        if (useProxy) {
            ProxyCredentials usedProxy = proxy != null ? proxy : proxyService.getRandomActiveProxy();
            options = new Browser.NewContextOptions()
                    .setViewportSize(1366, 768)
                    .setUserAgent(userAgent)
                    .setLocale("ru-RU")
                    .setDeviceScaleFactor(1.0)
                    .setIsMobile(false)
                    .setHasTouch(false)
                    .setTimezoneId("Europe/Berlin")
                    .setProxy(new Proxy("http://" + usedProxy.getHost() + ":" + usedProxy.getPort())
                            .setUsername(usedProxy.getUsername())
                            .setPassword(usedProxy.getPassword()));
        } else {
            options = new Browser.NewContextOptions()
                    .setViewportSize(1366, 768)
                    .setUserAgent(userAgent)
                    .setLocale("ru-RU")
                    .setDeviceScaleFactor(1.0)
                    .setIsMobile(false)
                    .setHasTouch(false)
                    .setTimezoneId("Europe/Berlin");
        }

        BrowserContext context = browser.newContext(options);

        context.addInitScript(
                "(() => {" +

                        // webdriver = undefined
                        "  Object.defineProperty(navigator, 'webdriver', { get: () => undefined });" +

                        // Убираем следы CDP-объектов
                        "  delete window.cdc_adoQpoasnfa76pfcZLmcfl_Array;" +
                        "  delete window.cdc_adoQpoasnfa76pfcZLmcfl_Promise;" +
                        "  delete window.cdc_adoQpoasnfa76pfcZLmcfl_Symbol;" +

                        // Реалистичный window.chrome
                        "  if (!window.chrome) window.chrome = {};" +
                        "  window.chrome.runtime = window.chrome.runtime || {};" +
                        "  window.chrome.app = {" +
                        "    isInstalled: false," +
                        "    InstallState: { DISABLED: 'disabled', INSTALLED: 'installed', NOT_INSTALLED: 'not_installed' }," +
                        "    RunningState: { CANNOT_RUN: 'cannot_run', READY_TO_RUN: 'ready_to_run', RUNNING: 'running' }" +
                        "  };" +
                        "  window.chrome.webstore = {" +
                        "    onInstallStageChanged: {}, onDownloadProgress: {}" +
                        "  };" +
                        "  window.chrome.csi = function() { return { onloadT: Date.now(), startE: Date.now(), pageT: 100, tran: 15 }; };" +
                        "  window.chrome.loadTimes = function() {" +
                        "    return {" +
                        "      commitLoadTime: Date.now() / 1000," +
                        "      connectionInfo: 'h2'," +
                        "      finishDocumentLoadTime: Date.now() / 1000," +
                        "      finishLoadTime: Date.now() / 1000," +
                        "      firstPaintTime: Date.now() / 1000," +
                        "      firstPaintAfterLoadTime: 0," +
                        "      navigationType: 'Other'," +
                        "      npnNegotiatedProtocol: 'h2'," +
                        "      requestTime: Date.now() / 1000," +
                        "      startLoadTime: Date.now() / 1000," +
                        "      wasAlternateProtocolAvailable: false," +
                        "      wasFetchedViaSpdy: true," +
                        "      wasNpnNegotiated: true" +
                        "    };" +
                        "  };" +

                        // Плагины (полный список, как в реальном Chrome)
                        "  const makePlugin = (name, filename, desc, mimes) => {" +
                        "    const plugin = Object.create(Plugin.prototype);" +
                        "    Object.defineProperties(plugin, {" +
                        "      name: { value: name }," +
                        "      filename: { value: filename }," +
                        "      description: { value: desc }," +
                        "      length: { value: mimes.length }" +
                        "    });" +
                        "    mimes.forEach((m, i) => { plugin[i] = m; });" +
                        "    return plugin;" +
                        "  };" +
                        "  const makeMime = (type, suffixes, desc) => {" +
                        "    const mime = Object.create(MimeType.prototype);" +
                        "    Object.defineProperties(mime, {" +
                        "      type: { value: type }," +
                        "      suffixes: { value: suffixes }," +
                        "      description: { value: desc }" +
                        "    });" +
                        "    return mime;" +
                        "  };" +
                        "  const pdfMime = makeMime('application/pdf', 'pdf', 'Portable Document Format');" +
                        "  const naclMime = makeMime('application/x-nacl', '', 'Native Client Executable');" +
                        "  const pnaclMime = makeMime('application/x-pnacl', '', 'Portable Native Client Executable');" +
                        "  const pluginsArr = [" +
                        "    makePlugin('PDF Viewer', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                        "    makePlugin('Chrome PDF Viewer', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                        "    makePlugin('Chromium PDF Viewer', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                        "    makePlugin('Microsoft Edge PDF Viewer', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                        "    makePlugin('WebKit built-in PDF', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                        "    makePlugin('Native Client', 'internal-nacl-plugin', '', [naclMime, pnaclMime])" +
                        "  ];" +
                        "  Object.defineProperty(navigator, 'plugins', {" +
                        "    get: () => {" +
                        "      const arr = [...pluginsArr];" +
                        "      arr.item = i => arr[i];" +
                        "      arr.namedItem = n => arr.find(p => p.name === n);" +
                        "      arr.refresh = () => {};" +
                        "      return arr;" +
                        "    }" +
                        "  });" +

                        // mimeTypes
                        "  Object.defineProperty(navigator, 'mimeTypes', {" +
                        "    get: () => {" +
                        "      const arr = [pdfMime, naclMime, pnaclMime];" +
                        "      arr.item = i => arr[i];" +
                        "      arr.namedItem = n => arr.find(m => m.type === n);" +
                        "      return arr;" +
                        "    }" +
                        "  });" +

                        // Языки
                        "  Object.defineProperty(navigator, 'languages', { get: () => ['ru-RU', 'ru', 'en-US', 'en'] });" +

                        // Железо
                        "  Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });" +
                        "  Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });" +
                        "  Object.defineProperty(navigator, 'maxTouchPoints', { get: () => 0 });" +

                        // platform
                        "  Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });" +
                        "  Object.defineProperty(navigator, 'vendor', { get: () => 'Google Inc.' });" +
                        "  Object.defineProperty(navigator, 'productSub', { get: () => '20030107' });" +

                        // WebGL1/WebGL2 — vendor/renderer
                        "  const patchGL = (proto) => {" +
                        "    if (!proto) return;" +
                        "    const orig = proto.getParameter;" +
                        "    proto.getParameter = function(p) {" +
                        "      if (p === 37445) return 'Intel Inc.';" +
                        "      if (p === 37446) return 'Intel Iris OpenGL Engine';" +
                        "      return orig.apply(this, arguments);" +
                        "    };" +
                        "  };" +
                        "  patchGL(window.WebGLRenderingContext && WebGLRenderingContext.prototype);" +
                        "  patchGL(window.WebGL2RenderingContext && WebGL2RenderingContext.prototype);" +

                        // Canvas fingerprint — микро-шум
                        "  const origToDataURL = HTMLCanvasElement.prototype.toDataURL;" +
                        "  HTMLCanvasElement.prototype.toDataURL = function() {" +
                        "    const ctx = this.getContext('2d');" +
                        "    if (ctx) {" +
                        "      const img = ctx.getImageData(0, 0, this.width, this.height);" +
                        "      for (let i = 0; i < img.data.length; i += 1000) {" +
                        "        img.data[i] = img.data[i] ^ 1;" +
                        "      }" +
                        "      ctx.putImageData(img, 0, 0);" +
                        "    }" +
                        "    return origToDataURL.apply(this, arguments);" +
                        "  };" +

                        // Permissions
                        "  const origQuery = window.navigator.permissions.query;" +
                        "  window.navigator.permissions.query = (params) => (" +
                        "    params.name === 'notifications'" +
                        "      ? Promise.resolve({ state: Notification.permission })" +
                        "      : origQuery(params)" +
                        "  );" +

                        // Connection
                        "  Object.defineProperty(navigator, 'connection', {" +
                        "    get: () => ({" +
                        "      downlink: 10," +
                        "      effectiveType: '4g'," +
                        "      rtt: 50," +
                        "      saveData: false," +
                        "      onchange: null" +
                        "    })" +
                        "  });" +

                        // WebRTC — оставляем настоящий, но фильтруем через mediaDevices
                        "  if (navigator.mediaDevices && navigator.mediaDevices.enumerateDevices) {" +
                        "    const origEnum = navigator.mediaDevices.enumerateDevices.bind(navigator.mediaDevices);" +
                        "    navigator.mediaDevices.enumerateDevices = () => origEnum();" +
                        "  }" +

                        // Убираем следы Playwright в Error.stack
                        "  Error.prepareStackTrace = (err, stack) => stack;" +

                        "})();"
        );

        return context;
    }
}