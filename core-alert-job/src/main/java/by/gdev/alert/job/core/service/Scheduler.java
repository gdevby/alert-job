package by.gdev.alert.job.core.service;

import java.util.List;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.common.model.Order;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class Scheduler implements ApplicationListener<ContextRefreshedEvent>{
	
	private final WebClient webClient;
	
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
	
	
	

}