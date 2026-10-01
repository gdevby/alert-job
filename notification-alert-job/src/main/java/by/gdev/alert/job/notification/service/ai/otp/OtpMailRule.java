package by.gdev.alert.job.notification.service.ai.otp;

import by.gdev.common.model.SiteName;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Правила разбора писем с одноразовым кодом. Биржа шлёт письмо с собственного адреса и
 * формулирует его по-своему, поэтому и опознание отправителя, и выделение кода задаются
 * отдельно для каждого сайта.
 * <p>
 * Правило имеет приоритет над общим разбором в {@link EmailOtpScheduler}: тот ищет в письме
 * имя элемента {@link SiteName} и цифровой код после слов «Код для подтверждения», что
 * подходит не всем биржам.
 */
public enum OtpMailRule {

    /**
     * FL.ru пишет с адреса no_reply@free-lance.ru — по имени элемента {@code FLRU} такое
     * письмо не опознать. Код из шести знаков, заглавные латинские буквы вперемешку с
     * цифрами: «...вставьте в форму подтверждения на сайте FL.ru: 750R3N».
     */
    FLRU(SiteName.FLRU,
            List.of("free-lance.ru", "fl.ru"),
            List.of(
                    Pattern.compile("(?iu:скопируйте следующий код).{0,300}?" + Tokens.FL_CODE, Pattern.DOTALL),
                    Pattern.compile(Tokens.FL_CODE)
            ));

    /**
     * Вынесено в отдельный класс: обратиться к статическому полю самого перечисления из
     * списка его же элементов язык не позволяет.
     */
    private static final class Tokens {
        /** Ровно шесть знаков из заглавных букв и цифр, причём и того и другого хотя бы по одному. */
        private static final String FL_CODE =
                "\\b(?=[A-Z0-9]{6}\\b)(?=[A-Z0-9]*[A-Z])(?=[A-Z0-9]*[0-9])([A-Z0-9]{6})\\b";
    }

    private final SiteName site;
    private final List<String> senderMarkers;
    private final List<Pattern> codePatterns;

    OtpMailRule(SiteName site, List<String> senderMarkers, List<Pattern> codePatterns) {
        this.site = site;
        this.senderMarkers = senderMarkers;
        this.codePatterns = codePatterns;
    }

    public SiteName site() {
        return site;
    }

    /**
     * Ищет правило по адресу отправителя. Тело письма намеренно не учитывается: упоминание
     * чужой биржи внутри текста не должно уводить код не в тот ящик.
     */
    public static Optional<OtpMailRule> forSender(String from) {
        String sender = from == null ? "" : from.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(rule -> rule.senderMarkers.stream().anyMatch(sender::contains))
                .findFirst();
    }

    /**
     * Достаёт код из очищенного текста письма. Шаблоны перебираются по порядку: сначала
     * привязанный к формулировке, затем свободный — на случай, если биржа перепишет текст.
     *
     * @param text результат {@link #normalize(String)}
     */
    public Optional<String> extractCode(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        for (Pattern pattern : codePatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    /** Убирает разметку и сводит пробелы: письмо приходит и текстом, и HTML одновременно. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
