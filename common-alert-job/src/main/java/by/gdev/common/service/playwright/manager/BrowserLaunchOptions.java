package by.gdev.common.service.playwright.manager;

import by.gdev.common.model.proxy.ProxyCredentials;

/**
 * Универсальные опции запуска браузера.
 * Локальный менеджер использует все поля,
 * Camoufox: proxy и headless передаются в Python-лаунчер (/launch).
 */
public record BrowserLaunchOptions(
        ProxyCredentials proxy,
        boolean headless,
        boolean useProxy
) {
    public static BrowserLaunchOptions empty() {
        return new BrowserLaunchOptions(null, false, false);
    }
}