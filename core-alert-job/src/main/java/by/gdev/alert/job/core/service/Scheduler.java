package by.gdev.alert.job.core.service;

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

import by.gdev.alert.job.core.model.AppUser;
import by.gdev.alert.job.core.model.UserFilter;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.common.model.Order;
import by.gdev.common.model.Price;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class Scheduler implements ApplicationListener<ContextRefreshedEvent>{
	
	private final WebClient webClient;
	
	private final AppUserRepository userRepository;
	
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
//		sseConnection();
	}
	
	public void sseConnection() {
		ParameterizedTypeReference<ServerSentEvent<List<Order>>> type = new ParameterizedTypeReference<ServerSentEvent<List<Order>>>() {
		};
		Flux<ServerSentEvent<List<Order>>> sseEvents = webClient.get().uri("http://core-alert-job:8017/api/stream-sse")
				.accept(MediaType.TEXT_EVENT_STREAM).retrieve().bodyToFlux(type)
				.doOnNext(e -> System.out.println(e.data().size()));
		sseEvents.subscribe(event -> {

		});
	}

	
	public void forEachOrders(List<AppUser> users, List<Order> orders) {
		users.forEach(user -> {
			List<Order> suitableOrders = orders.stream().filter(f -> isMatchUserFilter(user, f)).collect(Collectors.toList());
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