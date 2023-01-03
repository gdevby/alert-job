package by.gdev.alert.job.core.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.core.model.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CoreService {
	
	private final WebClient webClient;
	private final AppUserRepository userRepository;
	
	public Mono<Void> sendTestMessageOnMai(String uuid) {
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("user not found"));
			if (user.isDefaultSendType()) {
				UserNotification un = new UserNotification(user.getEmail(), "Test message from alerjob.by");
				webClient.post().uri("http://notification-alert-job:8019/mail").bodyValue(un).retrieve().bodyToMono(Void.class);
			}else {
				UserNotification un = new UserNotification(String.valueOf(user.getTelegram()), "Test message from alerjob.by");
				webClient.post().uri("http://notification-alert-job:8019/telegram").bodyValue(un).retrieve().bodyToMono(Void.class);
			}
			m.success();
		});
	}
	
	public Mono<Boolean> changeAlertStatus(String uuid, boolean status) {
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			user.setSwitchOffAlerts(status);
			userRepository.save(user);
			m.success(status);
		});
	}
	
	public Mono<Boolean> showAlertStatus(String uuid){
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			m.success(user.isSwitchOffAlerts());
		});
	}
}
