package by.gdev.alert.job.parser.repository;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.OrderLinks;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface OrderLinksRepository extends CrudRepository<OrderLinks, Long> {

	/**
	 * Проверка существования ссылки с учётом {@code subCategory == null}
	 * (категории без подкатегорий). Spring Data derived query некорректно обрабатывает null.
	 */
	@Query("SELECT CASE WHEN COUNT(ol) > 0 THEN TRUE ELSE FALSE END FROM OrderLinks ol " +
			"WHERE ol.category = :category AND ol.links = :links " +
			"AND ((:subCategory IS NULL AND ol.subCategory IS NULL) OR ol.subCategory = :subCategory)")
	boolean existsByCategoryAndSubCategoryAndLinks(
			@Param("category") Category category,
			@Param("subCategory") Subcategory subCategory,
			@Param("links") String links);

	/**
	 * Удаляет запись из {@code order_links} при cleanup заказа, чтобы не оставлять
	 * «сиротские» ссылки и не получать UK при следующем парсинге.
	 */
	@Modifying
	@Query("DELETE FROM OrderLinks ol WHERE ol.links = :link AND ol.category.id = :categoryId " +
			"AND ((:subCategoryId IS NULL AND ol.subCategory IS NULL) OR ol.subCategory.id = :subCategoryId)")
	void deleteByLinksAndCategoryIdAndSubCategoryId(
			@Param("link") String link,
			@Param("categoryId") Long categoryId,
			@Param("subCategoryId") Long subCategoryId);
}
