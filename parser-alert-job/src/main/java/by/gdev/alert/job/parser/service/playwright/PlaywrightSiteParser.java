package by.gdev.alert.job.parser.service.playwright;


import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.service.order.AbsctractSiteParser;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.playwright.PlaywrightManager;
import by.gdev.common.util.Pair;
import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;


/**
 * Абстрактный парсер для сайтов, требующих динамического рендеринга через Playwright.
 * Предоставляет:
 *  - создание Playwright‑сессии (браузер, контекст, страница);
 *  - работу с прокси и retry‑логикой;
 *  - обработку ошибок Playwright;
 *  - вспомогательные методы для кликов и повторных попыток;
 *  - общий цикл получения заказов через Playwright.
 *
 * Конкретные реализации должны переопределить методы:
 *  - {@link #mapItems(String, Long, List)}
 *  - {@link #mapPlaywrightItems(String, Long, Pair, Page)}
 *  - {@link #mapPlaywrightItems(String, Long, Category, Subcategory)}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public abstract class PlaywrightSiteParser extends AbsctractSiteParser {

    /**
     * Менеджер Playwright, отвечающий за создание браузеров, контекстов, страниц и прокси.
     */
    @Autowired
    private PlaywrightManager playwrightManager;


    /**
     * Количество попыток повторного выполнения операции Playwright.
     */
    @Value("${parser.site.retry.attempts:3}")
    private int retryAttempts;


    /**
     * Количество попыток клика по элементу категории/подкатегории.
     */
    @Value("${parser.category-click-retry-attempts:3}")
    @Getter
    private int categoryClickRetryAttempts;

    /**
     * Задержка между попытками клика (мс).
     */
    @Value("${parser.category-click-retry-attempts-delay:500}")
    @Getter
    private int categoryClickRetryAttemptsDelay;

    /**
     * Базовая задержка между retry‑попытками Playwright (мс).
     */
    @Value("${parser.site.retry.delay:2000}")
    private long retryDelayMs;


    /**
     * Сырые строки ошибок Playwright, которые должны игнорироваться (CSV).
     */
    @Value("${parser.retry.ignored-errors:}")
    private String ignoredErrorsRaw;

    /**
     * Список ошибок Playwright, которые будут игнорироваться при retry.
     */
    private List<String> ignoredErrors;

    /**
     * Флаг режима отладки Playwright - дополнительные логи и задержки.
     */
    protected boolean debug;

    /**
     * Флаг headless‑режима браузера.
     */
    protected boolean headless;


    /**
     * Инициализирует список ошибок Playwright, которые должны игнорироваться при retry.
     * Список берётся из конфигурации (CSV‑строка).
     */
    @PostConstruct
    private void initIgnoredErrors() {
        if (ignoredErrorsRaw == null || ignoredErrorsRaw.isBlank()) {
            ignoredErrors = List.of();
            return;
        }

        ignoredErrors = Arrays.stream(ignoredErrorsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        log.debug("Игнорируемые ошибки Playwright: {}", ignoredErrors);
    }

    /**
     * Получает прокси с retry‑логикой.
     *
     * @param maxRetries количество попыток
     * @param retryDelayMs задержка между попытками
     * @return {@link ProxyCredentials} или null, если прокси не удалось получить
     */
    protected ProxyCredentials getProxyWithRetry(int maxRetries, long retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ProxyCredentials proxy = playwrightManager.getProxyWithRetry(maxRetries, retryDelayMs);
                if (proxy != null) {;
                    return proxy;
                }
            } catch (Exception e) {
                log.error("Error getting proxy on attempt {} for {}: {}",
                        attempt, getSiteName(), e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Создаёт новый экземпляр {@link Playwright}.
     *
     * @return объект Playwright
     */
    public Playwright createPlaywright() {
        return playwrightManager.createPlaywright();
    }

    /**
     * Закрывает все ресурсы Playwright.
     *
     * @param page страница
     * @param context контекст браузера
     * @param browser браузер
     * @param playwright движок Playwright
     */
    protected void closeResources(Page page, BrowserContext context, Browser browser, Playwright playwright) {
        playwrightManager.closeResources(page, context , browser, playwright, getSiteName());
    }

    /**
     * Создаёт новый {@link BrowserContext}.
     *
     * @param browser браузер
     * @param proxy прокси (может быть null)
     * @param useProxy использовать ли прокси
     * @return новый контекст браузера
     */
    protected BrowserContext createBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy) {
        return playwrightManager.createBrowserContext(browser, proxy, useProxy, getSiteName());
    }

    /**
     * Создаёт браузер Playwright.
     *
     * @param playwright движок Playwright
     * @param proxy прокси (может быть null)
     * @param headless режим headless
     * @param isActiveProxy использовать ли прокси
     * @return браузер
     */
    protected Browser createBrowser(Playwright playwright, ProxyCredentials proxy, boolean headless, boolean isActiveProxy){
        return playwrightManager.createBrowser(playwright, proxy, headless, isActiveProxy, getSiteName());
    }

    /**
     * Хранит полную Playwright‑сессию:
     *  - движок Playwright;
     *  - браузер;
     *  - контекст;
     *  - страницу;
     *  - прокси.
     */
    @Getter
    @AllArgsConstructor
    protected static class PlaywrightSession {
        private final Playwright playwright;
        private final Browser browser;
        private final BrowserContext context;
        private final Page page;
        private final ProxyCredentials proxy;
    }

    /**
     * Создаёт Playwright‑сессию.
     *
     * @param headless режим headless
     * @param useProxy использовать ли прокси
     * @return {@link PlaywrightSession}
     */
    protected PlaywrightSession createSession(boolean headless, boolean useProxy) {
        Playwright playwright = createPlaywright();
        ProxyCredentials proxy = useProxy ? getProxyWithRetry(5, 2000) : null;

        Browser browser = createBrowser(playwright, proxy, headless, useProxy);
        BrowserContext context = createBrowserContext(browser, proxy, useProxy);
        Page page = context.newPage();
        return new PlaywrightSession(playwright, browser, context, page, proxy);
    }

    /**
     * Переопределяет стандартный {@link AbsctractSiteParser#parse()},
     * так как Playwright‑парсеры используют другой цикл обработки.
     *
     * @return список заказов
     */
    @Override
    public List<OrderDTO> parse() {
        SiteSourceJob siteSourceJob = getSiteSourceJobRepository().findWithCategories(getSiteName().getId());
        if (siteSourceJob == null) {
            return List.of();
        }
        return getOrdersForPlayright(siteSourceJob);
    }

    /**
     * Получает заказы через Playwright.
     *
     * @param siteSourceJob объект с категориями и настройками сайта
     * @return список заказов
     */
    public List<OrderDTO> getOrdersForPlayright(SiteSourceJob siteSourceJob) {
        List<Pair<Category, Subcategory>>  categoriesPairList = getCategoriesPairListForJob(siteSourceJob);
        return mapItems(siteSourceJob.getParsedURI(), siteSourceJob.getId(), categoriesPairList);
    }

    /**
     * Формирует список пар (категория, подкатегория), которые нужно парсить.
     *
     * @param siteSourceJob источник с категориями
     * @return список пар
     */
    List<Pair<Category, Subcategory>> getCategoriesPairListForJob(SiteSourceJob siteSourceJob){
        List<Pair<Category, Subcategory>> categoriesPairList = new ArrayList<>();
        siteSourceJob.getCategories().forEach(category -> {
            Set<Subcategory> siteSubCategories = category.getSubCategories();
            if (category.isParse()) {
                Pair<Category, Subcategory> categoryPair = new Pair<>(category, null);
                categoriesPairList.add(categoryPair);
            }
            siteSubCategories.stream()
                    .filter(Subcategory::isParse)
                    .forEach(subCategory -> {
                        Pair<Category, Subcategory> subCategoryPair = new Pair<>(category, subCategory);
                        categoriesPairList.add(subCategoryPair);
                    });
        });
        return categoriesPairList;
    }

    /**
     * Универсальный механизм retry для операций Playwright.
     *
     * @param operation операция, возвращающая список заказов
     * @param operationDescription описание операции для логов
     * @return результат операции или пустой список
     */
    private List<OrderDTO> executeWithRetry(Supplier<List<OrderDTO>> operation,
                                            String operationDescription) {
        Exception lastIgnored = null;
        Exception lastNonIgnored = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                log.debug("Попытка {}/{} {}",
                        attempt, retryAttempts, operationDescription);
                return operation.get();
            } catch (PlaywrightException e) {
                boolean isIgnored = ignoredErrors.stream()
                        .anyMatch(err -> e.getMessage() != null && e.getMessage().contains(err));

                if (isIgnored) {
                    lastIgnored = e;
                    continue;
                }

                lastNonIgnored = e;
                log.error("Playwright ошибка на попытке {} для {}: {}",
                        attempt, getSiteName(), e.getMessage());
            } catch (Exception e) {
                lastNonIgnored = e;
                log.error("Неожиданная ошибка на попытке {} для {}: {}",
                        attempt, getSiteName(), e.getMessage());
            }
            // Задержка между попытками
            if (attempt < retryAttempts) {
                try {
                    long delay = retryDelayMs * (long) Math.pow(2, attempt - 1);
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                }
            }
        }

        if (lastNonIgnored != null) {
            log.error("Все {} попытки {} провалились. Последняя ошибка: {}",
                    retryAttempts, operationDescription, lastNonIgnored.getMessage());
            throw new RuntimeException("Все попытки " + operationDescription + " провалились", lastNonIgnored);
        }

        if (lastIgnored != null) {
            log.warn("Все {} попытки {} завершились предупреждениями. Последнее: {}",
                    retryAttempts, operationDescription, lastIgnored.getMessage());;
        }

        return List.of();
    }

    /**
     * Выполняет парсинг категории/подкатегории с retry‑логикой.
     *
     * @param link URL страницы
     * @param proxyActive использовать ли прокси
     * @param siteSourceJobId ID источника
     * @param category категория
     * @param subCategory подкатегория (может быть null)
     * @return список заказов
     */
    public List<OrderDTO> mapItemsWithRetry(String link, boolean proxyActive,
                                            Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active) return List.of();

        String description = String.format("парсинга %s: категория '%s', подкатегория '%s', прокси %s",
                getSiteName(),
                category.getNativeLocName(),
                subCategory != null ? subCategory.getNativeLocName() : "нет",
                proxyActive ? "включен" : "выключен");

        return executeWithRetry(
                () -> mapPlaywrightItems(link, siteSourceJobId, category, subCategory),
                description
        );
    }

    /**
     * Версия mapItemsWithRetry для заранее созданной Playwright‑страницы.
     *
     * @param link URL страницы
     * @param proxyActive использовать ли прокси
     * @param siteSourceJobId ID источника
     * @param pair пара категория/подкатегория
     * @param page Playwright‑страница
     * @return список заказов
     */
    public List<OrderDTO> mapItemsWithRetry(String link, boolean proxyActive,
                                            Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page) {
        if (!active) return List.of();

        Category category = pair.getLeft();
        Subcategory subCategory = pair.getRight();

        String description = String.format("парсинга %s: категория '%s', подкатегория '%s', прокси %s",
                getSiteName(),
                category.getNativeLocName(),
                subCategory != null ? subCategory.getNativeLocName() : "нет",
                proxyActive ? "включен" : "выключен");

        return executeWithRetry(
                () -> mapPlaywrightItems(link, siteSourceJobId, pair, page),
                description
        );
    }

    /**
     * Функциональный интерфейс для действий клика.
     */
    @FunctionalInterface
    public interface ClickAction {
        void click();
    }

    /**
     * Выполняет клик с несколькими попытками.
     * Проверяет изменение URL как признак успешного клика.
     *
     * @param page страница Playwright
     * @param name имя элемента для логов
     * @param action действие клика
     * @return true, если клик успешен
     */
    public boolean clickWithRetry(Page page, String name, ClickAction action) {
        for (int attempt = 1; attempt <= categoryClickRetryAttempts; attempt++) {
            String beforeUrl = page.url();
            try {
                action.click();
            } catch (Exception e) {
                log.warn("Ошибка при клике по '{}', попытка {}", name, attempt, e);
            }
            page.waitForTimeout(categoryClickRetryAttemptsDelay);
            String afterUrl = page.url();
            if (!afterUrl.equals(beforeUrl)) {
                log.debug("'{}' выбран успешно (попытка {})", name, attempt);
                return true;
            }
            log.warn("URL не изменился после клика по '{}' (попытка {})", name, attempt);
        }
        log.warn("'{}' НЕ выбран после 3 попыток", name);
        return false;
    }

    /**
     * Парсинг всех категорий/подкатегорий через Playwright.
     *
     * @param link URL страницы
     * @param siteSourceJobId ID источника
     * @param categoriesPairList список пар категория/подкатегория
     * @return список заказов
     */
    protected abstract List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList);

    /**
     * Парсинг одной категории/подкатегории через Playwright (с готовой страницей).
     *
     * @param link URL страницы
     * @param siteSourceJobId ID источника
     * @param pair пара категория/подкатегория
     * @param page Playwright‑страница
     * @return список заказов
     */
    protected abstract List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page);

    /**
     * Парсинг одной категории/подкатегории через Playwright (создаёт страницу самостоятельно).
     *
     * @param link URL страницы
     * @param siteSourceJobId ID источника
     * @param c категория
     * @param sub подкатегория (может быть null)
     * @return список заказов
     */
    protected abstract List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category c, Subcategory sub);
}
