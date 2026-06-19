package by.gdev.alert.job.llm.service.aiautoreply.promt;

import by.gdev.alert.job.llm.domain.dto.promt.AiPromptDto;
import by.gdev.alert.job.llm.domain.promt.AiPrompt;
import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import by.gdev.alert.job.llm.repository.promt.AiPromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Сервис для управления AI‑промтами, используемыми системой автоответов.
 * <p>
 * Отвечает за:
 * <ul>
 *     <li>создание и обновление промтов;</li>
 *     <li>хранение версий и контроль изменений;</li>
 *     <li>экспорт всех промтов в ZIP‑архив;</li>
 *     <li>получение промта по категории и подкатегории;</li>
 *     <li>формирование DTO для UI без текста промта.</li>
 * </ul>
 * <p>
 * Логика сервиса гарантирует, что:
 * <ul>
 *     <li>каждый тип промта существует в единственном экземпляре;</li>
 *     <li>обновление промта увеличивает его версию;</li>
 *     <li>невозможно сохранить идентичный текст без изменений;</li>
 *     <li>при отсутствии промта для категории используется DEFAULT.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AiPromptService {

    /**
     * Репозиторий для работы с сущностями {@link AiPrompt}.
     */
    private final AiPromptRepository repo;

    /**
     * Создаёт новый промт или обновляет существующий:
     * <ul>
     *     <li>ищет промт по типу;</li>
     *     <li>если найден — обновляет имя, текст и увеличивает версию;</li>
     *     <li>если текст или имя не изменились — выбрасывает ошибку;</li>
     *     <li>если не найден — создаёт новый промт с версией 1.</li>
     * </ul>
     *
     * @param type тип промта
     * @param name имя промта
     * @param text текст промта
     * @return сохранённый промт
     * @throws IllegalArgumentException если тип или текст некорректны
     * @throws IllegalStateException если обновление не вносит изменений
     */
    public AiPrompt createOrUpdatePrompt(AiPromptType type, String name, String text) {

        if (type == null) {
            throw new IllegalArgumentException("Prompt type cannot be null");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Prompt text cannot be empty");
        }

        Optional<AiPrompt> existingOpt = repo.findByType(type);

        if (existingOpt.isPresent()) {
            AiPrompt existing = existingOpt.get();

            if (existing.getPromptText().equals(text) || existing.getName().equals(name)) {
                throw new IllegalStateException(
                        "Текст промта идентичен текущей версии. Нечего обновлять."
                );
            }

            existing.setName(name);
            existing.setPromptText(text);
            existing.setVersion(existing.getVersion() + 1);
            return repo.save(existing);
        }

        AiPrompt prompt = AiPrompt.builder()
                .type(type)
                .name(name)
                .promptText(text)
                .version(1)
                .build();
        return repo.save(prompt);
    }

    /**
     * Экспортирует все промты в ZIP‑архив.
     * <p>
     * Каждый промт сохраняется как отдельный текстовый файл:
     * <ul>
     *     <li>имя файла = тип промта;</li>
     *     <li>содержимое = текст промта;</li>
     *     <li>кодировка UTF‑8.</li>
     * </ul>
     *
     * @return ZIP‑файл в виде массива байт
     * @throws RuntimeException если произошла ошибка записи ZIP
     */
    public byte[] exportAllPromptsAsZip() {
        List<AiPrompt> prompts = repo.findAll();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            for (AiPrompt p : prompts) {
                ZipEntry entry = new ZipEntry(p.getType().name() + ".txt");
                zos.putNextEntry(entry);
                zos.write(p.getPromptText().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to export prompts", e);
        }
    }

    /**
     * Возвращает текст промта по категории и подкатегории.
     * <p>
     * Логика:
     * <ul>
     *     <li>тип определяется через {@code AiPromptTypeResolver};</li>
     *     <li>если промт отсутствует — возвращается DEFAULT;</li>
     *     <li>если DEFAULT отсутствует — выбрасывается ошибка.</li>
     * </ul>
     *
     * @param category категория заказа
     * @param subcategory подкатегория заказа
     * @return текст промта
     */
    public String getPrompt(String category, String subcategory) {
        AiPromptType type = AiPromptTypeResolver.resolve(category, subcategory);

        return repo.findByType(type)
                .map(AiPrompt::getPromptText)
                .orElseGet(() -> repo.findByType(AiPromptType.DEFAULT)
                        .map(AiPrompt::getPromptText)
                        .orElseThrow(() -> new IllegalStateException("DEFAULT prompt not found")));
    }

    /**
     * Возвращает список DTO для UI.
     * <p>
     * DTO содержит только служебную информацию:
     * <ul>
     *     <li>ID;</li>
     *     <li>тип;</li>
     *     <li>версию;</li>
     *     <li>имя;</li>
     *     <li>даты создания и обновления.</li>
     * </ul>
     * Текст промта не включается.
     *
     * @return список {@link AiPromptDto}
     */
    public List<AiPromptDto> getAllPromptDtos() {
        return repo.findAll().stream()
                .map(p -> AiPromptDto.builder()
                        .id(p.getId())
                        .type(p.getType())
                        .version(p.getVersion())
                        .name(p.getName())
                        .createdAt(p.getCreatedAt())
                        .updatedAt(p.getUpdatedAt())
                        .build())
                .toList();
    }
}
