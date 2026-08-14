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
import java.util.*;

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
                .sorted(Comparator.comparing(
                        CategoryDTO::getOrder,
                        Comparator.nullsLast(Integer::compareTo)
                ))
				.toList();
		if (list.isEmpty()) {
			throw new ResourceNotFoundException("not found category with source id " + id);
		}
		return list;
	}

	public List<SubCategoryDTO> getSubCategories(Long categoryId) {
		return subCategoryRepository.findAllByCategoryId(categoryId).stream()
				.map(el -> mapper.map(el, SubCategoryDTO.class))
				.sorted(Comparator.comparing(
						SubCategoryDTO::getOrder,
						Comparator.nullsLast(Integer::compareTo)
				))
				.toList();
	}

	/**
	 * Проверяет, нужно ли сохранять заказ для данной категории/подкатегории.
	 * Возвращает {@code true}, если ссылку ещё можно обрабатывать в этом контексте.
	 * <p>
	 * Один и тот же {@code link} может встретиться в разных категориях — проверка
	 * всегда в рамках пары {@code (category, subCategory)}, без глобального отсечения по link.
	 * <ul>
	 *   <li>{@code order_links} — UK {@code (category_id, sub_category_id, links)}</li>
	 *   <li>{@code parser_order} — уже сохранён для этой же категории/подкатегории</li>
	 * </ul>
	 */
	public boolean shouldSaveOrder(Category category, Subcategory subCategory, String link) {
		Long subCategoryId = subCategory != null ? subCategory.getId() : null;
		return !linkRepository.existsByCategoryAndSubCategoryAndLinks(category, subCategory, link)
				&& !orderRepository.existsByLinkCategoryAndSubCategory(link, category.getId(), subCategoryId);
	}

	/**
	 * @deprecated не используется; дедупликация — через {@link #shouldSaveOrder(Category, Subcategory, String)}.
	 */
	@Deprecated
	public boolean shouldSaveOrder(String link) {
		return !orderRepository.existsByLink(link);
	}

	/**
	 * Сохраняет ссылку в {@code order_links}. Идемпотентен: повторный вызов с тем же
	 * {@code (category, subCategory, link)} не создаёт дубль и не бросает UK-ошибку.
	 */
	public void saveOrderLinks(Category category, Subcategory subCategory, String link) {
		if (linkRepository.existsByCategoryAndSubCategoryAndLinks(category, subCategory, link)) {
			return;
		}
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