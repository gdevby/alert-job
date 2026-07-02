package by.gdev.alert.job.llm.service.aiautoreply.promt;

import by.gdev.alert.job.llm.client.CoreClient;
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

    private final AiPromptRepository aiPromptRepository;
    private final CoreClient coreClient;

    /**
     * Создаёт новый промт или обновляет существующий:
     *  - ищет по типу;
     *  - если найден — обновляет текст и увеличивает версию;
     *  - если нет — создаёт новую запись.
     *
     * @param name имя промта
     * @param text текст промта
     * @return сохранённый промт
     */
    public AiPrompt createOrUpdatePrompt(String name, Long moduleId, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Текст промта не должен быть пустым");
        }

        Optional<AiPrompt> existingOpt;

        // DEFAULT PROMPT
        if (moduleId == null) {
            existingOpt = aiPromptRepository.findByType(AiPromptType.DEFAULT);
        } else {
            // PROMPT FOR MODULE
            existingOpt = aiPromptRepository.findByModuleId(moduleId);
        }

        if (existingOpt.isPresent()) {
            AiPrompt existing = existingOpt.get();

            boolean sameName = existing.getName().equals(name);
            boolean sameText = existing.getPromptText().equals(text);

            // Ошибка только если НИЧЕГО не изменилось
            if (sameName && sameText) {
                throw new IllegalStateException("Промт не изменён — обновлять нечего");
            }

            existing.setName(name);
            existing.setPromptText(text);
            existing.setVersion(existing.getVersion() + 1);

            return aiPromptRepository.save(existing);
        }

        // Создание нового промта
        AiPrompt prompt = AiPrompt.builder()
                .type(moduleId == null ? AiPromptType.DEFAULT : AiPromptType.MODULE)
                .name(name)
                .moduleId(moduleId)
                .promptText(text)
                .version(1)
                .build();

        return aiPromptRepository.save(prompt);
    }

    /**
     * Экспортирует все промты в ZIP:
     *  - каждый промт сохраняется как отдельный .txt файл;
     *  - имя файла = тип промта.
     *
     * @return ZIP-файл в виде массива байт
     */
    public byte[] exportAllPromptsAsZip() {
        List<AiPrompt> prompts = aiPromptRepository.findAll();

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
     * @param moduleId ид модуля
     * @return текст промта
     */
    public String getPrompt(Long moduleId) {
        // Если указан модуль — ищем промт модуля
        if (moduleId != null) {
            return aiPromptRepository.findByModuleId(moduleId)
                    .map(AiPrompt::getPromptText)
                    .orElseGet(() -> getDefaultPrompt());
        }

        // Иначе — DEFAULT
        return getDefaultPrompt();
    }

    private String getDefaultPrompt() {
        return aiPromptRepository.findByType(AiPromptType.DEFAULT)
                .map(AiPrompt::getPromptText)
                .orElseThrow(() -> new IllegalStateException("DEFAULT prompt not found"));
    }


    /**
     * Возвращает список DTO промтов пользователя для UI:
     *  - без текста промта;
     *  - только общая информация.
     *
     * @return список AiPromptDto
     */
    public List<AiPromptDto> getAllPromptDtos(String uuid) {
        return aiPromptRepository.findAll().stream()
                .map(p -> AiPromptDto.builder()
                        .id(p.getId())
                        .type(p.getType())
                        .version(p.getVersion())
                        .name(p.getName())
                        .moduleId(p.getModuleId())
                        .moduleName(resolveModuleName(p.getModuleId(), uuid))
                        .createdAt(p.getCreatedAt())
                        .updatedAt(p.getUpdatedAt())
                        .build())
                .toList();
    }

    private String resolveModuleName(Long moduleId, String userUuid) {
        if (moduleId == null) {
            return "DEFAULT";
        }
        return coreClient.getModuleName(userUuid, moduleId);
    }

}