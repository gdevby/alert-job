package by.gdev.alert.job.parser.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import by.gdev.alert.job.parser.domain.SiteCategoryDTO;
import by.gdev.alert.job.parser.domain.SiteSourceDTO;
import by.gdev.alert.job.parser.domain.SiteSubCategoryDTO;
import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.OrderLinks;
import by.gdev.alert.job.parser.domain.db.SiteCategory;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.SubCategory;
import by.gdev.alert.job.parser.exeption.ResourceNotFoundException;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import by.gdev.alert.job.parser.repository.SiteCategoryRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Service
@RequiredArgsConstructor
public class ParserService {

	private final SiteSourceJobRepository siteSourceJobRepository;
	private final SiteCategoryRepository siteCategoryRepository;
	private final CategoryRepository categoryRepository;
	private final OrderLinksRepository linkRepository;

	private final ModelMapper mapper;

	public List<SiteSourceDTO> getSites() {
		return Lists.newArrayList(siteSourceJobRepository.findAll()).stream()
				.map(s -> mapper.map(s, SiteSourceDTO.class)).collect(Collectors.toList());
	}

	@Transactional
	public List<SiteCategoryDTO> getCategories(Long id) {
		SiteSourceJob ssj = siteSourceJobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException());
		return ssj.getSiteCategories().stream().map(e -> {
			return mapper.map(e, SiteCategoryDTO.class);
		}).collect(Collectors.toList());
	}

	@Transactional
	public List<SiteSubCategoryDTO> getSubCategories(Long category) {
		SiteCategory sc = siteCategoryRepository.findById(category).orElseThrow(() -> new ResourceNotFoundException());
		return sc.getSiteSubCategories().stream().map(e -> mapper.map(e, SiteSubCategoryDTO.class))
				.collect(Collectors.toList());
	}

	public boolean isExistsOrder(Category category, SubCategory subCategory, String link) {
		return !linkRepository.existsByCategoryAndSubCategoryAndLinks(category, subCategory, link);
	}

	public void saveOrderLinks(Category category, SubCategory subCategory, String link) {
		OrderLinks ol = new OrderLinks();
		ol.setCategory(category);
		ol.setSubCategory(subCategory);
		ol.setLinks(link);
		linkRepository.save(ol);
	}

}