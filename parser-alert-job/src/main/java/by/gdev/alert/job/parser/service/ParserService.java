package by.gdev.alert.job.parser.service;

import java.util.Iterator;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

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
import by.gdev.common.model.SiteCategoryDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SiteSubCategoryDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
	
	public Flux<SiteCategoryDTO> getCategories(Long id) {
		return Flux.create(flux -> {
			SiteSourceJob ssj = siteSourceJobRepository.findOneEager(id)
					.orElseThrow(() -> new ResourceNotFoundException(""));
			Iterator<SiteCategory> iterator = ssj.getSiteCategories().iterator();
			while (iterator.hasNext()) {
				flux.next(mapper.map(iterator.next(), SiteCategoryDTO.class));
			}
			flux.complete();
		});
	}
	
	public Flux<SiteSubCategoryDTO> getSubCategories(Long category) {
		return Flux.create(flux -> {
			SiteCategory sc = siteCategoryRepository.findOneEager(category)
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
	
	public Mono<SiteSourceDTO> getSite(Long id) {
		return Mono
				.just(siteSourceJobRepository.findById(id)
						.orElseThrow(() -> new ResourceNotFoundException("not found site with id " + id)))
				.map(e -> mapper.map(e, SiteSourceDTO.class));
	}
	
	public Mono<SiteCategoryDTO> getCategory(Long id, Long cId) {
		return Mono.create(m -> {
			SiteSourceJob ssj = siteSourceJobRepository.test(id, cId);
			SiteCategory c = ssj.getSiteCategories().stream().findFirst().get();
			m.success(mapper.map(c, SiteCategoryDTO.class));
		});
	}

	public Mono<SiteSubCategoryDTO> getSubCategory(Long cId, Long sId){
		return Mono.create(m -> {
			SiteCategory sc = siteCategoryRepository.test(cId, sId);
			SiteSubCategory sub = sc.getSiteSubCategories().stream().findFirst().get();
			m.success(mapper.map(sub, SiteSubCategoryDTO.class));
		});
	}
}