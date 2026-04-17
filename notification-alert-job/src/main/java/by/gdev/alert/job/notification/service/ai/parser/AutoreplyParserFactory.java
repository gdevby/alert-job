package by.gdev.alert.job.notification.service.ai.parser;

import by.gdev.common.model.SiteName;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AutoreplyParserFactory {

    private final Map<SiteName, AutoreplyPlaywrightParser> registry = new HashMap<>();

    public AutoreplyParserFactory(List<AutoreplyPlaywrightParser> parsers) {
        for (AutoreplyPlaywrightParser parser : parsers) {
            registry.put(parser.getSiteName(), parser);
        }
    }

    public AutoreplyPlaywrightParser getParser(SiteName site) {
        return registry.get(site);
    }

    public AutoreplyPlaywrightParser getParser(String moduleName) {
        return registry.get(SiteName.valueOf(moduleName.toUpperCase()));
    }
}

