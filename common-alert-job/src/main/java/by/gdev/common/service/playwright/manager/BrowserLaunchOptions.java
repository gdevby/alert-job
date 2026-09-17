package by.gdev.common.service.playwright.manager;

import by.gdev.common.model.proxy.ProxyCredentials;

/**
 * Универсальные опции запуска браузера.
 * Локальный менеджер использует все поля,
 * Camoufox игнорирует proxy/headless (они заданы на Python-сервере).
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