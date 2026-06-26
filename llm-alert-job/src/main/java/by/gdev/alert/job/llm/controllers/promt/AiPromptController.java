package by.gdev.alert.job.llm.controllers.promt;

import by.gdev.alert.job.llm.domain.promt.AiPrompt;
import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import by.gdev.alert.job.llm.service.aiautoreply.promt.AiPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class AiPromptController {

    private final AiPromptService promptService;

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleEnumError(MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() == AiPromptType.class) {
            return ResponseEntity.badRequest().body(
                    "Invalid prompt type: " + ex.getValue() +
                            ". Allowed values: " + Arrays.toString(AiPromptType.values())
            );
        }
        return ResponseEntity.badRequest().body("Invalid parameter: " + ex.getName());
    }

    /**
     * Загружает текст промта из файла и сохраняет его:
     *  - принимает MultipartFile;
     *  - обновляет существующий промт или создаёт новый;
     *  - увеличивает версию при обновлении.
     *
     * @param file файл с текстом промта
     * @param moduleId модуль
     * @param name имя промта
     * @return HTTP 200 при успехе
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPrompt(
            @RequestPart("file") MultipartFile file,
            @RequestParam("module") Long moduleId,
            @RequestParam("name") String name
    ) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            AiPrompt saved = promptService.createOrUpdatePrompt(name, moduleId, text);
            return ResponseEntity.ok("Prompt saved. type=" + saved.getType() + ", version=" + saved.getVersion());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Возвращает ZIP-файл со всеми промтами:
     *  - каждый промт — отдельный .txt файл;
     *  - имя файла = тип промта.
     *
     * @return ZIP в виде массива байт
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPrompts() {
        byte[] zip = promptService.exportAllPromptsAsZip();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prompts.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }

    /**
     * Возвращает список всех промтов в виде DTO:
     *  - без текста промта;
     *  - только служебная информация.
     *
     * @return список AiPromptDto
     */
    @GetMapping("/list")
    public ResponseEntity<?> listPrompts(@RequestHeader("UUID-user-header") String uuid) {
        return ResponseEntity.ok(promptService.getAllPromptDtos(uuid));
    }

}
