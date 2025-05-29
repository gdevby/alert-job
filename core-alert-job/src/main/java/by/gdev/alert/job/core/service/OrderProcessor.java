package by.gdev.alert.job.core.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.core.configuration.ApplicationProperty;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.DelayOrderNotification;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.DelayOrderNotificationRepository;
import by.gdev.alert.job.core.repository.UserFilterRepository;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessor {
	private final WebClient webClient;
	private final StatisticService statisticService;
	private final AppUserRepository userRepository;
	private final DelayOrderNotificationRepository delayOrderRepository;
	private final ApplicationProperty property;
	private final UserFilterRepository filterRepository;

	public void forEachOrders(Set<AppUser> users, List<OrderDTO> orders) {
		orders.forEach(orderDTO -> statisticService.statisticTitleWord(orderDTO.getTitle(), orderDTO.getSourceSite()));
		users.forEach(user -> {
			user.getOrderModules().stream().filter(orderModule -> Objects.nonNull(orderModule.getCurrentFilter()))
					.forEach(orderModule -> {
						UserFilter currentFilter = filterRepository
								.findByIdEagerAllWords(orderModule.getCurrentFilter().getId());
						orderModule.getSources().forEach(s -> {
							List<OrderDTO> list = orders.stream().peek(orderDTO -> {
							}).filter(f -> compareSiteSources(f.getSourceSite(), s))
									.filter(f -> isMatchUserFilter(f, currentFilter)).collect(Collectors.toList());
							if (!list.isEmpty()) {
								sendOrderToUser(user, list, orderModule.getName());
							}
						});
					});
		});
	}

	private void sendOrderToUser(AppUser user, List<OrderDTO> list, String orderName) {
		if (CollectionUtils.isEmpty(user.getUserAlertTimes()) || isMatchUserAlertTimes(user)) {
			List<String> orders = list.stream().map(e -> {
				SourceSiteDTO s = e.getSourceSite();
				return createOrdersMessage(orderName, e.getTitle(), e.getLink(), s.getCategoryName(),
						s.getSubCategoryName());
			}).toList();
			sendMessageToUser(user, orders);
		} else {
			list.forEach(l -> {
				SourceSiteDTO s = l.getSourceSite();
				DelayOrderNotification don = new DelayOrderNotification();
				don.setUser(user);
				don.setLink(l.getLink());
				don.setTitle(l.getTitle());
				don.setOrderName(orderName);
				don.setCategoryName(s.getCategoryName());
				if (!StringUtils.isEmpty(s.getSubCategoryName())) {
					don.setSubCategoryName(s.getSubCategoryName());
				}
				delayOrderRepository.save(don);
			});
		}
	}

	private void sendMessageToUser(AppUser user, List<String> list) {
		String sendMessage = user.isDefaultSendType() ? "http://notification:8019/mail"
				: "http://notification:8019/telegram";
		StringBuilder b = new StringBuilder();
		for (String s : list) {
			b.append(s).append(", ");
			if (b.length() > 3000) {
				sendMessage(user, sendMessage, b.substring(0, b.length() - 2));
				b.setLength(0);
			}
		}
		if (b.length() != 0) {
			sendMessage(user, sendMessage, b.substring(0, b.length() - 2));
		}
	}

	private void sendMessage(AppUser user, String sendMessage, String message) {
		UserNotification un = user.isDefaultSendType() ? new UserNotification(user.getEmail(), message)
				: new UserNotification(String.valueOf(user.getTelegram()), message);
		Mono<Void> mono = webClient.post().uri(sendMessage).bodyValue(un).retrieve().bodyToMono(Void.class);
		mono.subscribe(
				e -> log.debug("sent new order for user by mail: {}, to {}", user.isDefaultSendType(), un.getToMail()),
				e -> log.debug("failed to sent user's message by mail: {}, to {} {}", user.isDefaultSendType(),
						un.getToMail(), user.getUuid()));
	}

	private boolean compareSiteSources(SourceSiteDTO orderSource, SourceSite userSource) {
		return userSource.getSiteSource().equals(orderSource.getSource())
				&& userSource.getSiteCategory().equals(orderSource.getCategory())
				&& Objects.equals(userSource.getSiteSubCategory(), orderSource.getSubCategory());
	}

	public boolean isMatchUserFilter(OrderDTO order, UserFilter userFilter) {
		boolean minValue = true, maxValue = true, containsTitle = false, containsTech = false, containsDesc = false,
				containsTitle1 = false, containsTech1 = false, containsDesc1 = false;
		if (property.getSitesOpenForAll().contains(order.getSourceSite().getSource()) && userFilter.isOpenForAll()) {
			boolean openForAll = order.isOpenForAll() == userFilter.isOpenForAll();
			if (openForAll == false) {
				return false;
			}
		}
		if (Objects.nonNull(order.getPrice())) {
			if (Objects.nonNull(userFilter.getMinValue())) {
				minValue = userFilter.getMinValue() <= order.getPrice().getValue();
			}
			if (Objects.nonNull(userFilter.getMaxValue()) && userFilter.getMaxValue() != 0) {
				maxValue = userFilter.getMaxValue() >= order.getPrice().getValue();
			}
			if ((!minValue || !maxValue) && !CollectionUtils.isEmpty(userFilter.getDescriptionWordPrice())) {
				boolean dwp = userFilter.getDescriptionWordPrice().stream()
						.anyMatch(e -> StringUtils.containsIgnoreCase(order.getMessage(), e.getName()));
				if (dwp) {
					maxValue = true;
					minValue = true;
				}
			}
			if (!(minValue && maxValue)) {
				log.trace("ignore by min max {} {} {}", minValue, maxValue, order.getLink());
				return false;
			}
		}
		if (CollectionUtils.isEmpty(userFilter.getTitles()) && CollectionUtils.isEmpty(userFilter.getDescriptions())) {
			return true;
		}
		if (!CollectionUtils.isEmpty(userFilter.getTitles())) {
			containsTitle = userFilter.getTitles().stream()
					.anyMatch(e -> StringUtils.containsIgnoreCase(order.getTitle(), e.getName()));
		}
		if (!CollectionUtils.isEmpty(userFilter.getDescriptions())) {
			containsDesc = userFilter.getDescriptions().stream()
					.anyMatch(e -> StringUtils.containsIgnoreCase(order.getMessage(), e.getName()));
		}
		if (containsTitle || containsDesc || containsTech) {
			log.trace("some positive is true title {} desc {} tech {} {}", containsTitle, containsDesc, containsTech,
					order.getLink());
			if (userFilter.isActivatedNegativeFilters()) {
				if (!CollectionUtils.isEmpty(userFilter.getNegativeDescriptions())) {
					containsDesc1 = userFilter.getNegativeDescriptions().stream()
							.anyMatch(e -> StringUtils.containsIgnoreCase(order.getMessage(), e.getName()));
				}

				if (!CollectionUtils.isEmpty(userFilter.getNegativeTitles())) {
					containsTitle1 = userFilter.getNegativeTitles().stream()
							.anyMatch(e -> StringUtils.containsIgnoreCase(order.getTitle(), e.getName()));
				}
				log.trace("negative filter title {} desc {} tech {} {}", containsTitle1, containsDesc1, containsTech1,
						order.getLink());
				return !(containsDesc1 || containsTech1 || containsTitle1);
			} else {
				return true;
			}
		}
		return false;
	}

	@Scheduled(cron = "0 0/10 * * * *")
	public void sendDelayOrders() {
		userRepository.findAllOneEagerUserAlertTimes().forEach(user -> {
			boolean isMatchAlertdate = isMatchUserAlertTimes(user);
			if (isMatchAlertdate) {
				List<String> orders = user.getDelayOrderNotifications().stream()
						.map(e -> createOrdersMessage(e.getOrderName(), e.getTitle(), e.getLink(), e.getCategoryName(),
								e.getSubCategoryName()))
						.toList();
				if (!orders.isEmpty()) {
					sendMessageToUser(user, orders);
					delayOrderRepository.deleteAll(user.getDelayOrderNotifications());
				}
			}

		});
	}

	private boolean isMatchUserAlertTimes(AppUser user) {
		return user.getUserAlertTimes().stream().anyMatch(pr -> {
			LocalDateTime time = LocalDateTime.now(ZoneId.of(pr.getTimeZone()));
			Integer day = time.getDayOfWeek().getValue();
			Integer hour = time.getHour();
			return pr.getAlertDate().equals(day) && pr.getStartAlert() <= hour && hour < pr.getEndAlert();
		});
	}

	private String createOrdersMessage(String name, String title, String link, String categoryName,
			String subCategoryName) {
		return StringUtils.isNotEmpty(subCategoryName)
				? String.format("%s, %s, %s новый заказ - %s \n %s", name, categoryName, subCategoryName, title, link)
				: String.format("%s, %s, новый заказ - %s \n %s", name, categoryName, title, link);
	}
}
