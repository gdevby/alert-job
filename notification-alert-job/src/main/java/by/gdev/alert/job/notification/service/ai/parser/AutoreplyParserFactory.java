package by.gdev.alert.job.notification.service.ai.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AutoreplyParserFactory {

    private final Map<String, AutoreplyPlaywrightParser> parsers;

    public AutoreplyPlaywrightParser getParser(String moduleName) {
        AutoreplyPlaywrightParser parser = parsers.get(moduleName.toLowerCase());
        if (parser == null) {
            throw new IllegalArgumentException("Parser not found for module: " + moduleName);
        }
        return parser;
    }
}
