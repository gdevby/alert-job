package by.gdev.alert.job.core.controller;

import by.gdev.common.model.HeaderName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autoreply")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AutoReply", description = "Управление автоответами")
public class AutoReplyController {

    @Value("${autoreply.enabled:false}")
    private boolean autoreplyEnabled;

    @Operation(
            summary = "Получить статус автоответов",
            description = "Возвращает true, если автоответы включены, иначе false."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Статус успешно получен",
            content = @Content(schema = @Schema(implementation = Boolean.class))
    )
    @GetMapping("/status")
    public boolean getStatus(@Parameter(hidden = true)
                                 @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
        return autoreplyEnabled;
    }
}