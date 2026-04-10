package by.gdev.alert.job.llm.controllers.promt;

import by.gdev.alert.job.llm.domain.promt.AiPrompt;
import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import by.gdev.alert.job.llm.service.aiautoreply.promt.AiPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class AiPromptController {

    private final AiPromptService promptService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPrompt(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") AiPromptType type
    ) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            AiPrompt saved = promptService.createOrUpdatePrompt(type, text);
            return ResponseEntity.ok("Prompt saved. type=" + type + ", version=" + saved.getVersion());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPrompts() {
        byte[] zip = promptService.exportAllPromptsAsZip();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prompts.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }


}
