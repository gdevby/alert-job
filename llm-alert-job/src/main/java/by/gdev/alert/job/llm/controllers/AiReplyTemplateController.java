package by.gdev.alert.job.llm.controllers;

import by.gdev.alert.job.llm.domain.AiReplyTemplate;
import by.gdev.alert.job.llm.domain.dto.template.CreateTemplateRequest;
import by.gdev.alert.job.llm.domain.dto.template.TemplateResponse;
import by.gdev.alert.job.llm.service.template.AiReplyTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Slf4j
public class AiReplyTemplateController {

    private final AiReplyTemplateService templateService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrUpdate(@RequestBody CreateTemplateRequest req) {
        try {
            AiReplyTemplate template = templateService.createOrUpdateTemplate(req);
            return ResponseEntity.ok(template.getId());
        } catch (Exception e) {
            log.error("Failed to create/update template", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{uuid}")
    public ResponseEntity<?> getTemplatesByUser(@PathVariable String uuid) {
        try {
            List<AiReplyTemplate> templates = templateService.getTemplatesByUser(uuid);

            List<TemplateResponse> result = templates.stream().map(t -> {
                TemplateResponse dto = new TemplateResponse();
                dto.setName(t.getName());
                dto.setId(t.getId());
                dto.setHtmlTemplate(t.getHtmlTemplate());
                dto.setCreatedAt(
                        t.getCreatedAt() != null
                                ? t.getCreatedAt().toString()
                                : LocalDateTime.now().toString()
                );
                return dto;
            }).toList();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to load templates for user {}", uuid, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<?> exists(@PathVariable Long id) {
        try {
            boolean exists = templateService.exists(id);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            log.error("Failed to check template {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(@PathVariable Long id) {
        try {
            AiReplyTemplate t = templateService.getTemplateById(id);
            TemplateResponse dto = new TemplateResponse();
            dto.setId(t.getId());
            dto.setName(t.getName());
            dto.setHtmlTemplate(t.getHtmlTemplate());
            dto.setCreatedAt(
                    t.getCreatedAt() != null
                            ? t.getCreatedAt().toString()
                            : LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
