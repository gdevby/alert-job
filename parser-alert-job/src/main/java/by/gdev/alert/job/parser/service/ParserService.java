package by.gdev.alert.job.parser.service;

import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.OrderLinks;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.OrderLinksRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.repository.SubCategoryRepository;
import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SourceSiteDTO;
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
    private final OrderRepository orderRepository;

    private final ModelMapper mapper;

    public Flux<SiteSourceDTO> getSites() {
	return Flux.fromIterable(siteSourceJobRepository.findAll()).map(e -> mapper.map(e, SiteSourceDTO.class));
    }

    public Flux<CategoryDTO> getCategories(Long id) {
	return Flux.fromIterable(categoryRepository.findAllBySourceId(id)).map(e -> mapper.map(e, CategoryDTO.class))
		.switchIfEmpty(Flux.error(new ResourceNotFoundException("not found category with source id " + id)));
    }

    public Flux<SubCategoryDTO> getSubCategories(Long category) {
	return Flux.fromIterable(subCategoryRepository.findAllByCategoryId(category))
		.map(e -> mapper.map(e, SubCategoryDTO.class)).switchIfEmpty(Flux
			.error(new ResourceNotFoundException("not found sub category with category id " + category)));
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
	return Mono.justOrEmpty(siteSourceJobRepository.findById(id)).map(e -> mapper.map(e, SiteSourceDTO.class))
		.switchIfEmpty(Mono.error(new ResourceNotFoundException("not found site with id " + id)));
    }

    public Mono<CategoryDTO> getCategory(Long id, Long cId) {
	return Mono.justOrEmpty(categoryRepository.findByIdAndSourceId(cId, id))
		.map(e -> mapper.map(e, CategoryDTO.class)).switchIfEmpty(Mono.error(new ResourceNotFoundException(
			String.format("not found category by category %s and source %s", cId, id))));
    }

    public Mono<SubCategoryDTO> getSubCategory(Long cId, Long sId) {
	return Mono.justOrEmpty(subCategoryRepository.findByIdAndCategoryId(sId, cId))
		.map(e -> mapper.map(e, SubCategoryDTO.class)).switchIfEmpty(Mono.error(new ResourceNotFoundException(
			String.format("not found sub category by sub category %s and category %s", sId, cId))));
    }

    public Mono<Void> subcribeOnSource(Long categoryId, Long subCategoryId, boolean cValue, boolean sValue) {
	return Mono.create(m -> {
	    Category c = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException());
	    c.setParse(cValue);
	    categoryRepository.save(c);
	    if (Objects.nonNull(subCategoryId)) {
		Subcategory s = subCategoryRepository.findById(subCategoryId)
			.orElseThrow(() -> new ResourceNotFoundException());
		s.setParse(sValue);
		subCategoryRepository.save(s);
	    }
	    log.trace("changed parser value {} {}, {} {}", categoryId, cValue, subCategoryId, sValue);
	    m.success();
	});
    }

    public Flux<OrderDTO> getOrdersBySource(Long source, Long category, Long subcategory) {
	return Flux.fromIterable(Objects.isNull(subcategory)
		? orderRepository.findAllBySourceSubCategoryIsNullOneEagerTechnologies(source, category)
		: orderRepository.findAllBySourceOneEagerTechnologies(source, category, subcategory)).map(e -> {
		    OrderDTO dto = mapper.map(e, OrderDTO.class);
		    SourceSiteDTO s = dto.getSourceSite();

		    String sourceName = siteSourceJobRepository.findById(s.getSource())
			    .orElseThrow(
				    () -> new ResourceNotFoundException("don't found by source id " + s.getSource()))
			    .getName();
		    s.setSourceName(sourceName);
		    String categoryName = categoryRepository.findById(s.getCategory()).orElseThrow(
			    () -> new ResourceNotFoundException("don't found by category id " + s.getCategory()))
			    .getNativeLocName();
		    s.setCategoryName(categoryName);

		    if (Objects.nonNull(s.getSubCategory())) {
			String subCategoryName = subCategoryRepository.findById(s.getSubCategory())
				.orElseThrow(() -> new ResourceNotFoundException(
					"don't found by subcategory id " + s.getSubCategory()))
				.getNativeLocName();
			s.setSubCategoryName(subCategoryName);
		    }
		    dto.setSourceSite(s);
		    return dto;
		});
    }
}