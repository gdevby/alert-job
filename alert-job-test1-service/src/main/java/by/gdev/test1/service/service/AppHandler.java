package by.gdev.test1.service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
@Slf4j
public class AppHandler {

	private final WebClient.Builder loadBalancedWebClientBuilder;

	public Mono<ServerResponse> testBalancer(ServerRequest request) {
		return ServerResponse.ok().body(loadBalancedWebClientBuilder.build().get().uri("http://alert-job-test-service/public/message")
				.retrieve().bodyToMono(String.class), String.class);
	}
}
