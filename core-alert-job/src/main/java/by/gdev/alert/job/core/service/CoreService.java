package by.gdev.alert.job.core.service;

import java.util.Objects;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.core.model.KeyWord;
import by.gdev.alert.job.core.model.WordDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import by.gdev.alert.job.core.model.db.key.TitleWord;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.DescriptionWordRepository;
import by.gdev.alert.job.core.repository.TechnologyWordRepository;
import by.gdev.alert.job.core.repository.TitleWordRepository;
import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.model.NotificationAlertType;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CoreService {

	private final WebClient webClient;
	private final AppUserRepository userRepository;
	private final TitleWordRepository titleRepository;
	private final DescriptionWordRepository descriptionRepository;
	private final TechnologyWordRepository technologyRepository;
	
	private final ModelMapper mapper;
	
	public Mono<ResponseEntity<String>> authentication(String uuid, String mail){
		return Mono.create(m -> {
			if (Objects.isNull(uuid)) {
				m.success(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
			}else {
				Optional<AppUser> appUser = userRepository.findByUuid(uuid);
					if (appUser.isPresent()) {
						m.success(ResponseEntity.ok("ok"));
					}else {
						AppUser user = new AppUser();
						user.setUuid(uuid);
						user.setEmail(mail);
						userRepository.save(user);
						m.success(ResponseEntity.status(HttpStatus.CREATED).body("user created"));
					}
			}
		});
	}
	
	
	public Mono<Void> sendTestMessageOnMai(String uuid) {
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			if (user.isDefaultSendType()) {
				UserNotification un = new UserNotification(user.getEmail(), "Test message from alerjob.by");
				webClient.post().uri("http://notification-alert-job:8019/mail").bodyValue(un).retrieve()
						.bodyToMono(Void.class).subscribe();
			} else {
				UserNotification un = new UserNotification(String.valueOf(user.getTelegram()),
						"Test message from alerjob.by");
				webClient.post().uri("http://notification-alert-job:8019/telegram").bodyValue(un).retrieve()
						.bodyToMono(Void.class).subscribe();
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

	public Mono<Boolean> showAlertStatus(String uuid) {
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			m.success(user.isSwitchOffAlerts());
		});
	}

	public Mono<Boolean> changeDefaultSendType(String uuid, boolean defaultSend) {
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			user.setDefaultSendType(defaultSend);
			user = userRepository.save(user);
			m.success(user.isDefaultSendType());
		});
	}
	
	public Mono<NotificationAlertType> notificationUserAlertType(String uuid) {
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			NotificationAlertType alerType = new NotificationAlertType();
			alerType.setType(user.isDefaultSendType());
			alerType.setValue(user.isDefaultSendType() ? user.getEmail() : String.valueOf(user.getTelegram()));
			m.success(alerType);
		});
	}
	
	public Flux<WordDTO> showTitleWords() {
		return Flux.fromIterable(titleRepository.findAll()).map(e -> mapper.map(e, WordDTO.class));
	}
	
	public Mono<ResponseEntity<WordDTO>> addTitleWord(KeyWord keyWord){
		return Mono.create(m -> {
			Optional<TitleWord> t = titleRepository.findByName(keyWord.getName());
			if (t.isPresent()) {
				m.success(ResponseEntity.status(HttpStatus.CONFLICT).build());
			}else {
				TitleWord titleWord = new TitleWord();
				titleWord.setName(keyWord.getName());
				titleWord = titleRepository.save(titleWord);
				m.success(ResponseEntity.ok(mapper.map(titleWord, WordDTO.class)));
			}
		});
	}
	
	public Flux<WordDTO> showTechnologyWords() {
		return Flux.fromIterable(technologyRepository.findAll()).map(e -> mapper.map(e, WordDTO.class));
	}
	
	public Mono<ResponseEntity<WordDTO>> addTechnologyWord(KeyWord keyWord){
		return Mono.create(m -> {
			Optional<TechnologyWord> t = technologyRepository.findByName(keyWord.getName());
			if (t.isPresent()) {
				m.success(ResponseEntity.status(HttpStatus.CONFLICT).build());
			}else {
				TechnologyWord technologyWordWord = new TechnologyWord();
				technologyWordWord.setName(keyWord.getName());
				technologyWordWord = technologyRepository.save(technologyWordWord);
				m.success(ResponseEntity.ok(mapper.map(technologyWordWord, WordDTO.class)));
			}
		});
	}
	
	public Flux<WordDTO> showDescriptionWords() {
		return Flux.fromIterable(descriptionRepository.findAll()).map(e -> mapper.map(e, WordDTO.class));
	}
	
	public Mono<ResponseEntity<WordDTO>> addDescriptionWord(KeyWord keyWord){
		return Mono.create(m -> {
			Optional<DescriptionWord> t = descriptionRepository.findByName(keyWord.getName());
			if (t.isPresent()) {
				m.success(ResponseEntity.status(HttpStatus.CONFLICT).build());
			}else {
				DescriptionWord descriptionWord = new DescriptionWord();
				descriptionWord.setName(keyWord.getName());
				descriptionWord = descriptionRepository.save(descriptionWord);
				m.success(ResponseEntity.ok(mapper.map(descriptionWord, WordDTO.class)));
			}
		});
	}
	
	public Mono<Void> changeUserTelegram(String uuid, Long telegramId) {
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			user.setTelegram(telegramId);
			userRepository.save(user);
			m.success();
		});
	}
}