package by.gdev.common.service.playwright;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CaptchaService {

    private Frame findYandexCaptchaFrame(Page page) {
        for (Frame f : page.frames()) {
            String url = f.url();
            if (url != null && url.contains("checkbox")) { // ВАЖНО!
                return f;
            }
        }
        return null;
    }

    private boolean clickYandexSmartCaptcha(Page page, Frame frame) {
        try {
            // Клик внутри iframe
            Locator iframeLocator = page.locator("iframe[data-testid='checkbox-iframe']");
            BoundingBox box = iframeLocator.boundingBox();
            if (box == null) {
                log.warn("SmartCaptcha: не удалось получить boundingBox iframe");
                return false;
            }
            page.mouse().click(box.x + 5, box.y + 5);
            Thread.sleep(200);

            // Ждём появления input
            frame.waitForSelector("input#js-button",
                    new Frame.WaitForSelectorOptions().setTimeout(10000));
            // Отправляем start
            frame.evaluate("window.postMessage(JSON.stringify({methodCall: 'start'}), '*')");
            Thread.sleep(1500);
            // Проверяем aria-checked
            Locator input = frame.locator("input#js-button");
            String aria = input.getAttribute("aria-checked");
            if ("true".equals(aria)) {
                log.debug("SmartCaptcha: aria-checked=true — капча пройдена");
                return true;
            }
            // Проверяем data-checked
            String checked = frame.locator(".CheckboxCaptcha-Checkbox")
                    .getAttribute("data-checked");
            if ("true".equals(checked)) {
                log.debug("SmartCaptcha: data-checked=true — капча пройдена");
                return true;
            }
            log.warn("SmartCaptcha: input/data-checked не изменились");
            return false;
        } catch (Exception e) {
            log.error("SmartCaptcha: ошибка при клике", e);
            return false;
        }
    }

    public boolean solveYandexSmartCaptcha(Page page) {
        try {
            // Ищем iframe с капчей
            Frame frame = findYandexCaptchaFrame(page);
            if (frame == null) {
                log.debug("SmartCaptcha: iframe не найден — капча отсутствует");
                return true; // капчи нет - всё ок
            }
            log.debug("SmartCaptcha: iframe найден, начинаем обход...");

            // Пытаемся пройти капчу
            boolean result = clickYandexSmartCaptcha(page, frame);
            if (result) {
                log.info("SmartCaptcha: успешно пройдена");
            } else {
                log.warn("SmartCaptcha: не удалось пройти");
            }
            return result;

        } catch (Exception e) {
            log.error("SmartCaptcha: ошибка при обходе", e);
            return false;
        }
    }



}
