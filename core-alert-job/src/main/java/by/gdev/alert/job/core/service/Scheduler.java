package by.gdev.alert.job.core.service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		sseConnection();
	}

	public void sseConnection() {
		ParameterizedTypeReference<ServerSentEvent<List<OrderDTO>>> type = new ParameterizedTypeReference<ServerSentEvent<List<OrderDTO>>>() {
		};
		Flux<ServerSentEvent<List<OrderDTO>>> sseConection = webClient.get().uri("http://parser:8017/api/stream-sse")
				.accept(MediaType.TEXT_EVENT_STREAM).retrieve().bodyToFlux(type)
				.doOnSubscribe(s -> log.info("subscribed on orders"))
				.retryWhen(Retry.backoff(Integer.MAX_VALUE, Duration.ofSeconds(2)));
		sseConection.subscribe(event -> {
			log.trace("got elements by subscription {} size {}", event.event(), event.data().size());
			Set<AppUser> users = userRepository.findAllUsersEagerOrderModules();
			forEachOrders(users, event.data());
		}, error -> log.warn("failed to get orders from parser {}", error));
	}

	private void forEachOrders(Set<AppUser> users, List<OrderDTO> orders) {
		users.forEach(user -> {
			user.getOrderModules().stream().filter(e -> Objects.nonNull(e.getCurrentFilter())).forEach(o -> {
				o.getSources().forEach(s -> {
					List<String> list = orders.stream().peek(p -> {
						statisticService.statisticTitleWord(p.getTitle());
						statisticService.statisticDescriptionWord(p.getMessage());
						statisticService.statisticTechnologyWord(p.getTechnologies());
					}).filter(f -> compareSiteSources(f.getSourceSite(), s))
							.filter(f -> isMatchUserFilter(f, o.getCurrentFilter()))
							.map(e -> String.format("Новый заказ - %s \n %s", e.getTitle(), e.getLink()))
							.collect(Collectors.toList());
					String sendMessage = user.isDefaultSendType() ? "http://notification:8019/mail"
							: "http://notification:8019/telegram";
					if (!list.isEmpty()) {
						UserNotification un = user.isDefaultSendType() ? new UserNotification(user.getEmail(), null)
								: new UserNotification(String.valueOf(user.getTelegram()), null);
						un.setMessage(list.stream().collect(Collectors.joining(", ")));
						Mono<Void> mono = webClient.post().uri(sendMessage).bodyValue(un).retrieve()
								.bodyToMono(Void.class);
						mono.subscribe(
								e -> log.debug("sent new order for user by mail: {}, to {}", user.isDefaultSendType(),
										un.getToMail()),
								e -> log.debug("failed to sent user's message by mail: {}, to {}",
										user.isDefaultSendType(), un.getToMail()));
					}
				});
			});
		});
	}

	private boolean compareSiteSources(SourceSiteDTO orderSource, SourceSite userSource) {
		return userSource.getSiteSource().equals(orderSource.getSource())
				&& userSource.getSiteCategory().equals(orderSource.getCategory())
				&& Objects.equals(userSource.getSiteSubCategory(), orderSource.getSubCategory());
	}

	public boolean isMatchUserFilter(OrderDTO order, UserFilter userFilter) {
		boolean minValue = true, maxValue = true, containsTitle = true, containsTech = true, containsDesc = true;
		StringBuilder sb = new StringBuilder("order comparing ");
		if (Objects.nonNull(order.getPrice())) {
			if (Objects.nonNull(userFilter.getMinValue())) {
				minValue = userFilter.getMinValue() <= order.getPrice().getValue();
				sb.append("min val ").append(minValue).append(" ");
			}
			if (Objects.nonNull(userFilter.getMaxValue())) {
				maxValue = userFilter.getMaxValue() >= order.getPrice().getValue();
				sb.append("max val ").append(minValue).append(" ");
			}
		}
		if (!CollectionUtils.isEmpty(userFilter.getTitles())) {
			containsTitle = userFilter.getTitles().stream().anyMatch(e -> order.getTitle().contains(e.getName()));
			sb.append("title ").append(containsTitle).append(" ");
		}
		if (!CollectionUtils.isEmpty(userFilter.getTechnologies())) {
			containsTech = userFilter.getTechnologies().stream()
					.anyMatch(e -> order.getTechnologies().contains(e.getName()));
			sb.append("tech ").append(containsTech).append(" ");
		}
		if (!CollectionUtils.isEmpty(userFilter.getDescriptions())) {
			containsDesc = userFilter.getDescriptions().stream()
					.anyMatch(e -> order.getMessage().contains(e.getName()));
			sb.append("desc ").append(containsDesc).append(" ");
		}
		
		if (userFilter.isActivatedNegativeFilters()) {
			if (!CollectionUtils.isEmpty(userFilter.getNegativeDescriptions()) && containsDesc)
				containsDesc = userFilter.getNegativeDescriptions().stream()
						.anyMatch(e -> !order.getMessage().contains(e.getName()));
			
			if (!CollectionUtils.isEmpty(userFilter.getNegativeTechnologies()) && containsTech)
				containsTech = userFilter.getNegativeTechnologies().stream()
						.anyMatch(e -> !order.getTechnologies().contains(e.getName()));
			
			if (!CollectionUtils.isEmpty(userFilter.getNegativeTitles()) && containsTitle) {
				containsTitle = userFilter.getNegativeTitles().stream().anyMatch(e -> !order.getTitle().contains(e.getName()));
			}
		}
		sb.append(order.getLink()).append(" ").append(order.getTitle());
		log.debug(sb.toString());
		return minValue && maxValue && containsTitle && containsDesc && containsTech;
	}
}