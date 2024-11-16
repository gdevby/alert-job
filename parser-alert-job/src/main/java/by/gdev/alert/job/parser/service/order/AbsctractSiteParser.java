package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.factory.RestTemplateFactory;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.common.model.OrderDTO;
import jakarta.xml.bind.UnmarshalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public abstract class AbsctractSiteParser implements SiteParser{
	@Value("${delay.reply.request}")
	private long delayReplyRequest;

	private static final int ATTEMPTS_COUNT = 3;

	@Autowired
	private SiteSourceJobRepository siteSourceJobRepository;

	@Autowired
	private RestTemplateFactory restTemplateFactory;

	@Transactional
	public List<OrderDTO> parse(){
		return getOrders(getSiteName().getId());
	};

	public List<OrderDTO> getOrders(Long siteId) {
		Exception ex = null;
		List<OrderDTO> orders = new ArrayList<>();
		for (int i = 0; i < ATTEMPTS_COUNT; i++) {
			try {
				SiteSourceJob siteSourceJob = siteSourceJobRepository.findById(siteId).get();
				log.trace("parsed {}", siteSourceJob.getName());
				siteSourceJob.getCategories().stream()
						// parse only categories that can parse=true
						// iterate over each category from this collection
						.forEach(category -> {

							List<Subcategory> siteSubCategories = category.getSubCategories();
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

	protected RestTemplate getRestTemplate(boolean isProxyNeeded){
		return restTemplateFactory.getRestTemplate(isProxyNeeded);
	}

	protected abstract List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category c, Subcategory sub);
}
