package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.factory.RestTemplateFactory;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import jakarta.xml.bind.UnmarshalException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Базовый абстрактный парсер для всех сайтов‑источников.
 * Определяет общий жизненный цикл парсинга:
 *  - загрузка {@link SiteSourceJob} и связанных категорий;
 *  - обход категорий и подкатегорий;
 *  - вызов {@link #mapItems(String, Long, Category, Subcategory)} для получения заказов;
 *  - сохранение заказов и преобразование в {@link OrderDTO}.
 *
 * Реализации должны переопределить только метод {@link #mapItems(String, Long, Category, Subcategory)}.
 */
@Slf4j
public abstract class AbsctractSiteParser implements SiteParser{

	/**
	 * Задержка между повторными попытками запросов к сайту.
	 * Используется в механизме retry при сетевых ошибках.
	 */
	@Value("${delay.reply.request}")
	private long delayReplyRequest;

	/**
	 * Флаг активности парсера.
	 * Если false — парсер полностью отключён.
	 */
    protected boolean active;

	private static final int ATTEMPTS_COUNT = 3;

	@Getter
    @Autowired
	private SiteSourceJobRepository siteSourceJobRepository;

	@Autowired
	private RestTemplateFactory restTemplateFactory;

	@Autowired
	private ModelMapper mapper;

	@Getter
	@Autowired
	private ParserService parserService;

	@Getter
	@Autowired
	private CurrencyRepository currencyRepository;

	@Getter
	@Autowired
	private ParserSourceRepository parserSourceRepository;

	@Getter
	@Autowired
	private OrderRepository orderRepository;

	/**
	 * Точка входа для внешнего вызова.
	 * Запускает процесс парсинга для текущего сайта.
	 *
	 * @return список заказов в формате {@link OrderDTO}
	 */
	public List<OrderDTO> parse(){
		return getOrders(getSiteName().getId());
	};

	/**
	 * Основной цикл получения заказов:
	 *  - делает до {@code ATTEMPTS_COUNT} попыток (retry);
	 *  - загружает {@link SiteSourceJob} вместе с категориями и подкатегориями;
	 *  - обходит категории и подкатегории, отмеченные как {@code parse=true};
	 *  - вызывает {@link #mapItems(String, Long, Category, Subcategory)} для каждой категории/подкатегории;
	 *  - собирает все найденные заказы в общий список.
	 *
	 * Обрабатывает:
	 *  - ошибки XML ({@link jakarta.xml.bind.UnmarshalException});
	 *  - сетевые таймауты ({@link java.net.SocketTimeoutException});
	 *  - любые другие исключения, возникающие при парсинге.
	 *
	 * @param siteId ID сайта (значение из {@link by.gdev.alert.job.parser.util.SiteName})
	 * @return список заказов в формате {@link OrderDTO}
	 */
    public List<OrderDTO> getOrders(Long siteId) {
		Exception ex = null;
		List<OrderDTO> orders = new ArrayList<>();
		for (int i = 0; i < ATTEMPTS_COUNT; i++) {
			try {
                SiteSourceJob siteSourceJob = siteSourceJobRepository.findWithCategories(siteId);
				siteSourceJob.getCategories().stream()
						// parse only categories that can parse=true
						// iterate over each category from this collection
						.forEach(category -> {

							Set<Subcategory> siteSubCategories = category.getSubCategories();
							// checking if a subcategory exists for this category
							// category does't have a subcategory
							if (category.isParse()) {
								orders.addAll(mapItems(category.getLink(), siteSourceJob.getId(), category, null));
								log.trace("getting order by category {} rss link {}", category.getNativeLocName(),
										category.getLink());
							}
							// category have a subcategory
							siteSubCategories.stream()
									// parse only sub categories that can parse=true
									.filter(Subcategory::isParse)
									// Iterate all sub category
									.forEach(subCategory -> {
										log.trace("getting order by category {} and subcategory  {} {}",
												category.getNativeLocName(), subCategory.getNativeLocName(),
												subCategory.getLink());
										List<OrderDTO> list1 = mapItems(subCategory.getLink(), siteSourceJob.getId(),
												category, subCategory);
										orders.addAll(list1);
									});
						});
				return orders;
			} catch (Exception e) {
				if (e instanceof UnmarshalException && Objects.nonNull(e.getCause())
						&& (e.getCause().getMessage().contains("Server returned HTTP response code: 5")
								|| e.getCause().getMessage().contains("Server returned HTTP response code: 4")))
					log.warn("warn 500 error", e);
				else if (e instanceof SocketTimeoutException) {
					log.warn("warn", e);
				} else {
					ex = e;
				}
			}
			try {
				Thread.sleep(delayReplyRequest);
			} catch (InterruptedException e) {
			}
		}
		if (Objects.nonNull(ex)) {
			log.error("erorr", ex);
		}
		return orders;
	}

	/**
	 * Возвращает {@link RestTemplate} с учётом необходимости использования прокси.
	 *
	 * @param isProxyNeeded использовать ли прокси
	 * @return настроенный {@link RestTemplate}
	 */
	protected RestTemplate getRestTemplate(boolean isProxyNeeded){
		return restTemplateFactory.getRestTemplate(isProxyNeeded);
	}

	/**
	 * Сохраняет заказ в БД:
	 *  - сохраняет ссылку через {@link ParserService#saveOrderLinks(Category, Subcategory, String)};
	 *  - находит или создаёт {@link ParserSource};
	 *  - сохраняет {@link Order} в репозитории.
	 *
	 * @param order заказ
	 * @param category категория
	 * @param subCategory подкатегория
	 * @return сохранённый {@link Order}
	 */
	protected final Order saveOrder(Order order, Category category, Subcategory subCategory) {
		parserService.saveOrderLinks(category, subCategory, order.getLink());
		ParserSource ps = order.getSourceSite();
		ParserSource existing = parserSourceRepository
				.findBySourceAndCategoryAndSubCategory(ps.getSource(), ps.getCategory(), ps.getSubCategory())
				.orElseGet(() -> parserSourceRepository.save(ps));
		order.setSourceSite(existing);
		return orderRepository.save(order);
	}

	/**
	 * Преобразует {@link Order} в {@link OrderDTO} и добавляет человекочитаемые названия категорий.
	 *
	 * @param order заказ
	 * @param category категория
	 * @param subCategory подкатегория
	 * @return DTO заказа
	 */
	private OrderDTO getOrderData(Order order, Category category, Subcategory subCategory){
		OrderDTO dto = mapper.map(order, OrderDTO.class);
		SourceSiteDTO source = dto.getSourceSite();
		source.setCategoryName(category.getNativeLocName());
		if (subCategory != null)
			source.setSubCategoryName(subCategory.getNativeLocName());
		dto.setSourceSite(source);
		return dto;
	}

	/**
	 * Обрабатывает список заказов:
	 *  - фильтрует невалидные;
	 *  - проверяет уникальность через {@link ParserService#isExistsOrder(Category, Subcategory, String)};
	 *  - сохраняет в БД;
	 *  - преобразует в {@link OrderDTO}.
	 *
	 * @param orders список заказов
	 * @param category категория
	 * @param subCategory подкатегория
	 * @return список DTO
	 */
	protected List<OrderDTO> getOrdersData(List<Order> orders, Category category, Subcategory subCategory){
		return orders.stream()
				.filter(Objects::nonNull)
				.filter(Order::isValidOrder)
				.filter(order -> getParserService().isExistsOrder(category, subCategory, order.getLink()))
				.map(order -> saveOrder(order, category, subCategory))
				.peek(order -> {
					log.info("*** save order: site {},  title {} , link {}",
							getSiteName(),
							order.getTitle(),
							order.getLink());
				})
				.map(order -> getOrderData(order, category, subCategory))
				.toList();
	}

	/**
	 * Проверяет, активен ли парсер.
	 *
	 * @return true если активен
	 */
    public boolean isActive() {
        return active;
    }

	/**
	 * Абстрактный метод, который должен реализовать каждый конкретный парсер.
	 * Выполняет загрузку и разбор заказов для конкретной категории/подкатегории.
	 *
	 * @param link URL категории или подкатегории
	 * @param siteSourceJobId ID {@link SiteSourceJob}
	 * @param c категория
	 * @param sub подкатегория
	 * @return список заказов в формате {@link OrderDTO}
	 */
	protected abstract List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category c, Subcategory sub);
}
