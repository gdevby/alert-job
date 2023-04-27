package by.gdev.alert.job.core.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
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
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
@Slf4j
public class Scheduler implements ApplicationListener<ContextRefreshedEvent> {

    private final WebClient webClient;
    private final StatisticService statisticService;
    private final AppUserRepository userRepository;
    private final DelayOrderNotificationRepository delayOrderRepository;

    private final ApplicationProperty property;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
	sseConnection();
    }

    public void sseConnection() {
	ParameterizedTypeReference<ServerSentEvent<List<OrderDTO>>> type = new ParameterizedTypeReference<ServerSentEvent<List<OrderDTO>>>() {
	};
	Flux<ServerSentEvent<List<OrderDTO>>> sseConection = webClient.get().uri("http://parser:8017/api/stream-sse")
		.accept(MediaType.TEXT_EVENT_STREAM).retrieve().bodyToFlux(type)
		.doOnSubscribe(s -> log.info("trying subscribe"))
		.retryWhen(Retry.backoff(Integer.MAX_VALUE, Duration.ofSeconds(30)));
	sseConection.subscribe(event -> {
	    try {
		log.trace("got elements by subscription {} size {}", event.event(), event.data().size());
		Set<AppUser> users = userRepository.findAllUsersEagerOrderModules();
		forEachOrders(users, event.data());
	    } catch (Throwable ex) {
		log.error("problem with subscribe", ex);
	    }
	}, error -> log.warn("failed to get orders from parser {}", error));

    }

    private void forEachOrders(Set<AppUser> users, List<OrderDTO> orders) {
	users.forEach(user -> {
	    user.getOrderModules().stream().filter(e -> Objects.nonNull(e.getCurrentFilter())).forEach(o -> {
		o.getSources().forEach(s -> {
		    List<OrderDTO> list = orders.stream().peek(p -> {
			statisticService.statisticTitleWord(p.getTitle(), p.getSourceSite());
			statisticService.statisticTechnologyWord(p.getTechnologies(), p.getSourceSite());
		    }).filter(f -> compareSiteSources(f.getSourceSite(), s))
			    .filter(f -> isMatchUserFilter(f, o.getCurrentFilter())).collect(Collectors.toList());
		    if (!list.isEmpty()) {
			sendOrderToUser(user, list, o.getName());

		    }
		});
	    });
	});
    }

    private void sendOrderToUser(AppUser user, List<OrderDTO> list, String orderName) {
	if (CollectionUtils.isEmpty(user.getUserAlertTimes()) || isMatchUserAlertTimes(user)) {
	    List<String> orders = list.stream()
		    .map(e -> String.format("%s, новый заказ - %s \n %s", orderName, e.getTitle(), e.getLink()))
		    .toList();
	    sendMessageToUser(user, orders);
	} else {
	    list.forEach(l -> {
		DelayOrderNotification don = new DelayOrderNotification();
		don.setUser(user);
		don.setLink(l.getLink());
		don.setTitle(l.getTitle());
		don.setOrderName(orderName);
		delayOrderRepository.save(don);
	    });
	}
    }

    private void sendMessageToUser(AppUser user, List<String> list) {
	String sendMessage = user.isDefaultSendType() ? "http://notification:8019/mail"
		: "http://notification:8019/telegram";
	UserNotification un = user.isDefaultSendType() ? new UserNotification(user.getEmail(), null)
		: new UserNotification(String.valueOf(user.getTelegram()), null);
	un.setMessage(list.stream().collect(Collectors.joining(", ")));
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
	    if (openForAll == false)
		return false;
	}
	if (Objects.nonNull(order.getPrice())) {
	    if (Objects.nonNull(userFilter.getMinValue()))
		minValue = userFilter.getMinValue() <= order.getPrice().getValue();
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
	if (CollectionUtils.isEmpty(userFilter.getTitles()) && CollectionUtils.isEmpty(userFilter.getDescriptions())
		&& CollectionUtils.isEmpty(userFilter.getTechnologies()))
	    return true;
	if (!CollectionUtils.isEmpty(userFilter.getTitles())) {
	    containsTitle = userFilter.getTitles().stream()
		    .anyMatch(e -> StringUtils.containsIgnoreCase(order.getTitle(), e.getName()));
	}
	if (!CollectionUtils.isEmpty(userFilter.getTechnologies()) && Objects.nonNull(order.getTechnologies())) {
	    containsTech = userFilter.getTechnologies().stream().anyMatch(e -> order.getTechnologies().stream()
		    .map(String::toLowerCase).toList().contains(e.getName().toLowerCase()));
	}
	if (!CollectionUtils.isEmpty(userFilter.getDescriptions())) {
	    containsDesc = userFilter.getDescriptions().stream()
		    .anyMatch(e -> StringUtils.containsIgnoreCase(order.getMessage(), e.getName()));
	}
	if (containsTitle || containsDesc || containsTech) {
	    log.trace("some positive is true title {} desc {} tech {} {}", containsTitle, containsDesc, containsTech,
		    order.getLink());
	    if (userFilter.isActivatedNegativeFilters()) {
		if (!CollectionUtils.isEmpty(userFilter.getNegativeDescriptions()))
		    containsDesc1 = userFilter.getNegativeDescriptions().stream()
			    .anyMatch(e -> StringUtils.containsIgnoreCase(order.getMessage(), e.getName()));

		if (!CollectionUtils.isEmpty(userFilter.getNegativeTechnologies())
			&& Objects.nonNull(order.getTechnologies()))
		    containsTech1 = userFilter.getNegativeTechnologies().stream().anyMatch(e -> order.getTechnologies()
			    .stream().map(String::toLowerCase).toList().contains(e.getName().toLowerCase()));

		if (!CollectionUtils.isEmpty(userFilter.getNegativeTitles())) {
		    containsTitle1 = userFilter.getNegativeTitles().stream()
			    .anyMatch(e -> StringUtils.containsIgnoreCase(order.getTitle(), e.getName()));
		}
		log.trace("negative filter title {} desc {} tech {} {}", containsTitle1, containsDesc1, containsTech1,
			order.getLink());
		return !(containsDesc1 || containsTech1 || containsTitle1);
	    } else
		return true;
	}
	return false;
    }

    @Scheduled(cron = "0 0/10 * * * *")
    public void sendDelayOrders() {
	userRepository.findAllOneEagerUserAlertTimes().forEach(user -> {
	    if (isMatchUserAlertTimes(user)) {
		List<String> orders = user.getDelayOrderNotifications().stream().map(
			e -> String.format("%s, новый заказ - %s \n %s", e.getOrderName(), e.getTitle(), e.getLink()))
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
}