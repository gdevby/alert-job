package by.gdev.alert.job.parser.service;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.OrderLinks;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.repository.SubCategoryRepository;
import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SubCategoryDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Data
@Service
@RequiredArgsConstructor
@Slf4j
public class ParserService {

	private final SiteSourceJobRepository siteSourceJobRepository;
	private final CategoryRepository categoryRepository;
	private final SubCategoryRepository subCategoryRepository;
	private final OrderLinksRepository linkRepository;
	private final ParserSourceRepository parserSourceRepository;

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
	
	public Flux<CategoryDTO> getCategories(Long id) {
		return Flux.create(flux -> {
			SiteSourceJob ssj = siteSourceJobRepository.findOneEager(id)
					.orElseThrow(() -> new ResourceNotFoundException(""));
			Iterator<Category> iterator = ssj.getCategories().iterator();
			while (iterator.hasNext()) {
				flux.next(mapper.map(iterator.next(), CategoryDTO.class));
			}
			flux.complete();
		});
	}
	
	public Flux<SubCategoryDTO> getSubCategories(Long category) {
		return Flux.create(flux -> {
			Category sc = categoryRepository.findOneEager(category)
					.orElseThrow(() -> new ResourceNotFoundException());
			Iterator<Subcategory> iterator = sc.getSubCategories().iterator();
			while (iterator.hasNext()) {
				flux.next(mapper.map(iterator.next(), SubCategoryDTO.class));
			}
			flux.complete();
		});
	}
	
	public boolean isExistsOrder(Category category, Subcategory subCategory, String link) {
		return !linkRepository.existsByCategoryAndSubCategoryAndLinks(category, subCategory, link);
	}

	public void saveOrderLinks(Category category, Subcategory subCategory, String link) {
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
	
	public Mono<CategoryDTO> getCategory(Long id, Long cId) {
		return Mono.create(m -> {
			SiteSourceJob ssj = siteSourceJobRepository.findByIdAndCategory(id, cId);
			Category c = ssj.getCategories().stream().findFirst().get();
			m.success(mapper.map(c, CategoryDTO.class));
		});
	}

	public Mono<SubCategoryDTO> getSubCategory(Long cId, Long sId){
		return Mono.create(m -> {
			Category sc = categoryRepository.findByIdAndSubCategory(cId, sId);
			Subcategory sub = sc.getSubCategories().stream().findFirst().get();
			m.success(mapper.map(sub, SubCategoryDTO.class));
		});
	}
	
	public Mono<Void> subcribeOnSource(Long categoryId, Long subCategoryId, boolean cValue, boolean sValue){
		return Mono.create(m -> {
			Category c = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException());
			c.setParse(cValue);
			categoryRepository.save(c);
			if (Objects.nonNull(subCategoryId)) {
				Subcategory s = subCategoryRepository.findById(subCategoryId).orElseThrow(() -> new ResourceNotFoundException());
				s.setParse(sValue);
				subCategoryRepository.save(s);
			}
			log.trace("changed parser value {} {}, {} {}",categoryId, cValue, subCategoryId, sValue);
			m.success();
		});
	}
	
	public Flux<OrderDTO> getOrdersBySource(Long source, Long category, Long subcategory) {
		return Flux.just(parserSourceRepository.findBySourceAndCategoryAndSubCategory(source, category, subcategory))
				.flatMapIterable(e -> e.get().getOrders()).map(e -> mapper.map(e, OrderDTO.class)).onErrorResume(
						NoSuchElementException.class, e -> Flux.error(new ResourceNotFoundException("user not found")));
	}
}