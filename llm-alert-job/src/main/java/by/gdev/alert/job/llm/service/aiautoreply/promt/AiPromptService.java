package by.gdev.alert.job.llm.service.aiautoreply.promt;

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

    public AiPrompt createOrUpdatePrompt(AiPromptType type, String text) {

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
            existing.setPromptText(text);
            existing.setVersion(existing.getVersion() + 1);
            return repo.save(existing);
        }

        // Создаём новый
        AiPrompt prompt = AiPrompt.builder()
                .type(type)
                .promptText(text)
                .version(1)
                .build();

        return repo.save(prompt);
    }

    public List<AiPrompt> getAllPrompts() {
        return repo.findAll();
    }

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

    public String getPrompt(String category, String subcategory) {
        AiPromptType type = AiPromptTypeResolver.resolve(category, subcategory);

        return repo.findByType(type)
                .map(AiPrompt::getPromptText)
                .orElseGet(() -> repo.findByType(AiPromptType.DEFAULT)
                        .map(AiPrompt::getPromptText)
                        .orElseThrow(() -> new IllegalStateException("DEFAULT prompt not found")));
    }
}


