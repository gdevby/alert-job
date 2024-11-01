package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.repository.SubCategoryRepository;
import by.gdev.alert.job.parser.util.SiteName;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParserCategories {
    private final SiteSourceJobRepository siteSourceJobRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;


    @Autowired
    @Qualifier("categoryParserMap")
    private Map<SiteName, CategoryParser> categoryParserMap;


    @Transactional
    public void parse() throws IOException {
        log.info("parsed sites and categories");

        categoryParserMap.forEach((siteName, categoryParser) -> {
                    Optional<SiteSourceJob> optionalSiteSourceJob = siteSourceJobRepository.findById(siteName.getId());
                    optionalSiteSourceJob.ifPresentOrElse(siteSourceJob -> {
                        Map<ParsedCategory, List<ParsedCategory>> parsed = categoryParser.parse(siteSourceJob);
                        parsed.forEach((category, subCategories) -> saveData(siteSourceJob, category, subCategories));
                    }, () -> {
                        throw new RuntimeException(String.format("Cannot find %s site", siteName));
                    });
                }
        );

        log.info("finished parsing sites and categories");
    }

    private void saveData(SiteSourceJob site, ParsedCategory parsedCategory, List<ParsedCategory> subCategories) {
        Optional<Category> op = site.getCategories().stream()
                .filter(category -> (Objects.nonNull(category.getName()) && Objects.equals(category.getName(), parsedCategory.name()))
                        || (Objects.nonNull(category.getNativeLocName()) && Objects.equals(category.getNativeLocName(), parsedCategory.translatedName()))
                )
                .findAny();
        if (op.isEmpty()) {
            Category siteCategory = new Category();
            siteCategory.setName(parsedCategory.name());
            siteCategory.setNativeLocName(parsedCategory.translatedName());
            siteCategory.setSiteSourceJob(site);
            siteCategory.setLink(parsedCategory.rss());
            op = Optional.of(categoryRepository.save(siteCategory));
            site.getCategories().add(op.get());
            log.debug("added site category {},{}, {}", site.getName(), parsedCategory.name(), parsedCategory.translatedName());
        }
        Category sc = op.get();
        if (Objects.isNull(sc.getSubCategories())) {
            sc.setSubCategories(new ArrayList<>());
        }
        subCategories.forEach(subCategory -> {
            Optional<Subcategory> sscOp = sc.getSubCategories().stream()
                    .filter(subcategory -> (Objects.nonNull(subcategory.getName()) && Objects.equals(subcategory.getName(), subCategory.name()))
                            || (Objects.nonNull(subcategory.getNativeLocName()) && Objects.equals(subcategory.getNativeLocName(), subCategory.translatedName()))
                    )
                    .findAny();
            if (sscOp.isEmpty()) {
                Subcategory ssc1 = new Subcategory();
                ssc1.setName(subCategory.name());
                ssc1.setNativeLocName(subCategory.translatedName());
                ssc1.setCategory(sc);
                ssc1.setLink(subCategory.rss());
                sc.getSubCategories().add(subCategoryRepository.save(ssc1));
                log.debug("added site subcategory {}, {},{}, ", site.getName(), ssc1.getName(),
                        ssc1.getNativeLocName());
            }
        });
    }

    public void updateHubrLink(List<String> lineFies) {
        List<Category> categories = siteSourceJobRepository.findById(2L).get().getCategories();
        Map<String, List<String>> map = aggregateByKeys(lineFies);
        map.forEach((k, v) -> {
            String[] l = k.split("\t");
            String name = l[0];
            String link = l[1];
            Optional<Category> cat = categories.stream().filter(n -> n.getNativeLocName().equals(name)).findAny();
            if (cat.isPresent()) {
                Category presentCategory = cat.get();
                if (StringUtils.isEmpty(presentCategory.getLink()) || !presentCategory.getLink().equals(link)) {
                    presentCategory.setLink(link);
                    presentCategory = categoryRepository.save(presentCategory);
                    log.info(String.format("update link category with id = %s and name %s", presentCategory.getId(),
                            presentCategory.getNativeLocName()));
                    for (String s : v) {
                        String[] l1 = s.split("\t");
                        String name1 = l1[0];
                        String link1 = l1[1];
                        Optional<Subcategory> sub = presentCategory.getSubCategories().stream()
                                .filter(n -> n.getNativeLocName().equals(name1)).findAny();
                        if (sub.isPresent()) {
                            Subcategory presentSubCategory = sub.get();
                            if (StringUtils.isEmpty(presentSubCategory.getLink())
                                    || !presentSubCategory.getLink().equals(link1)) {
                                presentSubCategory.setLink(link1);
                                presentSubCategory = subCategoryRepository.save(presentSubCategory);
                                log.info(String.format("update link sub category with id = %s and name %s",
                                        presentSubCategory.getId(), presentSubCategory.getNativeLocName()));
                            }
                        } else {
                            log.warn("dont find sub category with name " + name1);
                        }
                    }
                }
            } else {
                log.warn("dont find category with name " + name);
            }
        });
    }

    private Map<String, List<String>> aggregateByKeys(List<String> filePath) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        String category = "";
        for (String line : filePath) {
            if (!line.startsWith("\t")) {
                category = line;
                map.put(line, Lists.newArrayList());
            } else {
                String value = line.replaceFirst("\t", "");
                if (map.containsKey(category)) {
                    map.get(category).add(value);
                } else {
                    map.put(category, Lists.newArrayList(value));
                }
            }
        }
        return map;
    }
}