package by.gdev.alert.job.llm.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Глобальный обработчик исключений для REST‑контроллеров.
 * <p>
 * Перехватывает распространённые ошибки валидации и состояния,
 * возвращая клиенту корректный HTTP‑ответ вместо стандартного stacktrace.
 * <p>
 * Основные особенности:
 * <ul>
 *     <li>Обрабатывает {@link IllegalStateException} — ошибки логики или некорректного состояния.</li>
 *     <li>Обрабатывает {@link IllegalArgumentException} — ошибки неверных аргументов.</li>
 *     <li>Возвращает HTTP 400 (Bad Request) с текстом ошибки.</li>
 *     <li>Применяется ко всем REST‑контроллерам благодаря {@code @RestControllerAdvice}.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка ошибок некорректного состояния.
     *
     * @param ex выброшенное исключение
     * @return HTTP 400 с текстом ошибки
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    /**
     * Обработка ошибок неверных аргументов.
     *
     * @param ex выброшенное исключение
     * @return HTTP 400 с текстом ошибки
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
