package by.gdev.test.service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import by.gdev.test.service.config.ApplicationProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
@Slf4j
public class AppHandler {

	private final ApplicationProperty property;

	public Mono<ServerResponse> message(ServerRequest request) {
		return ServerResponse.ok().bodyValue("test " + property.getTest());
	}

	public Mono<ServerResponse> secureMessage(ServerRequest request) {
		log.info("headers " + request.headers().toString());
		return ServerResponse.ok().bodyValue(property.getTest() + " **** secure");
	}
}
