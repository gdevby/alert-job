package by.gdev.alert.job.core.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.common.collect.Lists;

import by.gdev.alert.job.core.model.AppUser;
import by.gdev.alert.job.core.model.UserFilter;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.common.model.Order;
import by.gdev.common.model.Price;
import by.gdev.common.model.SourceSiteDTO;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class Scheduler implements ApplicationListener<ContextRefreshedEvent>{

	private final WebClient webClient;
	private final StatisticService statisticService;
	private final AppUserRepository userRepository;
	
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
//		sseConnection();
	}
	
	public void sseConnection() {
		ParameterizedTypeReference<ServerSentEvent<List<Order>>> type = new ParameterizedTypeReference<ServerSentEvent<List<Order>>>() {
		};
		Flux<ServerSentEvent<List<Order>>> sseEvents = webClient.get().uri("http://core-alert-job:8017/api/stream-sse")
				.accept(MediaType.TEXT_EVENT_STREAM).retrieve().bodyToFlux(type);
		sseEvents.subscribe(event -> {
			List<AppUser> users = Lists.newArrayList(userRepository.findAll());
			forEachOrders(users, event.data());
		});
	}

	
	public void forEachOrders(List<AppUser> users, List<Order> orders) {
		users.forEach(user -> {
			user.getSources().forEach(s -> {
				List<Order> list = orders.stream()
						.peek(p -> {
							statisticService.statisticTitleWord(p.getTitle());
							statisticService.statisticDescriptionWord(p.getMessage());
							statisticService.statisticTechnologyWord(p.getTechnologies());
						})
						.filter(f -> {
					SourceSiteDTO source = f.getSourceSite();
					return s.getSiteSource() == source.getSiteSource()
							&& s.getSiteCategory() == source.getSiteCategory()
							&& s.getSiteSubCategory() == source.getSiteSubCategory();
				}).collect(Collectors.toList());
				List<String> messages = list.stream().filter(f1 -> isMatchUserFilter(user, f1))
						.map(e -> String.format("New order - %s \n %s", e.getTitle(), e.getLink()))
						.collect(Collectors.toList());
				String sendMessage = StringUtils.isNotEmpty(user.getEmail()) ? "http://notification-alert-job:8019/mail"
						: "http://notification-alert-job:8019/telegram";
				UserNotification un = StringUtils.isNotEmpty(user.getEmail()) ? new UserNotification(user.getEmail(), null)
						: new UserNotification(String.valueOf(user.getTelegram()), null);
				messages.forEach(message -> {
					un.setMessage(message);
					Mono<Void> mono = webClient.post().uri(sendMessage).bodyValue(un).retrieve().bodyToMono(Void.class);
					mono.subscribe();
				});
			});

		});
	}
	
	private boolean isMatchUserFilter(AppUser user, Order order) {
		UserFilter userFilter = user.getCurrentFilter();
		Price price = order.getPrice();
		boolean isMinValue = Objects.nonNull(userFilter.getMinValue()) ? userFilter.getMinValue() <= price.getValue()
				: true;
		boolean isMaxValue = Objects.nonNull(userFilter.getMaxValue()) ? price.getValue() <= userFilter.getMaxValue()
				:true;
		boolean isContainsTitle = CollectionUtils.isEmpty(userFilter.getTitles())
				? userFilter.getTitles().stream().anyMatch(e -> order.getTitle().contains(e.getName()))
				: true;
		boolean isContainsDesc = CollectionUtils.isEmpty(userFilter.getDescriptions())
				? userFilter.getDescriptions().stream().anyMatch(e -> order.getMessage().contains(e.getName()))
				: true;
		boolean isContainsTech = CollectionUtils.isEmpty(userFilter.getTechnologies())
				? userFilter.getTechnologies().stream().anyMatch(e -> order.getTechnologies().contains(e.getName()))
				: true;
			return isMinValue && isMaxValue && isContainsTitle && isContainsDesc && isContainsTech;
	}
}