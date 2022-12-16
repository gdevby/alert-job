package by.gdev.alert.job.parser.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import by.gdev.alert.job.parser.domain.Category;
import by.gdev.alert.job.parser.domain.EnumSite;
import by.gdev.alert.job.parser.domain.SubCategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.SiteCategoryRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Service
@RequiredArgsConstructor
public class AlertService {
	
	private final SiteSourceJobRepository siteSourceJobRepository;
	private final SiteCategoryRepository siteCategoryRepository;
	private final CategoryRepository categoryRepository;
	
	
	@Transactional
	public List<Category> getCategories(EnumSite site){
		return siteSourceJobRepository.findAllByName(site.name()).stream().map(e -> e.getSiteCategories())
				.flatMap(e2 -> e2.stream().map(m -> m.getCategory())).collect(Collectors.toList());
	}
	
	@Transactional
	public List<SubCategory> getSubCategories(String category) {
		Category c = categoryRepository.findByName(category).orElseThrow(() -> new RuntimeException());
		return siteCategoryRepository.findByCategory(c).stream().map(e -> e.getSiteSubCategories())
				.flatMap(e2 -> e2.stream().map(m -> m.getSubCategory())).collect(Collectors.toList());
	}
}