package by.gdev.common.service.playwright.captcha;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component("captchaServiceCommon")
@Slf4j
@RequiredArgsConstructor
public class CaptchaService {

    private static final String WEBLANCER_COOKIE_URL = "https://www.weblancer.net";
    private static final Pattern SITEKEY_QUERY = Pattern.compile("[?&]sitekey=([^&]+)", Pattern.CASE_INSENSITIVE);

    private final TwoCaptchaClient twoCaptchaClient;

    @Value("${captcha.cloudflare.enabled:true}")
    private boolean cloudflareEnabled;

    @Value("${captcha.cloudflare.timeout-ms:45000}")
    private long cloudflareTimeoutMs;

    @Value("${captcha.yandex.two-captcha-fallback:true}")
    private boolean yandexTwoCaptchaFallback;

    @Value("${captcha.yandex.two-captcha-first:false}")
    private boolean yandexTwoCaptchaFirst;

    private Frame findYandexCaptchaFrame(Page page) {
        for (Frame f : page.frames()) {
            String url = f.url();
            if (url != null && (url.contains("smartcaptcha.yandexcloud.net")
                    || url.contains("captcha.yandex")
                    || url.contains("checkbox")
                    || url.contains("advanced"))) {
                return f;
            }
        }
        return null;
    }

    /** Ждёт появления виджета SmartCaptcha (логин FL.ru, /validate-captcha и т.д.). */
    public void waitForYandexSmartCaptchaWidget(Page page, long timeoutMs) {
        try {
            page.waitForSelector(
                    "iframe[src*='smartcaptcha.yandexcloud.net'], iframe[data-testid='checkbox-iframe'], "
                            + "iframe[data-testid='advanced-iframe'], .smart-captcha, [data-sitekey]",
                    new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
        } catch (Exception e) {
            log.debug("SmartCaptcha: виджет не дождались за {} ms: {}", timeoutMs, e.getMessage());
        }
    }

    private boolean clickYandexSmartCaptcha(Page page, Frame frame) {
        try {
            Locator iframeLocator = page.locator(
                    "iframe[data-testid='checkbox-iframe'], iframe[src*='smartcaptcha.yandexcloud.net']");
            if (iframeLocator.count() == 0) {
                log.warn("SmartCaptcha: iframe виджета не найден на странице");
                return false;
            }
            BoundingBox box = iframeLocator.first().boundingBox();
            if (box == null) {
                log.warn("SmartCaptcha: не удалось получить boundingBox iframe");
                return false;
            }
            page.mouse().click(box.x + 5, box.y + 5);
            Thread.sleep(200);

            frame.waitForSelector("input#js-button",
                    new Frame.WaitForSelectorOptions().setTimeout(10000));
            frame.evaluate("window.postMessage(JSON.stringify({methodCall: 'start'}), '*')");
            Thread.sleep(1500);
            Locator input = frame.locator("input#js-button");
            String aria = input.getAttribute("aria-checked");
            if ("true".equals(aria)) {
                log.debug("SmartCaptcha: aria-checked=true — капча пройдена");
                return true;
            }
            String checked = frame.locator(".CheckboxCaptcha-Checkbox")
                    .getAttribute("data-checked");
            if ("true".equals(checked)) {
                log.debug("SmartCaptcha: data-checked=true — капча пройдена");
                return true;
            }
            if (isSmartCaptchaOverlayVisible(page)) {
                log.warn("SmartCaptcha: локальный клик не прошёл — открыт advanced challenge (оверлей поверх формы)");
            } else {
                log.warn("SmartCaptcha: input/data-checked не изменились");
            }
            return false;
        } catch (Exception e) {
            log.error("SmartCaptcha: ошибка при клике", e);
            return false;
        }
    }

    /** Скрывает full-screen оверлей Yandex SmartCaptcha (advanced), чтобы не перехватывал клики по «Войти». */
    public void dismissSmartCaptchaOverlay(Page page) {
        try {
            page.evaluate(
                    """
                            () => {
                              const selectors = [
                                '[data-testid="advanced-container"]',
                                '.SmartCaptcha-Overlay',
                                '.SmartCaptcha-Overlay_visible'
                              ];
                              for (const sel of selectors) {
                                document.querySelectorAll(sel).forEach((el) => {
                                  el.style.display = 'none';
                                  el.style.visibility = 'hidden';
                                  el.style.pointerEvents = 'none';
                                  el.classList.remove(
                                    'SmartCaptcha-Overlay_visible',
                                    'SmartCaptcha-Overlay_show_spinner'
                                  );
                                });
                              }
                            }
                            """);
            log.debug("SmartCaptcha: оверлей advanced скрыт (если был)");
        } catch (Exception e) {
            log.debug("SmartCaptcha: не удалось скрыть оверлей: {}", e.getMessage());
        }
    }

    public boolean isSmartCaptchaOverlayVisible(Page page) {
        try {
            Locator overlay = page.locator(
                    "[data-testid='advanced-container'].SmartCaptcha-Overlay_visible, "
                            + ".SmartCaptcha-Overlay.SmartCaptcha-Overlay_visible");
            return overlay.count() > 0 && overlay.first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    /** Капча на форме логина FL.ru (Yandex SmartCaptcha). Если виджета нет — считаем, что капчи нет. */
    public boolean solveYandexSmartCaptcha(Page page) {
        return solveYandexSmartCaptchaInternal(page, false);
    }

    /**
     * Снимок DOM/виджета для отладки {@code /validate-captcha} (смотри строки {@code validate-captcha DBG} в логе).
     */
    public void logValidateCaptchaState(Page page, String phase) {
        try {
            log.info("validate-captcha DBG [{}] url={}", phase, page.url());
            int hcaptchaIframes = page.locator("iframe[src*='hcaptcha.com']").count();
            log.info("validate-captcha DBG [{}] hcaptcha-iframes={} h-captcha-div={}",
                    phase, hcaptchaIframes, page.locator(".h-captcha").count());
            if (hcaptchaIframes > 0) {
                BoundingBox box = page.locator("iframe[src*='hcaptcha.com']").first().boundingBox();
                if (box != null) {
                    log.info("validate-captcha DBG [{}] iframe-bbox x={} y={} w={} h={} clickTarget=({}, {})",
                            phase, (int) box.x, (int) box.y, (int) box.width, (int) box.height,
                            (int) (box.x + Math.min(28, box.width * 0.12)), (int) (box.y + box.height / 2));
                } else {
                    log.info("validate-captcha DBG [{}] iframe-bbox=null (не виден / не отрисован)", phase);
                }
            }
            Object dom = page.evaluate(
                    """
                            () => {
                              const cap = document.querySelector('.h-captcha');
                              const ta = document.querySelector('textarea[name="h-captcha-response"]');
                              return JSON.stringify({
                                hcaptchaApi: typeof window.hcaptcha,
                                sendCaptcha: typeof window.sendCaptcha,
                                captchaForm: !!document.getElementById('captcha-form'),
                                csrfToken: !!(document.querySelector('input[name="_token"]')),
                                sitekey: cap ? cap.getAttribute('data-sitekey') : null,
                                callback: cap ? cap.getAttribute('data-callback') : null,
                                responseLen: ta && ta.value ? ta.value.length : 0,
                                yandexOverlay: !!document.querySelector('.SmartCaptcha-Overlay_visible')
                              });
                            }
                            """);
            log.info("validate-captcha DBG [{}] dom={}", phase, dom);
            log.info("validate-captcha DBG [{}] twoCaptchaOn={} responseField={} yandexOverlayVisible={}",
                    phase, twoCaptchaClient.isConfigured(), isHCaptchaResponsePresent(page),
                    isSmartCaptchaOverlayVisible(page));
        } catch (Exception e) {
            log.warn("validate-captcha DBG [{}] snapshot failed: {}", phase, e.getMessage());
        }
    }

    /**
     * FL.ru {@code /validate-captcha} после редиректа (IP-блок).
     * На странице используется <strong>hCaptcha</strong> («Я человек»), не Yandex SmartCaptcha с логина.
     */
    public boolean solveFlRuValidateCaptcha(Page page) {
        try {
            log.info("FL.ru validate-captcha: сценарий IP-блокировки (не форма логина)");
            logValidateCaptchaState(page, "01-start");
            waitForFlRuValidateCaptchaWidget(page);
            logValidateCaptchaState(page, "02-after-wait-widget");

            if (isHCaptchaPresent(page)) {
                boolean ok = solveFlRuValidateHCaptcha(page);
                logValidateCaptchaState(page, ok ? "99-hcaptcha-ok" : "99-hcaptcha-fail");
                return ok;
            }

            if (hasYandexSmartCaptchaWidget(page)) {
                log.debug("validate-captcha: найден Yandex SmartCaptcha");
                if (solveYandexSmartCaptchaInternal(page, true)) {
                    dismissSmartCaptchaOverlay(page);
                    return true;
                }
            }

            if (clickFlRuValidateHumanWidget(page)) {
                page.waitForTimeout(2000);
                dismissSmartCaptchaOverlay(page);
                if (isFlRuValidateCaptchaLikelyPassed(page)) {
                    log.info("validate-captcha: пройдено кликом «Я человек»");
                    return true;
                }
            }

            if (yandexTwoCaptchaFallback && twoCaptchaClient.isConfigured()) {
                Frame frame = findYandexCaptchaFrame(page);
                Frame tokenFrame = frame != null ? frame : page.mainFrame();
                if (extractYandexSiteKey(page).isPresent()
                        && solveYandexSmartCaptchaViaTwoCaptcha(page, tokenFrame)) {
                    dismissSmartCaptchaOverlay(page);
                    log.info("validate-captcha: пройдено через 2Captcha (Yandex)");
                    return true;
                }
            }

            log.warn("validate-captcha: не удалось пройти IP-капчу");
            logValidateCaptchaState(page, "99-fail-no-hcaptcha-path");
            return false;
        } catch (Exception e) {
            log.error("validate-captcha: ошибка", e);
            logValidateCaptchaState(page, "99-exception");
            return false;
        }
    }

    private boolean solveFlRuValidateHCaptcha(Page page) {
        log.info("validate-captcha: обнаружена hCaptcha (FL.ru IP-блок)");
        logValidateCaptchaState(page, "10-hcaptcha-start");

        // Сначала 2Captcha без клика по чекбоксу — иначе откроется visual challenge с картинками,
        // который локально не решается.
        if (twoCaptchaClient.isConfigured()) {
            log.info("validate-captcha DBG [11] шаг: 2Captcha hCaptcha (без клика по чекбоксу)");
            boolean twoCapOk = solveHCaptchaViaTwoCaptcha(page);
            logValidateCaptchaState(page, "12-after-2captcha-hcaptcha ok=" + twoCapOk);
            if (twoCapOk && waitFlRuValidateCaptchaCleared(page)) {
                log.info("validate-captcha: hCaptcha пройдена через 2Captcha (без puzzle)");
                return true;
            }
            log.info("validate-captcha DBG [13] 2Captcha hCaptcha не увёл со страницы, пробуем Yandex overlay");
            if (trySolveValidateCaptchaVisualOverlay(page) && waitFlRuValidateCaptchaCleared(page)) {
                log.info("validate-captcha: visual challenge пройден через 2Captcha (Yandex token)");
                return true;
            }
            logValidateCaptchaState(page, "14-after-2captcha-yandex-fallback");
        } else {
            log.warn("validate-captcha DBG [11] 2Captcha выключен — только локальный клик");
        }

        log.info("validate-captcha DBG [20] шаг: локальный клик по чекбоксу hCaptcha");
        logValidateCaptchaState(page, "20-before-checkbox-click");
        if (clickHCaptchaCheckbox(page)) {
            page.waitForTimeout(2500);
            logValidateCaptchaState(page, "21-after-checkbox-click");
            if (trySolveValidateCaptchaVisualOverlay(page) && waitFlRuValidateCaptchaCleared(page)) {
                return true;
            }
            if (waitFlRuValidateCaptchaCleared(page)) {
                log.info("validate-captcha: hCaptcha пройдена локальным кликом (без puzzle)");
                return true;
            }
            log.info("validate-captcha DBG [22] после клика — sendCaptcha/submit");
            submitFlRuValidateCaptchaForm(page);
            page.waitForTimeout(2000);
            logValidateCaptchaState(page, "23-after-submit");
            if (trySolveValidateCaptchaVisualOverlay(page) && waitFlRuValidateCaptchaCleared(page)) {
                return true;
            }
            if (waitFlRuValidateCaptchaCleared(page)) {
                log.info("validate-captcha: hCaptcha + submit формы");
                return true;
            }
        } else {
            log.warn("validate-captcha DBG [20] локальный клик по чекбоксу не выполнен");
        }

        if (isSmartCaptchaOverlayVisible(page) || findYandexCaptchaFrame(page) != null) {
            log.warn("validate-captcha: открыт visual challenge (картинки) — локально не решается, нужен 2Captcha");
        }
        log.warn("validate-captcha: hCaptcha не пройдена");
        logValidateCaptchaState(page, "29-hcaptcha-fail");
        return false;
    }

    /** Картинки «выберите всё, что…» — Yandex SmartCaptcha overlay; решается только токеном 2Captcha. */
    private boolean trySolveValidateCaptchaVisualOverlay(Page page) {
        if (!isSmartCaptchaOverlayVisible(page) && findYandexCaptchaFrame(page) == null) {
            return false;
        }
        if (!twoCaptchaClient.isConfigured()) {
            log.warn("validate-captcha: visual challenge без TWO_CAPTCHA_ENABLED");
            return false;
        }
        Optional<String> siteKey = extractYandexSiteKey(page);
        if (siteKey.isEmpty()) {
            log.warn("validate-captcha: visual challenge — sitekey Yandex не найден");
            return false;
        }
        Frame frame = findYandexCaptchaFrame(page);
        Frame tokenFrame = frame != null ? frame : page.mainFrame();
        if (!solveYandexSmartCaptchaViaTwoCaptcha(page, tokenFrame)) {
            return false;
        }
        dismissSmartCaptchaOverlay(page);
        submitFlRuValidateCaptchaForm(page);
        page.waitForTimeout(1500);
        return true;
    }

    private boolean waitFlRuValidateCaptchaCleared(Page page) {
        long deadline = System.currentTimeMillis() + 20_000;
        int tick = 0;
        while (System.currentTimeMillis() < deadline) {
            tick++;
            if (!page.url().contains("validate-captcha") && !isHCaptchaPresent(page)) {
                log.info("validate-captcha DBG [wait] страница покинута за {} проверок, url={}", tick, page.url());
                return true;
            }
            if (isHCaptchaResponsePresent(page)) {
                log.info("validate-captcha DBG [wait] h-captcha-response заполнен (tick={})", tick);
                page.waitForTimeout(1500);
                if (!page.url().contains("validate-captcha")) {
                    return true;
                }
            }
            if (tick == 1 || tick % 10 == 0) {
                log.info("validate-captcha DBG [wait] tick={} url={} response={}",
                        tick, page.url(), isHCaptchaResponsePresent(page));
            }
            page.waitForTimeout(400);
        }
        boolean left = !page.url().contains("validate-captcha");
        log.warn("validate-captcha DBG [wait] timeout 20s, left={}, url={}", left, page.url());
        return left;
    }

    private boolean isHCaptchaPresent(Page page) {
        try {
            if (page.locator("iframe[src*='hcaptcha.com']").count() > 0) {
                return true;
            }
            if (page.locator(".h-captcha, [data-hcaptcha-widget-id]").count() > 0) {
                return true;
            }
            return page.locator("text=hCaptcha").count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHCaptchaResponsePresent(Page page) {
        try {
            Object len = page.evaluate(
                    """
                            () => {
                              const ta = document.querySelector('textarea[name="h-captcha-response"]');
                              if (ta && ta.value && ta.value.length > 20) return ta.value.length;
                              const ta2 = document.querySelector('[name="g-recaptcha-response"]');
                              if (ta2 && ta2.value && ta2.value.length > 20) return ta2.value.length;
                              return 0;
                            }
                            """);
            return len instanceof Number number && number.longValue() > 20;
        } catch (Exception e) {
            return false;
        }
    }

    private void waitForHcaptchaApi(Page page) {
        try {
            log.info("validate-captcha DBG [api] ожидание window.hcaptcha / textarea…");
            page.waitForFunction(
                    "() => typeof window.hcaptcha !== 'undefined' || "
                            + "document.querySelector('textarea[name=\"h-captcha-response\"]') !== null",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(20_000));
            log.info("validate-captcha DBG [api] hcaptcha API готов");
        } catch (Exception e) {
            log.warn("validate-captcha DBG [api] api.js не загрузился за 20 с: {}", e.getMessage());
        }
    }

    private Optional<String> extractHCaptchaSiteKey(Page page) {
        String[] selectors = {".h-captcha[data-sitekey]", "div.h-captcha[data-sitekey]", "[data-sitekey]"};
        for (String selector : selectors) {
            Locator loc = page.locator(selector);
            if (loc.count() == 0) {
                continue;
            }
            try {
                String key = loc.first().getAttribute("data-sitekey");
                if (key != null && !key.isBlank()) {
                    return Optional.of(key);
                }
            } catch (Exception ignored) {
                // next
            }
        }
        try {
            Locator iframe = page.locator("iframe[src*='hcaptcha.com']");
            if (iframe.count() > 0) {
                String src = iframe.first().getAttribute("src");
                Optional<String> fromSrc = siteKeyFromUrl(src);
                if (fromSrc.isPresent()) {
                    return fromSrc;
                }
            }
        } catch (Exception e) {
            log.debug("hCaptcha: sitekey из iframe: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private boolean clickHCaptchaCheckbox(Page page) {
        String iframeSel = "iframe[src*='hcaptcha.com']";
        try {
            Locator outerIframe = page.locator(iframeSel);
            int outerCount = outerIframe.count();
            log.info("validate-captcha DBG [click] outer iframes hcaptcha.com={}", outerCount);
            if (outerCount > 0) {
                outerIframe.first().scrollIntoViewIfNeeded();
                page.waitForTimeout(400);

                var outerFrame = page.frameLocator(iframeSel).first();
                String[] innerSelectors = {"#checkbox", "[role='checkbox']", "#anchor-wr", ".check"};
                for (String innerSel : innerSelectors) {
                    try {
                        outerFrame.frameLocator("iframe").locator(innerSel).first()
                                .click(new Locator.ClickOptions().setTimeout(8000));
                        log.info("validate-captcha DBG [click] OK nested-iframe selector={}", innerSel);
                        return true;
                    } catch (Exception e) {
                        log.info("validate-captcha DBG [click] FAIL nested-iframe {}: {}", innerSel, e.getMessage());
                    }
                }
                for (String innerSel : innerSelectors) {
                    try {
                        outerFrame.locator(innerSel).first()
                                .click(new Locator.ClickOptions().setTimeout(8000));
                        log.info("validate-captcha DBG [click] OK outer-frame selector={}", innerSel);
                        return true;
                    } catch (Exception e) {
                        log.info("validate-captcha DBG [click] FAIL outer-frame {}: {}", innerSel, e.getMessage());
                    }
                }

                BoundingBox box = outerIframe.first().boundingBox();
                if (box != null) {
                    double x = box.x + Math.min(28, box.width * 0.12);
                    double y = box.y + box.height / 2.0;
                    page.mouse().move(x, y, new Mouse.MoveOptions().setSteps(14));
                    page.waitForTimeout(300);
                    page.mouse().down();
                    page.waitForTimeout(100);
                    page.mouse().up();
                    log.info("validate-captcha DBG [click] mouse on iframe bbox → ({}, {})", (int) x, (int) y);
                    return true;
                }
                log.warn("validate-captcha DBG [click] iframe bbox=null");
            }
            Locator widget = page.locator(".h-captcha");
            log.info("validate-captcha DBG [click] .h-captcha count={}", widget.count());
            if (widget.count() > 0) {
                widget.first().scrollIntoViewIfNeeded();
                BoundingBox box = widget.first().boundingBox();
                if (box != null) {
                    double x = box.x + Math.min(28, box.width * 0.12);
                    double y = box.y + box.height / 2.0;
                    page.mouse().move(x, y, new Mouse.MoveOptions().setSteps(14));
                    page.waitForTimeout(300);
                    page.mouse().click(x, y);
                    log.info("validate-captcha DBG [click] mouse on .h-captcha → ({}, {})", (int) x, (int) y);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("validate-captcha DBG [click] exception: {}", e.getMessage());
        }
        return false;
    }

    private boolean solveHCaptchaViaTwoCaptcha(Page page) {
        Optional<String> siteKey = extractHCaptchaSiteKey(page);
        if (siteKey.isEmpty()) {
            log.warn("validate-captcha DBG [2cap] sitekey не извлечён");
            return false;
        }
        waitForHcaptchaApi(page);
        log.info("validate-captcha DBG [2cap] sitekey={} url={}", siteKey.get(), page.url());
        String userAgent = page.evaluate("() => navigator.userAgent").toString();
        log.info("validate-captcha DBG [2cap] запрос токена у 2Captcha…");
        Optional<String> token = twoCaptchaClient.solveHCaptcha(page.url(), siteKey.get(), userAgent);
        if (token.isEmpty()) {
            log.warn("validate-captcha DBG [2cap] токен не получен (timeout или ошибка API)");
            return false;
        }
        log.info("validate-captcha DBG [2cap] токен получен, len={}", token.get().length());
        applyHCaptchaToken(page, token.get());
        logValidateCaptchaState(page, "2cap-after-apply-token");
        page.waitForTimeout(500);
        submitFlRuValidateCaptchaForm(page);
        waitForValidateCaptchaRedirect(page);
        boolean ok = !page.url().contains("validate-captcha");
        log.info("validate-captcha DBG [2cap] done responseField={} url={} success={}",
                isHCaptchaResponsePresent(page), page.url(), ok);
        return ok;
    }

    private void waitForValidateCaptchaRedirect(Page page) {
        try {
            page.waitForURL(
                    url -> url != null && !url.contains("validate-captcha"),
                    new Page.WaitForURLOptions().setTimeout(25_000));
            log.info("validate-captcha: редирект после POST, url={}", page.url());
        } catch (Exception e) {
            log.warn("validate-captcha: редирект не произошёл за 25 с, url={}", page.url());
        }
    }

    private void applyHCaptchaToken(Page page, String token) {
        page.evaluate(
                """
                        (token) => {
                          const form = document.getElementById('captcha-form') || document.querySelector('form');
                          const ensureTextarea = (name) => {
                            let el = form && form.querySelector(`textarea[name="${name}"], input[name="${name}"]`);
                            if (!el && form) {
                              el = document.createElement('textarea');
                              el.name = name;
                              el.style.display = 'none';
                              form.appendChild(el);
                            }
                            if (el) {
                              el.value = token;
                              el.innerHTML = token;
                              el.dispatchEvent(new Event('input', { bubbles: true }));
                              el.dispatchEvent(new Event('change', { bubbles: true }));
                            }
                          };
                          ensureTextarea('h-captcha-response');
                          ensureTextarea('g-recaptcha-response');

                          if (window.hcaptcha && typeof window.hcaptcha.setResponse === 'function') {
                            const ids = new Set();
                            document.querySelectorAll('[data-hcaptcha-widget-id]').forEach((el) => {
                              ids.add(el.getAttribute('data-hcaptcha-widget-id'));
                            });
                            document.querySelectorAll('iframe[data-hcaptcha-widget-id]').forEach((el) => {
                              ids.add(el.getAttribute('data-hcaptcha-widget-id'));
                            });
                            if (ids.size === 0) {
                              ids.add('0');
                              ids.add(0);
                            }
                            ids.forEach((id) => {
                              try {
                                window.hcaptcha.setResponse(id, token);
                              } catch (e) { /* ignore */ }
                            });
                          }
                        }
                        """,
                token);
        log.info("hCaptcha/2Captcha: response применён на странице");
    }

    /** FL.ru: {@code data-callback="sendCaptcha"} → {@code #captcha-form}.submit() */
    private void submitFlRuValidateCaptchaForm(Page page) {
        try {
            Object mode = page.evaluate(
                    """
                            () => {
                              if (typeof window.sendCaptcha === 'function') {
                                window.sendCaptcha();
                                return 'sendCaptcha';
                              }
                              const form = document.getElementById('captcha-form') || document.querySelector('form');
                              if (!form) return 'no-form';
                              if (typeof form.requestSubmit === 'function') {
                                form.requestSubmit();
                                return 'requestSubmit';
                              }
                              form.submit();
                              return 'submit';
                            }
                            """);
            log.info("validate-captcha DBG [submit] отправка формы mode={} url={}", mode, page.url());
        } catch (Exception e) {
            log.warn("validate-captcha: submit формы: {}", e.getMessage());
        }
    }

    private void waitForFlRuValidateCaptchaWidget(Page page) {
        long deadline = System.currentTimeMillis() + 15_000;
        String[] cssSelectors = {
                "iframe[src*='hcaptcha.com']",
                ".h-captcha",
                "iframe[src*='smartcaptcha.yandexcloud.net']"
        };
        while (System.currentTimeMillis() < deadline) {
            for (String selector : cssSelectors) {
                try {
                    if (page.locator(selector).count() > 0 && page.locator(selector).first().isVisible()) {
                        log.info("validate-captcha DBG [wait-widget] найден: {}", selector);
                        return;
                    }
                } catch (Exception ignored) {
                    // next
                }
            }
            try {
                if (page.getByText("подозрительная активность").count() > 0) {
                    log.debug("validate-captcha: текст IP-блока найден");
                    return;
                }
            } catch (Exception ignored) {
                // next
            }
            page.waitForTimeout(300);
        }
        log.warn("validate-captcha: виджет капчи не появился за 15 с");
    }

    private boolean hasYandexSmartCaptchaWidget(Page page) {
        if (findYandexCaptchaFrame(page) != null) {
            return true;
        }
        return page.locator("iframe[src*='smartcaptcha.yandexcloud.net'], .smart-captcha").count() > 0;
    }

    private boolean clickFlRuValidateHumanWidget(Page page) {
        String[] selectors = {
                "iframe[src*='smartcaptcha.yandexcloud.net']",
                "#label",
                "label-td",
                "text=Я человек"
        };
        for (String selector : selectors) {
            try {
                Locator loc = page.locator(selector);
                if (loc.count() == 0) {
                    continue;
                }
                Locator target = loc.first();
                if (selector.startsWith("iframe")) {
                    BoundingBox box = target.boundingBox();
                    if (box == null) {
                        continue;
                    }
                    double x = box.x + Math.min(28, box.width / 3);
                    double y = box.y + box.height / 2;
                    page.mouse().click(x, y);
                    log.debug("validate-captcha: клик по iframe SmartCaptcha ({}, {})", (int) x, (int) y);
                } else {
                    if (!target.isVisible()) {
                        continue;
                    }
                    target.click(new Locator.ClickOptions().setTimeout(5000));
                    log.debug("validate-captcha: клик по {}", selector);
                }
                page.waitForTimeout(800);
                Frame frame = findYandexCaptchaFrame(page);
                if (frame != null) {
                    try {
                        frame.evaluate("window.postMessage(JSON.stringify({methodCall: 'start'}), '*')");
                    } catch (Exception ignored) {
                        // optional
                    }
                }
                return true;
            } catch (Exception e) {
                log.debug("validate-captcha: клик {} не удался: {}", selector, e.getMessage());
            }
        }
        return false;
    }

    private boolean isFlRuValidateCaptchaLikelyPassed(Page page) {
        if (isSmartTokenPresentOnPage(page)) {
            return true;
        }
        try {
            Locator label = page.locator("#label:has-text('Я человек')");
            if (label.count() > 0 && label.first().isVisible()) {
                return false;
            }
            Frame frame = findYandexCaptchaFrame(page);
            if (frame != null && isYandexSmartCaptchaPassed(frame)) {
                return true;
            }
        } catch (Exception e) {
            log.debug("validate-captcha: проверка результата: {}", e.getMessage());
        }
        return !isSmartCaptchaOverlayVisible(page);
    }

    private boolean solveYandexSmartCaptchaInternal(Page page, boolean captchaRequired) {
        try {
            Frame frame = findYandexCaptchaFrame(page);
            if (frame == null) {
                if (captchaRequired) {
                    log.warn("SmartCaptcha: виджет обязателен, но iframe не найден");
                    return false;
                }
                log.debug("SmartCaptcha: iframe не найден — капча отсутствует");
                return true;
            }
            log.debug("SmartCaptcha: iframe найден, начинаем обход...");

            if (yandexTwoCaptchaFirst && twoCaptchaClient.isConfigured()) {
                if (solveYandexSmartCaptchaViaTwoCaptcha(page, frame)) {
                    log.info("SmartCaptcha: пройдена через 2Captcha (primary)");
                    return true;
                }
                log.warn("SmartCaptcha: 2Captcha (primary) не помог, пробуем локальный клик");
            }

            if (clickYandexSmartCaptcha(page, frame)) {
                log.info("SmartCaptcha: успешно пройдена локальным кликом");
                return true;
            }

            if (yandexTwoCaptchaFallback && twoCaptchaClient.isConfigured()) {
                if (solveYandexSmartCaptchaViaTwoCaptcha(page, frame)) {
                    log.info("SmartCaptcha: пройдена через 2Captcha (fallback)");
                    return true;
                }
            }

            log.warn("SmartCaptcha: не удалось пройти");
            return false;

        } catch (Exception e) {
            log.error("SmartCaptcha: ошибка при обходе", e);
            return false;
        }
    }

    private boolean solveYandexSmartCaptchaViaTwoCaptcha(Page page, Frame frame) {
        Optional<String> siteKey = extractYandexSiteKey(page);
        if (siteKey.isEmpty()) {
            log.warn("SmartCaptcha/2Captcha: sitekey не извлечён");
            return false;
        }

        String userAgent = page.evaluate("() => navigator.userAgent").toString();
        String cookies = page.context().cookies(page.url()).stream()
                .map(c -> c.name + "=" + c.value)
                .collect(Collectors.joining("; "));

        Optional<String> token = twoCaptchaClient.solveYandexSmartCaptcha(
                page.url(), siteKey.get(), userAgent, cookies);
        if (token.isEmpty()) {
            return false;
        }

        applyYandexSmartCaptchaToken(page, frame, token.get());
        dismissSmartCaptchaOverlay(page);
        page.waitForTimeout(1500);
        if (isYandexSmartCaptchaPassed(frame)) {
            return true;
        }
        if (isSmartTokenPresentOnPage(page)) {
            if (isSmartCaptchaOverlayVisible(page)) {
                log.warn("SmartCaptcha/2Captcha: smart-token есть, но advanced-оверлей ещё виден — скрываем и продолжаем");
                dismissSmartCaptchaOverlay(page);
                page.waitForTimeout(500);
            }
            log.info("SmartCaptcha/2Captcha: чекбокс в iframe не отметился, но smart-token на странице есть — продолжаем логин");
            return true;
        }
        log.warn("SmartCaptcha/2Captcha: токен получен, но не подтверждён ни UI, ни полем smart-token");
        return false;
    }

    private Optional<String> extractYandexSiteKey(Page page) {
        String[] iframeSelectors = {
                "iframe[data-testid='checkbox-iframe']",
                "iframe[data-testid='advanced-iframe']",
                "iframe[src*='smartcaptcha.yandexcloud.net']",
                "iframe[src*='captcha.yandex']"
        };
        for (String selector : iframeSelectors) {
            Locator iframe = page.locator(selector);
            if (iframe.count() == 0) {
                continue;
            }
            try {
                String src = iframe.first().getAttribute("src");
                Optional<String> fromSrc = siteKeyFromUrl(src);
                if (fromSrc.isPresent()) {
                    return fromSrc;
                }
            } catch (Exception e) {
                log.debug("SmartCaptcha: sitekey из iframe {} не получен: {}", selector, e.getMessage());
            }
        }

        try {
            Locator container = page.locator("[data-sitekey]");
            if (container.count() > 0) {
                String key = container.first().getAttribute("data-sitekey");
                if (key != null && !key.isBlank()) {
                    return Optional.of(key);
                }
            }
        } catch (Exception e) {
            log.debug("SmartCaptcha: data-sitekey не найден: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> siteKeyFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = SITEKEY_QUERY.matcher(url);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8));
    }

    private void applyYandexSmartCaptchaToken(Page page, Frame captchaFrame, String token) {
        page.evaluate(
                """
                        (token) => {
                          const setHidden = (root) => {
                            const selectors = [
                              'input[name="smart-token"]',
                              'input[name="smart_token"]',
                              'input[data-testid="smart-token"]'
                            ];
                            for (const sel of selectors) {
                              const el = root.querySelector(sel);
                              if (el) {
                                el.value = token;
                                el.dispatchEvent(new Event('input', { bubbles: true }));
                                el.dispatchEvent(new Event('change', { bubbles: true }));
                                return true;
                              }
                            }
                            return false;
                          };
                          if (!setHidden(document)) {
                            const form = document.querySelector('form');
                            if (form) {
                              const input = document.createElement('input');
                              input.type = 'hidden';
                              input.name = 'smart-token';
                              input.value = token;
                              form.appendChild(input);
                            }
                          }
                          const containers = document.querySelectorAll('.smart-captcha, [data-sitekey]');
                          containers.forEach((container) => {
                            setHidden(container);
                            const cbName = container.getAttribute('data-callback');
                            if (cbName && typeof window[cbName] === 'function') {
                              window[cbName](token);
                            }
                          });
                          if (window.smartCaptcha && typeof window.smartCaptcha.render === 'function') {
                            try {
                              const resp = window.smartCaptcha.getResponse && window.smartCaptcha.getResponse();
                              if (!resp && window.smartCaptcha.execute) {
                                /* noop — token injected manually */
                              }
                            } catch (e) { /* ignore */ }
                          }
                          window.dispatchEvent(new CustomEvent('smart-captcha-token', { detail: token }));
                          window.postMessage(JSON.stringify({ event: 'smart-captcha', token }), '*');
                          const submit = document.querySelector('#submit-button, button[type="submit"]');
                          if (submit) {
                            submit.removeAttribute('disabled');
                            submit.disabled = false;
                            submit.classList.remove('disabled');
                          }
                        }
                        """,
                token);
        try {
            captchaFrame.evaluate(
                    """
                            (token) => {
                              window.postMessage(JSON.stringify({ methodCall: 'success', token }), '*');
                            }
                            """,
                    token);
        } catch (Exception e) {
            log.debug("SmartCaptcha/2Captcha: postMessage во frame: {}", e.getMessage());
        }
        dismissSmartCaptchaOverlay(page);
        log.debug("SmartCaptcha/2Captcha: token применён на странице");
    }

    private boolean isSmartTokenPresentOnPage(Page page) {
        try {
            Object len = page.evaluate(
                    """
                            () => {
                              const pick = (root) => {
                                const sel = ['input[name="smart-token"]', 'input[name="smart_token"]'];
                                for (const s of sel) {
                                  const el = root.querySelector(s);
                                  if (el && el.value && el.value.length > 20) return el.value.length;
                                }
                                return 0;
                              };
                              let n = pick(document);
                              if (n > 0) return n;
                              for (const c of document.querySelectorAll('.smart-captcha, [data-sitekey]')) {
                                n = pick(c);
                                if (n > 0) return n;
                              }
                              if (window.smartCaptcha && typeof window.smartCaptcha.getResponse === 'function') {
                                const r = window.smartCaptcha.getResponse();
                                if (r && r.length > 20) return r.length;
                              }
                              return 0;
                            }
                            """);
            if (len instanceof Number number) {
                return number.longValue() > 20;
            }
            return false;
        } catch (Exception e) {
            log.debug("SmartCaptcha/2Captcha: проверка smart-token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isYandexSmartCaptchaPassed(Frame frame) {
        try {
            Locator input = frame.locator("input#js-button");
            if (input.count() > 0 && "true".equals(input.getAttribute("aria-checked"))) {
                return true;
            }
            Locator checkbox = frame.locator(".CheckboxCaptcha-Checkbox");
            if (checkbox.count() > 0 && "true".equals(checkbox.getAttribute("data-checked"))) {
                return true;
            }
        } catch (Exception e) {
            log.debug("SmartCaptcha: проверка после 2Captcha: {}", e.getMessage());
        }
        return findYandexCaptchaFrame(frame.page()) == null;
    }

    public boolean isCloudflareChallengePresent(Page page) {
        try {
            for (Frame f : page.frames()) {
                String url = f.url();
                if (url != null && (url.contains("challenges.cloudflare.com") || url.contains("turnstile"))) {
                    log.debug("Cloudflare Turnstile: frame detected {}", url);
                    return true;
                }
            }
            String[] textSelectors = {
                    "text=Я человек",
                    "text=Verify you are human",
                    "text=Just a moment",
                    "text=Checking your browser"
            };
            for (String sel : textSelectors) {
                Locator loc = page.locator(sel);
                if (loc.count() > 0 && loc.first().isVisible()) {
                    log.debug("Cloudflare Turnstile: текст challenge найден ({})", sel);
                    return true;
                }
            }
            if (page.locator("iframe[src*='challenges.cloudflare.com']").count() > 0) {
                return true;
            }
            if (page.locator("iframe[src*='turnstile']").count() > 0) {
                return true;
            }
            if (page.locator(".cf-turnstile, [data-turnstile-widget]").count() > 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("Cloudflare Turnstile: ошибка проверки presence: {}", e.getMessage());
            return false;
        }
    }

    public boolean waitForCfClearance(Page page, String cookieDomain, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (hasCfClearanceCookie(page, cookieDomain)) {
                log.info("Cloudflare Turnstile: cf_clearance получен для {}", cookieDomain);
                return true;
            }
            page.waitForTimeout(500);
        }
        log.warn("Cloudflare Turnstile: timeout ожидания cf_clearance ({} ms)", timeoutMs);
        return false;
    }

    private boolean hasCfClearanceCookie(Page page, String cookieDomain) {
        try {
            List<Cookie> cookies = page.context().cookies(cookieDomain);
            return cookies.stream().anyMatch(c -> "cf_clearance".equals(c.name));
        } catch (Exception e) {
            return false;
        }
    }

    private Locator findTurnstileWidget(Page page) {
        String[] selectors = {
                "iframe[src*='challenges.cloudflare.com']",
                "iframe[src*='turnstile']",
                ".cf-turnstile",
                "[data-turnstile-widget]"
        };
        for (String sel : selectors) {
            Locator loc = page.locator(sel);
            if (loc.count() > 0) {
                return loc.first();
            }
        }
        return null;
    }

    private void humanLikeClickOnWidget(Page page, Locator widget) {
        BoundingBox box = widget.boundingBox();
        if (box == null) {
            log.warn("Cloudflare Turnstile: boundingBox виджета не получен");
            return;
        }
        double cx = box.x + box.width / 2 + (Math.random() * 10 - 5);
        double cy = box.y + box.height / 2 + (Math.random() * 10 - 5);
        int steps = 8 + (int) (Math.random() * 8);
        page.mouse().move(cx, cy, new Mouse.MoveOptions().setSteps(steps));
        page.waitForTimeout(200 + (int) (Math.random() * 300));
        page.mouse().click(cx, cy);
        log.info("Cloudflare Turnstile: клик по виджету ({}, {})", (int) cx, (int) cy);
    }

    private void tryClickInsideTurnstileFrame(Page page) {
        String[] iframeSelectors = {
                "iframe[src*='challenges.cloudflare.com']",
                "iframe[src*='turnstile']"
        };
        for (String sel : iframeSelectors) {
            if (page.locator(sel).count() == 0) {
                continue;
            }
            try {
                Locator inner = page.frameLocator(sel)
                        .locator("input[type=checkbox], .ctp-checkbox-label, label, [role=checkbox]")
                        .first();
                inner.click(new Locator.ClickOptions().setTimeout(5000));
                log.info("Cloudflare Turnstile: клик внутри iframe ({})", sel);
                return;
            } catch (Exception e) {
                log.debug("Cloudflare Turnstile: клик внутри iframe не удался ({}): {}", sel, e.getMessage());
            }
        }
    }

    private boolean isLoginFormVisible(Page page) {
        try {
            Locator login = page.locator("input[name='login']");
            return login.count() > 0 && login.first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean solveCloudflareTurnstile(Page page) {
        if (!cloudflareEnabled) {
            log.debug("Cloudflare Turnstile: отключено в конфиге");
            return true;
        }
        try {
            if (!isCloudflareChallengePresent(page)) {
                log.debug("Cloudflare Turnstile: challenge не обнаружен");
                return true;
            }
            log.info("Cloudflare Turnstile: начинаем прохождение...");

            Locator widget = findTurnstileWidget(page);
            if (widget != null) {
                humanLikeClickOnWidget(page, widget);
            } else {
                log.warn("Cloudflare Turnstile: виджет не найден, пробуем клик внутри iframe");
            }
            tryClickInsideTurnstileFrame(page);

            page.waitForTimeout(1500);

            if (waitForCfClearance(page, WEBLANCER_COOKIE_URL, cloudflareTimeoutMs)) {
                return true;
            }
            if (!isCloudflareChallengePresent(page) && isLoginFormVisible(page)) {
                log.info("Cloudflare Turnstile: challenge исчез, форма логина доступна");
                return true;
            }
            if (!isCloudflareChallengePresent(page)) {
                log.info("Cloudflare Turnstile: challenge исчез после клика");
                return true;
            }
            log.warn("Cloudflare Turnstile: не удалось пройти");
            return false;
        } catch (Exception e) {
            log.error("Cloudflare Turnstile: ошибка при обходе", e);
            return false;
        }
    }
}
