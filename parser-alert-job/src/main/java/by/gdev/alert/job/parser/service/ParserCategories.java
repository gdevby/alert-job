package by.gdev.alert.job.parser.service;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.FlCategory;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.FlCategoryRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.repository.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParserCategories {
	private final SiteSourceJobRepository siteSourceJobRepository;
	private final CategoryRepository categoryRepository;
	private final SubCategoryRepository subCategoryRepository;
	private final FlCategoryRepository flCategoryRepository;
	private String flRss = "https://www.fl.ru/rss/all.xml?category=%s";
	private String flRssWithSubcategory = "https://www.fl.ru/rss/all.xml?subcategory=%s&category=%s";

	@Transactional
	public void parse() throws IOException {
		log.info("parsed sites and categories");
		SiteSourceJob fl = siteSourceJobRepository.findById(1L).get();
		SiteSourceJob site = siteSourceJobRepository.findById(2L).get();
		getSiteCategoriesFL(fl).forEach((k, v) -> saveData(fl, k, v));
		getSiteCategoriesHabr(site).forEach((k, v) -> saveData(site, k, v));

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
		if (Objects.isNull(sc.getSubCategories()))
			sc.setSubCategories(new ArrayList<Subcategory>());
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

	private Map<ParsedCategory, List<ParsedCategory>> getSiteCategoriesFL(SiteSourceJob site) throws IOException {
		Pattern p = Pattern.compile("(\\d*),'(.*?)'");
		Document doc = Jsoup.connect(site.getParsedURI()).get();
		Elements res = doc.select(".dropdown-menu.entire-scroll > a[href^=/freelancers/]");
		Map<Long, FlCategory> flList = res.stream().map(e -> {
			return flCategoryRepository.findByNativeLocName(e.text())
					.orElseThrow(() -> new RuntimeException("fl.ru has new category " + e.text()));
		}).filter(Objects::nonNull).collect(Collectors.toMap(FlCategory::getId, i -> i));
		String s = doc.html();
		int start = s.indexOf("filter_specs[2]");
		int end = s.indexOf("filter_specs_ids", start);
		String res1 = s.substring(start, end);
		String[] filter_specs = res1.split("filter_specs");
		Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();
		for (String fs : filter_specs) {
			if (fs.isEmpty())
				continue;
			String oneFilterSpec[] = fs.split("=");
			FlCategory flCategory = flList
					.get(Long.valueOf(oneFilterSpec[0].replaceFirst("\\[", "").replaceFirst("\\]", "")));
			ParsedCategory c = new ParsedCategory(null, flCategory.getNativeLocName(), flCategory.getId(),
					String.format(flRss, flCategory.getId().toString()));
			String afs = oneFilterSpec[1];
			String pairs = afs.substring(1, oneFilterSpec[1].length() - 2);
			java.util.regex.Matcher m = p.matcher(pairs);
			List<ParsedCategory> list = m.results().map(v -> {
				Long id = Long.valueOf(v.group(1));
				return new ParsedCategory(null, v.group(2), id,
						String.format(flRssWithSubcategory, id.toString(), flCategory.getId()));
			}).collect(Collectors.toList());
			log.debug("found category {} {} {}", c.id(), c.translatedName, c.rss());
			list.forEach(sc -> {
				log.debug("		found subcategory {} {} {} ", sc.id(), sc.translatedName, sc.rss());
			});
			result.put(c, list);
			log.debug("			subcategory size {}", list.size());
		}
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
			return new SimpleEntry<ParsedCategory, List<ParsedCategory>>(catNew, subList);
		}).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (u, v) -> {
			throw new IllegalStateException(String.format("Duplicate key %s", u));
		}, LinkedHashMap::new));
	}

	public static record ParsedCategory(String name, String translatedName, Long id, String rss) {
	}

	@SneakyThrows
	public void updateHubrLink(List<String> lineFies) {
		SiteSourceJob sites = siteSourceJobRepository.findByName("HABR");
		List<Category> categories = sites.getCategories();
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
						} else
							log.warn("dont find sub category with name " + name1);
					}
				}
			} else
				log.warn("dont find category with name " + name);
		});
	}

	@SneakyThrows
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
