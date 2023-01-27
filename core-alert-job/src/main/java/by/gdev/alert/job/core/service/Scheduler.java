package by.gdev.alert.job.core.service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.common.model.Order;
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

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		sseConnection();
	}

	public void sseConnection() {
		ParameterizedTypeReference<ServerSentEvent<List<Order>>> type = new ParameterizedTypeReference<ServerSentEvent<List<Order>>>() {
		};
		Flux<ServerSentEvent<List<Order>>> sseConection = webClient.get().uri("http://parser:8017/api/stream-sse")
				.accept(MediaType.TEXT_EVENT_STREAM).retrieve().bodyToFlux(type)
				.doOnSubscribe(s -> log.info("subscribed on orders"))
				.retryWhen(Retry.backoff(Integer.MAX_VALUE, Duration.ofSeconds(2)));
		sseConection.subscribe(event -> {
			log.trace("got elements by subscription {} size {}", event.event(), event.data().size());
			List<AppUser> users = userRepository.findAllUserEagerCurrentFilterAndSourceSite();
			forEachOrders(users, event.data());
		}, error -> log.warn("failed to get orders from parser {}", error.getMessage()));
	}

	public void forEachOrders(List<AppUser> users, List<Order> orders) {
		users.stream().filter(e -> Objects.nonNull(e.getCurrentFilter())).forEach(user -> {
			user.getSources().forEach(s -> {
				List<String> list = orders.stream().peek(p -> {
					statisticService.statisticTitleWord(p.getTitle());
					statisticService.statisticDescriptionWord(p.getMessage());
					statisticService.statisticTechnologyWord(p.getTechnologies());
				}).filter(f -> compareSiteSources(f.getSourceSite(), s)).filter(f -> isMatchUserFilter(user, f))
						.map(e -> String.format("Новый заказ - %s \n %s", e.getTitle(), e.getLink()))
						.collect(Collectors.toList());
				String sendMessage = user.isDefaultSendType() ? "http://notification:8019/mail"
						: "http://notification:8019/telegram";
				if (!list.isEmpty()) {
					UserNotification un = user.isDefaultSendType() ? new UserNotification(user.getEmail(), null)
							: new UserNotification(String.valueOf(user.getTelegram()), null);
					un.setMessage(list.stream().collect(Collectors.joining(", ")));
					Mono<Void> mono = webClient.post().uri(sendMessage).bodyValue(un).retrieve().bodyToMono(Void.class);
					mono.subscribe(
							e -> log.debug("sent new order for user by mail: {}, to {}", user.isDefaultSendType(),
									un.getToMail()),
							e -> log.debug("failed to sent user's message by mail: {}, to {}", user.isDefaultSendType(),
									un.getToMail()));
				}
			});
		});
	}

	// check for an empty subcategory, if the subcategory is empty, we compare only
	// by source and category, otherwise all fields are taken
	private boolean compareSiteSources(SourceSiteDTO orderSource, SourceSite userSource) {
		return (Objects.isNull(orderSource.getSubCategory()) || Objects.isNull(userSource.getSiteSubCategory()))
				? userSource.getSiteSource().equals(orderSource.getSource())
						&& userSource.getSiteCategory().equals(orderSource.getCategory())
				: userSource.getSiteSource().equals(orderSource.getSource())
						&& userSource.getSiteCategory().equals(orderSource.getCategory())
						&& userSource.getSiteSubCategory().equals(orderSource.getSubCategory());
	}

	private boolean isMatchUserFilter(AppUser user, Order order) {
		UserFilter userFilter = user.getCurrentFilter();
		boolean minValue = true, maxValue = true, containsTitle = true, containsDesc = true, containsTech = true;
		if (Objects.nonNull(order.getPrice())) {
			if (Objects.nonNull(userFilter.getMinValue()))
				minValue = userFilter.getMinValue() <= order.getPrice().getValue();
			if (Objects.nonNull(userFilter.getMaxValue()))
				maxValue = userFilter.getMaxValue() >= order.getPrice().getValue();
		}
		if (!CollectionUtils.isEmpty(userFilter.getTitles()))
			containsTitle = userFilter.getTitles().stream().anyMatch(e -> order.getTitle().contains(e.getName()));

		if (!CollectionUtils.isEmpty(userFilter.getDescriptions()))
			containsTitle = userFilter.getDescriptions().stream()
					.anyMatch(e -> order.getMessage().contains(e.getName()));

		if (!CollectionUtils.isEmpty(userFilter.getTechnologies()))
			containsTitle = userFilter.getTechnologies().stream()
					.anyMatch(e -> order.getTechnologies().contains(e.getName()));
		log.debug("filter value min price {}, max price {}, title words {}, technology {}, description {}", minValue,
				maxValue, containsTitle, containsTech, containsTech);
		return minValue && maxValue && containsTitle && containsDesc && containsTech;
	}
}