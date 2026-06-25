package by.gdev.alert.job.parser.service.playwright;


import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CaptchaService {

    /**
     * Пытается автоматически пройти Yandex SmartCaptcha на странице.
     * <p>
     * Алгоритм:
     * Ждёт появления iframe с чекбоксом капчи.
     * Ждёт появления элемента для нажатия внутри iframe.
     * Делает паузу, имитируя задержку человека.
     * Кликает по iframe, что инициирует прохождение капчи.
     * <p>
     * Метод НЕ проверяет успешность прохождения — он лишь выполняет действия.
     *
     * @param page Playwright-страница, на которой отображается капча.
     * @return true — если удалось выполнить все шаги без исключений,
     *         false — если возникла ошибка (iframe не найден, элементы не появились и т.д.).
     */
    public boolean solveSmartCaptcha(Page page) {
        try {
            //ищем фрейм капчи
            Locator iframe = page.locator("iframe[data-testid='checkbox-iframe']");
            iframe.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            //Ищем чекбокс в фрейме
            page.frameLocator("iframe[data-testid='checkbox-iframe']")
                    .locator(".CheckboxCaptcha-Checkbox")
                    .waitFor(new Locator.WaitForOptions().setTimeout(5000));
            //задержка имитации подтупливания человеком
            page.waitForTimeout(5000);
            iframe.click();
            return true;
        } catch (Exception e) {
            log.error("SmartCaptcha: ошибка", e);
            return false;
        }
    }

    /**
     * Проверяет, находится ли пользователь на странице проверки безопасности
     * (странице с Yandex SmartCaptcha).
     * <p>
     * Страница выводит заголовок "Проверка безопасности" при показе капчи.
     * Метод просто ищет этот заголовок.
     *
     * @param page Playwright-страница.
     * @return true — если на странице найден заголовок "Проверка безопасности",
     *         false — если заголовка нет или произошла ошибка.
     */
    public boolean isCaptchaPage(Page page) {
        try {
            return page.locator("h1:has-text('Проверка безопасности')").count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

}