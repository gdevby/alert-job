package by.gdev.alert.job.notification.controller;

import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parsers")
@Tag(name = "Parsers", description = "Информация о парсерах автоответов")
@Hidden
public class AutoreplyParserInfoController {
    private final List<AutoreplyPlaywrightParser> parsers;

    @Operation(
            summary = "Получить список поддерживаемых сайтов для автоответа",
            description = "Возвращает список всех сайтов, для которых доступен парсер автоответов."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список сайтов",
            content = @Content(schema = @Schema(implementation = List.class))
    )
    @GetMapping("/supported-sites")
    public List<String> supportedSites() {
        return parsers.stream()
                .map(p -> p.getSiteName().name())
                .toList();
    }


    @Operation(
            summary = "Проверить поддержку сайта",
            description = "Возвращает информацию о том, поддерживает ли система парсинг автоответов для указанного сайта."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Результат проверки",
            content = @Content(
                    schema = @Schema(
                            example = "{\"site\":\"KWORK\",\"supported\":true}"
                    )
            )
    )
    @GetMapping("/can-parse")
    public Map<String, Object> canParse(@RequestParam String site) {
        boolean supported = parsers.stream()
                .anyMatch(p -> p.getSiteName().name().equalsIgnoreCase(site));
        return Map.of(
                "site", site,
                "supported", supported
        );
    }

}

