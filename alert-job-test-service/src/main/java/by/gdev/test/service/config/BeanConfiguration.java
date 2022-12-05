package by.gdev.test.service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import by.gdev.test.service.service.AppHandler;

@Configuration
@EnableScheduling
public class BeanConfiguration {
	@Bean
	ApplicationProperty applicationPropertyCreate() {
		return new ApplicationProperty();
	};

	@Bean
	RouterFunction<ServerResponse> routs(AppHandler handler) {
		return RouterFunctions.route().GET("/public/message", handler::message)
				.GET("/secure/message", handler::secureMessage).build();
	}

	@Bean
	@LoadBalanced
	public WebClient.Builder lbWebClient() {
		return WebClient.builder();
	}

}
