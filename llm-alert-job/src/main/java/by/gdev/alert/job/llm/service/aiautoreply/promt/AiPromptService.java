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

@Service
@RequiredArgsConstructor
public class AiPromptService {

    private final AiPromptRepository repo;

    /**
     * Создаёт новый промт или обновляет существующий:
     *  - ищет по типу;
     *  - если найден — обновляет текст и увеличивает версию;
     *  - если нет — создаёт новую запись.
     *
     * @param type тип промта
     * @param name имя промта
     * @param text текст промта
     * @return сохранённый промт
     */
    public AiPrompt createOrUpdatePrompt(AiPromptType type, String name, String text) {

        if (type == null) {
            throw new IllegalArgumentException("Prompt type cannot be null");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Prompt text cannot be empty");
        }

        // Ищем существующий промт по типу
        Optional<AiPrompt> existingOpt = repo.findByType(type);

        if (existingOpt.isPresent()) {
            // Обновляем существующий
            AiPrompt existing = existingOpt.get();

            // Если текст не изменился — кидаем ошибку
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

        // Создаём новый
        AiPrompt prompt = AiPrompt.builder()
                .type(type)
                .name(name)
                .promptText(text)
                .version(1)
                .build();
        return repo.save(prompt);
    }

    /**
     * Экспортирует все промты в ZIP:
     *  - каждый промт сохраняется как отдельный .txt файл;
     *  - имя файла = тип промта.
     *
     * @return ZIP-файл в виде массива байт
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
     * Возвращает текст промта по категории и подкатегории:
     *  - резолвит тип через AiPromptTypeResolver;
     *  - если промт отсутствует — возвращает DEFAULT.
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
     * Возвращает список DTO для UI:
     *  - без текста промта;
     *  - только общая информация.
     *
     * @return список AiPromptDto
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


