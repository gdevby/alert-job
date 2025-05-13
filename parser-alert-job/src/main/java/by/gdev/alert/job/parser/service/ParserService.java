package by.gdev.alert.job.parser.service;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.OrderLinks;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
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
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

	public List<SiteSourceDTO>  getSites() {
		return siteSourceJobRepository.findAllByActiveTrue().stream()
				.map(el -> mapper.map(el, SiteSourceDTO.class))
				.toList();
	}

	public List<CategoryDTO> getCategories(Long id) {
		List<CategoryDTO> list = categoryRepository.findAllBySourceIdAndSourceActive(id).stream()
				.map(el -> mapper.map(el, CategoryDTO.class))
				.toList();
		if (list.isEmpty()) {
			throw new ResourceNotFoundException("not found category with source id " + id);
		}
		return list;
	}

	public List<SubCategoryDTO> getSubCategories(Long category) {
		List<SubCategoryDTO> list = subCategoryRepository.findAllByCategoryId(category).stream()
				.map(el -> mapper.map(el, SubCategoryDTO.class))
				.toList();
		if (list.isEmpty()) {
			throw new ResourceNotFoundException("not found sub category with category id " + category);
		}
		return list;
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

	public SiteSourceDTO getSite(Long id) {
		Optional<SiteSourceJob> byId = siteSourceJobRepository.findById(id);
		SiteSourceJob siteSourceJob = byId.orElseThrow(() -> new ResourceNotFoundException("not found site with id " + id));
		return mapper.map(siteSourceJob, SiteSourceDTO.class);
	}

	public CategoryDTO getCategory(Long id, Long cId) {
		Optional<Category> byIdAndSourceId = categoryRepository.findByIdAndSourceId(cId, id);
		Category category = byIdAndSourceId.orElseThrow(() ->
				new ResourceNotFoundException(String.format("not found category by category %s and source %s", cId, id)));
		return mapper.map(category, CategoryDTO.class);
	}

	public SubCategoryDTO getSubCategory(Long cId, Long sId) {
		Optional<Subcategory> byIdAndCategoryId = subCategoryRepository.findByIdAndCategoryId(sId, cId);
		Subcategory subcategory = byIdAndCategoryId.orElseThrow(() ->
				new ResourceNotFoundException(String.format("not found sub category by sub category %s and category %s", sId, cId)));

		return mapper.map(subcategory, SubCategoryDTO.class);
	}

	public void subcribeOnSource(Long categoryId, Long subCategoryId, boolean cValue, boolean sValue) {
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
	}

	public List<OrderDTO> getOrdersBySource(Long source, Long category, Long subcategory, Long period) {

		LocalDateTime ldt = LocalDateTime.now().minusDays(period);
		Date date = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
		Set<Order> orders;
		if (Objects.isNull(subcategory)) {
			orders = orderRepository.findAllBySourceSubCategoryIsNullOneEager(source, category, date);
		}else {
			orders = orderRepository.findAllBySourceOneEager(source, category, subcategory, date);
		}

		List<OrderDTO> list = orders.stream()
				.map(order -> {
					OrderDTO dto = mapper.map(order, OrderDTO.class);
					SourceSiteDTO s = dto.getSourceSite();

					String sourceName = siteSourceJobRepository.findById(s.getSource())
							.orElseThrow(() -> new ResourceNotFoundException("don't found by source id " + s.getSource()))
							.getName();
					s.setSourceName(sourceName);

					String categoryName = categoryRepository.findById(s.getCategory())
							.orElseThrow(() -> new ResourceNotFoundException("don't found by category id " + s.getCategory()))
							.getNativeLocName();
					s.setCategoryName(categoryName);

					if (Objects.nonNull(s.getSubCategory())) {
						String subCategoryName = subCategoryRepository.findById(s.getSubCategory())
								.orElseThrow(() -> new ResourceNotFoundException("don't found by subcategory id " + s.getSubCategory()))
								.getNativeLocName();
						s.setSubCategoryName(subCategoryName);
					}
					dto.setSourceSite(s);
					return dto;
				})
				.toList();

		return list;
	}
}