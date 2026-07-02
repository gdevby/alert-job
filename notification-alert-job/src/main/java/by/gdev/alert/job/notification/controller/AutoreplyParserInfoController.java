package by.gdev.alert.job.notification.controller;

import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
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
public class AutoreplyParserInfoController {
    private final List<AutoreplyPlaywrightParser> parsers;

    @GetMapping("/supported-sites")
    public List<String> supportedSites() {
        return parsers.stream()
                .map(p -> p.getSiteName().name())
                .toList();
    }

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

