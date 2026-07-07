package by.gdev.alert.job.llm.constants;

public final class LlmConstants {

    private LlmConstants() {
        // запрет создания экземпляров
    }

    /**
     * Плейсхолдер для автоматически сгенерированного текста в шаблонах писем.
     * Должен быть экранирован при использовании в String.formatted().
     */
    public static final String AUTO_GENERATED_PLACEHOLDER = "%auto_generated_text%";

    /**
     * Экранированная версия плейсхолдера для использования в String.formatted()
     * (чтобы избежать UnknownFormatConversionException).
     */
    public static final String ESCAPED_AUTO_GENERATED_PLACEHOLDER = "%%auto_generated_text%%";

    public static final String SYSTEM_PROMT = "Ты — строгий JSON-генератор. Отвечай только валидным JSON без пояснений.";
}