package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.parsing.FreelancerResult;
import by.gdev.alert.job.parser.factory.RestTemplateFactory;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FreelancerCategoryParser implements CategoryParser {

    @Value("${freelancer.proxy.active}")
    private boolean freelancerProxyActive;
    private String freelancerCategory = "https://www.freelancer.com/api/projects/0.1/jobs/search";

    private final RestTemplateFactory restTemplateFactory;

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        String orderLink = "https://www.freelancer.com/api/projects/0.1/projects/all?jobs[]=%s";

        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(freelancerProxyActive);

        FreelancerResult result = restTemplate.getForObject(freelancerCategory, FreelancerResult.class);

        return result.getCategories().stream()
                .collect(Collectors.groupingByConcurrent(freelancerSubCategory -> {

                    log.debug("found category {}", freelancerSubCategory.getCategory().getName());
                    return new ParsedCategory(null, freelancerSubCategory.getCategory().getName(), null, null);

                }, Collectors.mapping(freelancerSubCategory -> {

                    String subCategoryLink = String.format(orderLink, freelancerSubCategory.getId());
                    log.debug("found subcategory {}, {}", freelancerSubCategory.getName(), subCategoryLink);
                    return new ParsedCategory(null, freelancerSubCategory.getName(), null, subCategoryLink);

                }, Collectors.toList())));
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCER;
    }
}
