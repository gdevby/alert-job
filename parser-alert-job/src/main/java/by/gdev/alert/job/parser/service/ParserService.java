package by.gdev.alert.job.parser.service;

import java.util.Iterator;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.gdev.alert.job.parser.domain.SiteCategoryDTO;
import by.gdev.alert.job.parser.domain.SiteSourceDTO;
import by.gdev.alert.job.parser.domain.SiteSubCategoryDTO;
import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.OrderLinks;
import by.gdev.alert.job.parser.domain.db.SiteCategory;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.SiteSubCategory;
import by.gdev.alert.job.parser.domain.db.SubCategory;
import by.gdev.alert.job.parser.exeption.ResourceNotFoundException;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import by.gdev.alert.job.parser.repository.SiteCategoryRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Data
@Service
@RequiredArgsConstructor
public class ParserService {

	private final SiteSourceJobRepository siteSourceJobRepository;
	private final SiteCategoryRepository siteCategoryRepository;
	private final CategoryRepository categoryRepository;
	private final OrderLinksRepository linkRepository;

	private final ModelMapper mapper;

	public Flux<SiteSourceDTO> getSites() {
		return Flux.create(flux -> {
			Iterator<SiteSourceJob> iterator = siteSourceJobRepository.findAll().iterator();
			while (iterator.hasNext()) {
				flux.next(mapper.map(iterator.next(), SiteSourceDTO.class));
			}
			flux.complete();
		});
	}

	@Transactional(readOnly = true)
	public Flux<SiteCategoryDTO> getCategories(Long id) {
		return Flux.create(flux -> {
			SiteSourceJob ssj = siteSourceJobRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException(""));
			Iterator<SiteCategory> iterator = ssj.getSiteCategories().iterator();
			while (iterator.hasNext()) {
				flux.next(mapper.map(iterator.next(), SiteCategoryDTO.class));
			}
			flux.complete();
		});
	}

	@Transactional(readOnly = true)
	public Flux<SiteSubCategoryDTO> getSubCategories(Long category) {
		return Flux.create(flux -> {
			SiteCategory sc = siteCategoryRepository.findById(category)
					.orElseThrow(() -> new ResourceNotFoundException());
			Iterator<SiteSubCategory> iterator = sc.getSiteSubCategories().iterator();
			while (iterator.hasNext()) {
				flux.next(mapper.map(iterator.next(), SiteSubCategoryDTO.class));
			}
			flux.complete();
		});
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