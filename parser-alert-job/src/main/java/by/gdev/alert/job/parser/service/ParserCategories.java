package by.gdev.alert.job.parser.service;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import by.gdev.alert.job.parser.configuration.RestTemplateConfigurer;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.parsing.FlCategories;
import by.gdev.alert.job.parser.domain.parsing.FreelancerResult;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.repository.SubCategoryRepository;
import by.gdev.alert.job.parser.util.ParserStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParserCategories {
    private final SiteSourceJobRepository siteSourceJobRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private String categoriesLinkFl = "https://www.fl.ru/prof_groups/";
    private String subcategoriesLink = "https://www.fl.ru/prof_groups/professions/?prof_group_id=%s";
    private String flRss = "https://www.fl.ru/rss/all.xml?category=%s";
    private String flRssWithSubcategory = "https://www.fl.ru/rss/all.xml?subcategory=%s&category=%s";
    private String freelanceRuRss = "https://freelance.ru/rss/index";
    private String freelanceRuRssFeed = "https://freelance.ru/rss/feed/list/s.%s";
    private String freelanceRuRssFeedSubcategories = "https://freelance.ru/rss/feed/list/s.%s.f.%s";
    private String freelancerCategory = "https://www.freelancer.com/api/projects/0.1/jobs/search";

    private RestTemplate restTemplate;

    private final RestTemplateConfigurer restTemplateConfigurer;

    @Value("${freelancehunt.proxy.active}")
    private boolean freelancehuntProxyActive;
    @Value("${freelancer.proxy.active}")
    private boolean freelancerProxyActive;
    @Value("${kwork.proxy.active}")
    private boolean kworkProxyActive;


    @Transactional
    public void parse() throws IOException {
        log.info("parsed sites and categories");
        SiteSourceJob fl = siteSourceJobRepository.findById(1L).get();
        SiteSourceJob site = siteSourceJobRepository.findById(2L).get();
        SiteSourceJob freelanceRuJob = siteSourceJobRepository.findById(3L).get();
        SiteSourceJob weblancer = siteSourceJobRepository.findById(4L).get();
        SiteSourceJob freelancehunt = siteSourceJobRepository.findById(5L).get();
        SiteSourceJob youDo = siteSourceJobRepository.findById(6L).get();
        SiteSourceJob kwork = siteSourceJobRepository.findById(7L).get();
        SiteSourceJob freelancer = siteSourceJobRepository.findById(8L).get();

        getSiteCategoriesFL().forEach((k, v) -> saveData(fl, k, v));
        getSiteCategoriesHabr(site).forEach((k, v) -> saveData(site, k, v));
        getSiteCategoriesFreelanceRu().forEach((k, v) -> saveData(freelanceRuJob, k, v));
        getSiteCategoriesWeblancer(weblancer).forEach((k, v) -> saveData(weblancer, k, v));
        getFreelancehunt(freelancehunt).forEach((k, v) -> saveData(freelancehunt, k, v));
        getYouDo(youDo).forEach((k, v) -> saveData(youDo, k, v));
        getKwork(kwork).forEach((k, v) -> saveData(kwork, k, v));
        getFreelancer().forEach((k, v) -> saveData(freelancer, k, v));
        log.info("finished parsing sites and categories");
    }

    private void saveData(SiteSourceJob site, ParsedCategory k, List<ParsedCategory> v) {
        Optional<Category> op = site.getCategories().stream()
                .filter(sc -> (Objects.nonNull(sc.getName()) && Objects.equals(sc.getName(), k.name()))
                        || (Objects.nonNull(sc.getNativeLocName())
                        && Objects.equals(sc.getNativeLocName(), k.translatedName())))
                .findAny();
        if (op.isEmpty()) {
            Category siteCategory = new Category();
            siteCategory.setName(k.name());
            siteCategory.setNativeLocName(k.translatedName);
            siteCategory.setSiteSourceJob(site);
            siteCategory.setLink(k.rss);
            op = Optional.of(categoryRepository.save(siteCategory));
            site.getCategories().add(op.get());
            log.debug("added site category {},{}, {}", site.getName(), k.name(), k.translatedName());
        }
        Category sc = op.get();
        if (Objects.isNull(sc.getSubCategories())) {
            sc.setSubCategories(new ArrayList<>());
        }
        v.forEach(sub -> {
            Optional<Subcategory> sscOp = sc.getSubCategories().stream()
                    .filter(ssc -> (Objects.nonNull(ssc.getName()) && Objects.equals(ssc.getName(), sub.name()))
                            || (Objects.nonNull(ssc.getNativeLocName())
                            && Objects.equals(ssc.getNativeLocName(), sub.translatedName())))
                    .findAny();
            if (sscOp.isEmpty()) {
                Subcategory ssc1 = new Subcategory();
                ssc1.setName(sub.name());
                ssc1.setNativeLocName(sub.translatedName);
                ssc1.setCategory(sc);
                ssc1.setLink(sub.rss);
                sc.getSubCategories().add(subCategoryRepository.save(ssc1));
                log.debug("added site subcategory {}, {},{}, ", site.getName(), ssc1.getName(),
                        ssc1.getNativeLocName());
            }
        });
    }

    private Map<ParsedCategory, List<ParsedCategory>> getSiteCategoriesFL() {
        Map<ParsedCategory, List<ParsedCategory>> map = new HashMap<>();

        restTemplate = restTemplateConfigurer.getRestTemplate();

        FlCategories flCategories = restTemplate.getForObject(categoriesLinkFl, FlCategories.class);
        flCategories.items().stream()
                        .forEach(parsedCategory -> {
                            ParsedCategory c = new ParsedCategory(parsedCategory.name_en(), parsedCategory.name(), parsedCategory.id(), String.format(flRss, parsedCategory.id()));
                            log.debug("found category {} {} {}", c.id(), c.translatedName, c.rss());
                            List<ParsedCategory> listSubcat = new ArrayList<>();
                            map.put(c, listSubcat);

                            FlCategories flCategories1 = restTemplate.getForObject(String.format(subcategoriesLink, parsedCategory.id()), FlCategories.class);
                            flCategories1.items().stream()
                                            .forEach(ee -> {
                                                ParsedCategory pc = new ParsedCategory(ee.name_en(), ee.name(), ee.id(),
                                                        String.format(flRssWithSubcategory, ee.id(), c.id()));
                                                listSubcat.add(pc);
                                                log.debug("found subcategory {} {} {} ", pc.id(), pc.translatedName, pc.rss());
                                            });

                            log.debug("subcategory size {}", listSubcat.size());
                        });
        return map;
    }

    private Map<ParsedCategory, List<ParsedCategory>> getSiteCategoriesFreelanceRu() throws IOException {
        Document doc = Jsoup.connect(freelanceRuRss).get();
        Elements res = doc.getElementById("spec-selector-id").children();
        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();
        res.stream().map(fs -> {
                    ParsedCategory c = new ParsedCategory(null, fs.text(), Long.valueOf(fs.attr(ParserStringUtils.VALUE)),
                            String.format(freelanceRuRssFeed, fs.attr(ParserStringUtils.VALUE)));
                    log.debug("found category {} {} {}", c.id(), c.translatedName, c.rss());
                    return c;
                }).filter(f -> !f.id.equals(0L))
                .forEach(f -> result.put(f, doc.getElementById("spec-" + f.id).select("label").stream().map(sc -> {
                            Long id = Long.valueOf(sc.child(0).attr("value"));
                            return new ParsedCategory(null, sc.text(), id,
                                    String.format(freelanceRuRssFeedSubcategories, f.id, id));
                        }).filter(fc -> !fc.id.equals(f.id))
                        .peek(fc -> log.debug("		found subcategory {} {} {} ", fc.id(), fc.translatedName, fc.rss()))
                        .collect(Collectors.toList())));
        return result;
    }

    private Map<ParsedCategory, List<ParsedCategory>> getSiteCategoriesHabr(SiteSourceJob site) throws IOException {
        Document doc = Jsoup.connect(site.getParsedURI()).get();
        Elements res = doc.getElementsByClass("category-group__folder");
        return res.stream().map(ee -> {
            String categoryString = ee.getElementsByClass("link_dotted js-toggle").text();
            String engNameCategory = ee.getElementsByClass("checkbox_flat").attr("for");
            ParsedCategory catNew = new ParsedCategory(engNameCategory, categoryString, null, null);
            log.debug("found category {},{}, {}", site.getName(), categoryString, engNameCategory);
            List<ParsedCategory> subList = ee.getElementsByClass("sub-categories__item").stream().map(sub -> {
                Element el = sub.select("input[value]").first();
                ParsedCategory subCategory = new ParsedCategory(el.attr("value"), sub.text(), null, null);
                log.debug("		found subcategory {}, {},{}, {}", site.getName(), subCategory.name(),
                        subCategory.translatedName, subCategory.name);
                return subCategory;
            }).collect(Collectors.toList());
            log.debug("			subcategory size {}", subList.size());
            return new SimpleEntry<>(catNew, subList);
        }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (u, v) -> {
            throw new IllegalStateException(String.format(ParserStringUtils.DUPBLICATE_KEY, u));
        }, LinkedHashMap::new));
    }

    private Map<ParsedCategory, List<ParsedCategory>> getSiteCategoriesWeblancer(SiteSourceJob weblancer)
            throws IOException {
        Document doc = Jsoup.connect(weblancer.getParsedURI()).get();
        Element allCategories = doc.getElementsByClass("category_tree list-unstyled list-wide").get(0);
        return allCategories.children().stream().filter(f -> !f.children().get(0).tagName().equals("b")).map(e -> {
            Elements elements = e.children();
            Element element = elements.get(0);
            ParsedCategory category = new ParsedCategory(null, element.text(), null, null);
            log.debug("found category {},{}, {}", element.text());
            Element subElements = elements.get(2).getElementsByClass("collapse").get(0);
            Elements subElement = subElements.children();
            List<ParsedCategory> subCategory = subElement.stream().map(sub -> {
                Element n = sub.children().get(0);
                String link = weblancer.getParsedURI() + n.attr("href").replaceAll("/jobs", "");
                log.debug("		found subcategory {}, {}", n.text(), link);
                return new ParsedCategory(null, n.text(), null, link);
            }).toList();
            return new SimpleEntry<>(category, subCategory);
        }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (k, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", k));
        }, LinkedHashMap::new));
    }

    private Map<ParsedCategory, List<ParsedCategory>> getFreelancehunt(SiteSourceJob freelancehunt) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Application");
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        if (freelancehuntProxyActive) {
			restTemplate = restTemplateConfigurer.getRestTemplateWithProxy();
		}else{
			restTemplate = restTemplateConfigurer.getRestTemplate();
		}
        ResponseEntity<String> res = restTemplate.exchange(freelancehunt.getParsedURI(), HttpMethod.GET, entity,
                String.class);
        Document doc = Jsoup.parse(res.getBody());
        Element allCategories = doc.getElementById("skill-group-selector");
        Elements el = allCategories.children().select("div.panel.panel-default");
        return el.stream().map(e -> {
            Element elemCategory = e.selectFirst("div.panel-heading");
            ParsedCategory category = new ParsedCategory(null, elemCategory.text(), null, null);
            Element elemSubCategory = e.selectFirst("ul.panel-body.collapse");
            List<ParsedCategory> subCategory = elemSubCategory.children().select("li.accordion-inner.clearfix").stream()
                    .map(sub -> {
                        // remove first element (orders count)
                        List<String> listText = Lists.newArrayList(sub.text().split(" "));
                        listText.remove(0);
                        String text = listText.stream().collect(Collectors.joining(" "));
                        String link = sub.select("a").get(0).attr("href");
                        log.debug("		found subcategory {}, {}", text, link);
                        return new ParsedCategory(null, text, null, link);
                    }).toList();
            return new SimpleEntry<>(category, subCategory);
        }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (k, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", k));
        }, LinkedHashMap::new));

    }

    private Map<ParsedCategory, List<ParsedCategory>> getYouDo(SiteSourceJob youdo) throws IOException {
        Document doc = Jsoup.connect(youdo.getParsedURI()).get();
        Element page = doc.getElementsByClass("TasksRedesignPage_categories__eixSG").get(0);
        Elements el = page.getElementsByClass("Categories_item__Vxa16");
        return el.stream().map(e -> {
            Element elemCategory = e.selectFirst("label.Checkbox_label__2Tyla");
            Elements subEl = e.getElementsByClass("Categories_subList__nDohu");
            List<ParsedCategory> subCategory = subEl.stream()
                    .flatMap(sub -> sub.select("li.Categories_subItem__GN_As").stream()).map(e1 -> {
                        Element input = e1.selectFirst("input.Checkbox_checkbox__1fWfP");
                        String link = input.attr("value");
                        log.debug("		found subcategory {}, {}", e1.text(), link);
                        return new ParsedCategory(null, e1.text(), null, link);
                    }).toList();
            String rss = subCategory.stream().map(rs -> rs.rss).collect(Collectors.joining(","));
            if (StringUtils.isEmpty(rss)) {
                rss = "all";
            }
            log.debug("found category {}, {}", elemCategory.text(), rss);
            ParsedCategory category = new ParsedCategory(null, elemCategory.text(), null, rss);
            return new SimpleEntry<>(category, subCategory);
        }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (k, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", k));
        }, LinkedHashMap::new));
    }

	private Map<ParsedCategory, List<ParsedCategory>> getKwork(SiteSourceJob kwork) {
        if (kworkProxyActive) {
            restTemplate = restTemplateConfigurer.getRestTemplateWithProxy();
        }else{
            restTemplate = restTemplateConfigurer.getRestTemplate();
        }
		ResponseEntity<String> response = restTemplate.getForEntity(kwork.getParsedURI(), String.class);
		String regex = "\\{\"CATID\":\"([0-9]{1,3})\",\"name\":\"([А-Яа-я\\w\\s\\,]*)\",\"lang\":\"[\\w]{1,2}\",\"short_name\":\"[А-Яа-я\\w\\s\\,]*\","
				+ "\"h1\":\"[А-Яа-я\\w\\s\\,]*\",\"seo\":\"[\\-\\w]*\",\"parent\":\"%s\"";
		String body = response.getBody();
		String link = "https://kwork.ru/projects?c=%s";
		Pattern categoryPattern = Pattern.compile(String.format(regex, 0));
		Matcher categoryMatcer = categoryPattern.matcher(body);
		return categoryMatcer.results().map(m -> {
			String cName = m.group(2);
			String cLink = String.format(link, m.group(1));
			log.debug("found category {}, {}", cName, cLink);
			ParsedCategory category = new ParsedCategory(null, cName, null, cLink);
			Pattern subCategoryPattern = Pattern.compile(String.format(regex, m.group(1)));
			Matcher subCategoryMatcher = subCategoryPattern.matcher(body);
			List<ParsedCategory> subList = subCategoryMatcher.results().map(m1 -> {
				String sName = m1.group(2);
				String sLink = String.format(link, m1.group(1));
				log.debug("		found subcategory {}, {}", sName, sLink);
				return new ParsedCategory(null, sName, null, sLink);
			}).toList();
			return new SimpleEntry<>(category, subList);
		}).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (existing, replacement) -> {
			return existing;
		}, LinkedHashMap::new));
	}

    private Map<ParsedCategory, List<ParsedCategory>> getFreelancer() {
        String orderLink = "https://www.freelancer.com/api/projects/0.1/projects/all?jobs[]=%s";
		if (freelancerProxyActive) {
			restTemplate = restTemplateConfigurer.getRestTemplateWithProxy();
		}else{
			restTemplate = restTemplateConfigurer.getRestTemplate();
		}
        FreelancerResult result = restTemplate.getForObject(freelancerCategory, FreelancerResult.class);
        return result.getCategories().stream().collect(Collectors.groupingByConcurrent(e -> {
            log.debug("found category {}", e.getCategory().getName());
            return new ParsedCategory(null, e.getCategory().getName(), null, null);
        }, Collectors.mapping(e -> {
            String subCategoryLink = String.format(orderLink, String.valueOf(e.getId()));
            log.debug("		found subcategory {}, {}", e.getName(), subCategoryLink);
            return new ParsedCategory(null, e.getName(), null, subCategoryLink);
        }, Collectors.toList())));
    }

    public static record ParsedCategory(String name, String translatedName, Long id, String rss) {
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