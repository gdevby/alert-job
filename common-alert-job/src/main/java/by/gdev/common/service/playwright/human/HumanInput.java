package by.gdev.common.service.playwright.human;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ViewportSize;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Имитация живого ввода: движение мыши по дуге с разгоном и торможением, клик с удержанием
 * кнопки, печать с переменным ритмом, прокрутка рывками.
 * <p>
 * Ключевое отличие от штатных средств Playwright — паузы между событиями. Вызов
 * {@code move(x, y, setSteps(n))} шлёт все промежуточные точки одним пакетом, поэтому весь
 * жест укладывается в нулевое время; телеметрия антибот-систем видит такое сразу.
 */
public final class HumanInput {

    private HumanInput() {
    }

    /**
     * Клик по области с имитацией живой руки: подвод по дуге, промах мимо цели с короткой
     * доводкой, задержка на наведении и удержание кнопки.
     *
     * @return фактическая точка нажатия
     */
    public static double[] click(Page page, BoundingBox box) {
        // Люди целятся в центр, но попадают вокруг него — отсюда нормальное распределение, а не равномерное.
        double x = clamp(box.x + box.width / 2 + gaussian(box.width / 8), box.x + 2, box.x + box.width - 2);
        double y = clamp(box.y + box.height / 2 + gaussian(box.height / 8), box.y + 2, box.y + box.height - 2);
        moveTo(page, x, y);
        hoverDwell(page, x, y);
        page.mouse().down();
        page.waitForTimeout(50 + rnd().nextInt(90));
        page.mouse().up();
        return new double[]{x, y};
    }

    /**
     * Клик по элементу. Ручной жест мышью не выполняет проверок доступности элемента, которые
     * делает {@code Locator.click()}, поэтому при любой осечке откатываемся на штатный клик.
     */
    public static void click(Page page, Locator locator) {
        try {
            scrollIntoView(page, locator);
            BoundingBox box = locator.boundingBox();
            if (box != null) {
                click(page, box);
                return;
            }
        } catch (RuntimeException ignored) {
            // ниже штатный клик со своими ожиданиями
        }
        locator.click();
    }

    /** Подвод курсора к точке: заход из случайного направления, промах и доводка. */
    public static void moveTo(Page page, double x, double y) {
        double[] start = approachPoint(page, x, y);
        page.mouse().move(start[0], start[1]);

        double overshootX = x + gaussian(5);
        double overshootY = y + gaussian(5);
        traceCurve(page, start[0], start[1], overshootX, overshootY);
        page.waitForTimeout(40 + rnd().nextInt(90));
        traceCurve(page, overshootX, overshootY, x, y);
    }

    /**
     * Печать с переменным ритмом: заглавные и символы требуют Shift и потому медленнее,
     * после пробела рука притормаживает, изредка человек задумывается на середине строки.
     */
    public static void type(Page page, Locator input, String text) {
        click(page, input);
        page.waitForTimeout(200 + rnd().nextInt(400));
        clearIfFilled(page, input);

        char previous = 0;
        for (char c : text.toCharArray()) {
            page.keyboard().type(String.valueOf(c));

            int delay = 70 + rnd().nextInt(90);
            if (Character.isUpperCase(c) || (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) {
                delay += 40 + rnd().nextInt(80);
            }
            if (previous == ' ') {
                delay += 30 + rnd().nextInt(60);
            }
            if (rnd().nextInt(100) < 8) {
                delay += 350 + rnd().nextInt(550);
            }
            page.waitForTimeout(delay);
            previous = c;
        }
    }

    /**
     * Стирает прежнее содержимое выделением и Backspace — в отличие от {@code fill()},
     * который подменяет значение без единого события клавиатуры.
     */
    private static void clearIfFilled(Page page, Locator input) {
        try {
            if (input.inputValue().isEmpty()) {
                return;
            }
        } catch (RuntimeException e) {
            return;
        }
        page.keyboard().press("Control+A");
        page.waitForTimeout(60 + rnd().nextInt(120));
        page.keyboard().press("Backspace");
        page.waitForTimeout(80 + rnd().nextInt(150));
    }

    /** Блуждание курсора по странице между осмысленными действиями. */
    public static void wander(Page page) {
        ViewportSize viewport = page.viewportSize();
        double maxX = viewport != null ? viewport.width - 4 : 1360;
        double maxY = viewport != null ? viewport.height - 4 : 760;

        double x = 4 + rnd().nextDouble() * maxX;
        double y = 4 + rnd().nextDouble() * maxY;
        page.mouse().move(x, y);

        int waypoints = 2 + rnd().nextInt(3);
        for (int i = 0; i < waypoints; i++) {
            double nextX = clamp(x + gaussian(maxX / 4), 4, maxX);
            double nextY = clamp(y + gaussian(maxY / 4), 4, maxY);
            traceCurve(page, x, y, nextX, nextY);
            page.waitForTimeout(120 + rnd().nextInt(380));
            x = nextX;
            y = nextY;
        }
    }

    /** Пауза «на подумать»: обычно короткая, изредка заметно длиннее. */
    public static void pause(Page page) {
        int delay = 400 + rnd().nextInt(900);
        if (rnd().nextInt(100) < 15) {
            delay += 1000 + rnd().nextInt(2000);
        }
        page.waitForTimeout(delay);
    }

    /**
     * Прокрутка рывками: один взмах колеса — это серия близких событий, между взмахами человек
     * читает страницу, иногда проматывает чуть назад.
     */
    public static void scroll(Page page) {
        int bursts = 2 + rnd().nextInt(3);
        for (int i = 0; i < bursts; i++) {
            boolean back = rnd().nextInt(100) < 20;
            int ticks = 3 + rnd().nextInt(6);
            for (int t = 0; t < ticks; t++) {
                double delta = 100 + rnd().nextInt(60);
                page.mouse().wheel(0, back ? -delta : delta);
                page.waitForTimeout(20 + rnd().nextInt(40));
            }
            page.waitForTimeout(300 + rnd().nextInt(600));
        }
    }

    /** Докручивает колесом порциями, пока элемент не окажется в комфортной для чтения зоне экрана. */
    public static void scrollIntoView(Page page, Locator target) {
        ViewportSize viewport = page.viewportSize();
        if (viewport == null) {
            target.scrollIntoViewIfNeeded();
            return;
        }
        for (int i = 0; i < 12; i++) {
            BoundingBox box = target.boundingBox();
            if (box == null) {
                break;
            }
            double center = box.y + box.height / 2;
            if (center > viewport.height * 0.2 && center < viewport.height * 0.8) {
                return;
            }
            double delta = 90 + rnd().nextInt(110);
            page.mouse().wheel(0, center < viewport.height * 0.2 ? -delta : delta);
            page.waitForTimeout(60 + rnd().nextInt(120));
        }
        target.scrollIntoViewIfNeeded();
    }

    /** Проводит курсор по квадратичной кривой Безье с паузой на каждом шаге. */
    private static void traceCurve(Page page, double fromX, double fromY, double toX, double toY) {
        double distance = Math.hypot(toX - fromX, toY - fromY);
        int steps = Math.max(6, Math.min(40, (int) (distance / 12) + 6));
        double bend = distance / 6;
        double controlX = (fromX + toX) / 2 + gaussian(bend);
        double controlY = (fromY + toY) / 2 + gaussian(bend);

        for (int i = 1; i <= steps; i++) {
            double t = ease((double) i / steps);
            double inv = 1 - t;
            double x = inv * inv * fromX + 2 * inv * t * controlX + t * t * toX;
            double y = inv * inv * fromY + 2 * inv * t * controlY + t * t * toY;
            page.mouse().move(x, y);
            page.waitForTimeout(8 + rnd().nextInt(14));
        }
    }

    /** Микродрожание кисти, пока человек задерживает курсор перед нажатием. */
    private static void hoverDwell(Page page, double x, double y) {
        int moves = 1 + rnd().nextInt(3);
        for (int i = 0; i < moves; i++) {
            page.mouse().move(x + gaussian(1.2), y + gaussian(1.2));
            page.waitForTimeout(30 + rnd().nextInt(70));
        }
    }

    /** Точка, из которой курсор «приходит» к цели: случайное направление на расстоянии 120–400 px. */
    private static double[] approachPoint(Page page, double targetX, double targetY) {
        double angle = rnd().nextDouble() * 2 * Math.PI;
        double distance = 120 + rnd().nextDouble() * 280;
        ViewportSize viewport = page.viewportSize();
        double maxX = viewport != null ? viewport.width - 2 : 1364;
        double maxY = viewport != null ? viewport.height - 2 : 766;
        return new double[]{
                clamp(targetX + Math.cos(angle) * distance, 2, maxX),
                clamp(targetY + Math.sin(angle) * distance, 2, maxY)
        };
    }

    /** Разгон в начале движения и торможение у цели вместо постоянной скорости. */
    private static double ease(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    private static double gaussian(double spread) {
        return rnd().nextGaussian() * spread;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ThreadLocalRandom rnd() {
        return ThreadLocalRandom.current();
    }
}
