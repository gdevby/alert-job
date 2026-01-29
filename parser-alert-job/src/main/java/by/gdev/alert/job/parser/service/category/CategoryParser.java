package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.Parser;
import by.gdev.alert.job.parser.util.SiteName;

import java.util.List;
import java.util.Map;

public interface CategoryParser extends Parser {
    Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob);
}
