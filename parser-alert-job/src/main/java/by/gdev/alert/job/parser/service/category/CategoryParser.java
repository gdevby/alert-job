package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.SiteName;

import java.util.List;
import java.util.Map;

public interface CategoryParser {
    Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob);
    SiteName getSiteName();
}
