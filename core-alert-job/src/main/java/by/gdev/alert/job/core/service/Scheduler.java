package by.gdev.alert.job.core.service;

import java.util.List;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.common.model.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class Scheduler {

	private final WebClient webClient;
	private final AppUserRepository userRepository;
	private final OrderProcessor orderProcessor;

    private static final int period = 300;  //период обновления в секундах
    private static final int millsCoeff = 1000;

	@Scheduled(fixedDelay = period * millsCoeff)
	public void sseConnection() {
		log.trace("send request for parsing");
		ParameterizedTypeReference<List<OrderDTO>> type = new ParameterizedTypeReference<List<OrderDTO>>() {
		};
		List<OrderDTO> sseConection = webClient.get().uri("http://parser:8017/api/stream-orders").retrieve()
				.bodyToMono(type).block();
		try {
			log.trace("got elements size {}", sseConection.size());
			Set<AppUser> users = userRepository.findAllUsersEagerOrderModules();
			orderProcessor.forEachOrders(users, sseConection);
			log.trace("finished process elements {}", sseConection.size());
		} catch (Throwable ex) {
			log.error("problem with subscribe", ex);
		}
	}

}