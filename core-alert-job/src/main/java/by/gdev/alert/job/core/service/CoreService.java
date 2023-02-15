package by.gdev.alert.job.core.service;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.common.collect.Sets;

import by.gdev.alert.job.core.model.Modules;
import by.gdev.alert.job.core.model.OrderModulesDTO;
import by.gdev.alert.job.core.model.Source;
import by.gdev.alert.job.core.model.SourceDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.SourceSiteRepository;
import by.gdev.common.exeption.ConflictExeption;
import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.NotificationAlertType;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SourceSiteDTO;
import by.gdev.common.model.SubCategoryDTO;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreService {

	private final WebClient webClient;
	private final AppUserRepository userRepository;
	private final SourceSiteRepository sourceRepository;
	private final OrderModulesRepository modulesRepository;
	
	private final Scheduler scheduler;
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
				UserNotification un = new UserNotification(user.getEmail(), "Test message from alert");
				webClient.post().uri("http://notification:8019/mail").bodyValue(un).retrieve().bodyToMono(Void.class)
						.subscribe(e -> log.debug("successfully sent message on user mail {}", user.getEmail()),
								ex -> log.debug("can't send message on user mail {}, cause {}", user.getEmail(), ex.getMessage()));
			} else {
				UserNotification un = new UserNotification(String.valueOf(user.getTelegram()),
						"Test message from alert");
				webClient.post().uri("http://notification:8019/telegram").bodyValue(un).retrieve()
						.bodyToMono(Void.class)
						.subscribe(e -> log.debug("successfully sent message on user mail {}", user.getEmail()),
								ex -> log.debug("can't send message on user mail {}, cause {}", user.getEmail(), ex.getMessage()));
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
	
	
	
	public Mono<Void> changeUserTelegram(String uuid, Long telegramId) {
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			user.setTelegram(telegramId);
			userRepository.save(user);
			m.success();
		});
	}
	
	public Mono<ResponseEntity<OrderModulesDTO>> createOrderModules(String uuid, Modules modules) {
		return Mono.create(e -> {
			AppUser user = userRepository.findOneEagerOrderModules(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			if (!CollectionUtils.isEmpty(user.getOrderModules())) {
				for (OrderModules f : user.getOrderModules()) {
					if (f.getName().equals(modules.getName())) {
						throw new ConflictExeption(String.format("module with name %s exists" , modules.getName()));
					}
				}
			} else
				user.setOrderModules(Sets.newHashSet());
			OrderModules module = mapper.map(modules, OrderModules.class);
			module = modulesRepository.save(module);
			user.getOrderModules().add(module);
			userRepository.save(user);
			e.success(ResponseEntity.ok(mapper.map(module, OrderModulesDTO.class)));
		});
	}
	
	public Mono<OrderModulesDTO> updateOrderModules(String uuid, Long moduleId, Modules modules){
		return Mono.just(userRepository.findByUuidAndOrderModulesId(uuid, moduleId))
		.map(e -> {
			OrderModules m = e.get().getOrderModules().stream().findAny().get();
			mapper.map(modules, m);
			m = modulesRepository.save(m);
			return mapper.map(m, OrderModulesDTO.class);
		})
		.onErrorResume(NoSuchElementException.class, 	e -> Mono.error(new ResourceNotFoundException("not found module " + moduleId)));
	}
	
	
	
	public Flux<OrderModulesDTO> showOrderModules(String uuid) {
		return Flux.just(userRepository.findOneEagerOrderModules(uuid)).flatMapIterable(u -> u.get().getOrderModules())
				.map(e -> mapper.map(e, OrderModulesDTO.class)).onErrorResume(NoSuchElementException.class,
						e -> Mono.error(new ResourceNotFoundException("user not found")));
	}
	
	public Mono<ResponseEntity<Void>> removeOrderModules(String uuid, Long moduleId){
		return Mono.create(e -> {
			AppUser user = userRepository.findOneEagerOrderModules(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			OrderModules module = modulesRepository.findById(moduleId).orElseThrow(() -> new ResourceNotFoundException("not found module id " + moduleId));
			if (user.getOrderModules().removeIf(i -> i.getId().equals(module.getId()))) {
				userRepository.save(user);
				modulesRepository.delete(module);
				e.success(ResponseEntity.ok().build());
			}else 
				e.success(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
		});
	}
	
	public Flux<SourceDTO> showSourceSite(String uuid, Long id) {
		return Flux.just(userRepository.findByUuidAndOrderModulesIdOneEagerSources(uuid, id))
				.map(e -> e.get().getOrderModules().stream().findAny().get()).flatMapIterable(o -> o.getSources())
				.map(s -> {
					SourceDTO dto = new SourceDTO();
					dto.setId(s.getId());
					SiteSourceDTO source = new SiteSourceDTO();
					source.setId(s.getSiteSource());
					dto.setSiteSourceDTO(source);
					CategoryDTO category = new CategoryDTO();
					category.setId(s.getSiteCategory());
					dto.setSiteCategoryDTO(category);
					SubCategoryDTO subcategory = new SubCategoryDTO();
					subcategory.setId(s.getSiteSubCategory());
					dto.setSiteSubCategoryDTO(subcategory);
					return dto;
				}).flatMap(s -> {
					Long sourceId = s.getSiteSourceDTO().getId();
					Long categoryId = s.getSiteCategoryDTO().getId();
					Mono<SiteSourceDTO> m1 = webClient.get()
							.uri(String.format("http://parser:8017/api/site/%s", s.getSiteSourceDTO().getId()))
							.retrieve().bodyToMono(SiteSourceDTO.class);
					Mono<CategoryDTO> m2 = webClient.get()
							.uri(String.format("http://parser:8017/api/site/%s/category/%s", sourceId, categoryId))
							.retrieve().bodyToMono(CategoryDTO.class).onErrorReturn(new CategoryDTO());
					Mono<SubCategoryDTO> m3 = webClient.get()
							.uri(String.format("http://parser:8017/api/category/%s/subcategory/%s", categoryId,
									s.getSiteSubCategoryDTO().getId()))
							.retrieve().bodyToMono(SubCategoryDTO.class).onErrorReturn(new SubCategoryDTO());
					Mono<Tuple3<SiteSourceDTO, CategoryDTO, SubCategoryDTO>> tuple = Mono.zip(m1, m2, m3);
					return tuple.map(t -> {
						SourceDTO dto = s;
						dto.setSiteSourceDTO(t.getT1());
						dto.setSiteCategoryDTO(t.getT2());
						dto.setSiteSubCategoryDTO(t.getT3());
						return s;
					});
				}).onErrorResume(NoSuchElementException.class,
						e -> Mono.error(new ResourceNotFoundException("user not found")));
	}
	
	public Mono<ResponseEntity<SourceSiteDTO>> createSourceSite(String uuid, Long id, Source source) {
		return Mono.defer(() -> {
			AppUser user = userRepository.findByUuidAndOrderModulesIdOneEagerSources(uuid, id)
					.orElseThrow(() -> new ResourceNotFoundException("not found module with id " + id));
			
			OrderModules module = user.getOrderModules().stream().findFirst().get();
			if (CollectionUtils.isEmpty(module.getSources())) {
				module.setSources(Sets.newHashSet());
			}
			Optional<SourceSite> existSource = module.getSources().stream()
					.filter(s -> s.getSiteCategory().equals(source.getSiteCategory())
							&& s.getSiteSubCategory().equals(source.getSiteSubCategory()))
					.findAny();
			if (existSource.isPresent())
				return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
			SourceSite sourceSite = mapper.map(source, SourceSite.class);
			sourceRepository.save(sourceSite);
			module.getSources().add(sourceSite);
			modulesRepository.save(module);
			changeParserSubcribe(sourceSite.getSiteCategory(), sourceSite.getSiteSubCategory(), true, true)
			.subscribe(
					c -> log.info("changed status for {}, {}", sourceSite.getSiteCategory(),
							sourceSite.getSiteSubCategory()),
					ex -> log.info("failed to change parser status for {} {}",
							sourceSite.getSiteCategory(), sourceSite.getSiteSubCategory()));
			return Mono.just(ResponseEntity.ok(mapper.map(sourceSite, SourceSiteDTO.class)));
		});
		
	}
	
	public Mono<ResponseEntity<Void>> removeSourceSite(String uuid, Long id, Long sourceId) {
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuidAndOrderModulesIdOneEagerSources(uuid, id)
					.orElseThrow(() -> new ResourceNotFoundException("not found module with id " + id));
			SourceSite sourceSite = sourceRepository.findById(sourceId)
					.orElseThrow(() -> new ResourceNotFoundException("not found site source with id " + sourceId));
			OrderModules module = user.getOrderModules().stream().findFirst().get();
			if (module.getSources().removeIf(s -> s.equals(sourceSite))) {
				modulesRepository.save(module);
				if (!modulesRepository.existsBySources(sourceSite)) {
					sourceRepository.delete(sourceSite);
					boolean cValue = sourceRepository.existsBySiteCategory(sourceSite.getSiteCategory());
					boolean sValue = sourceRepository.existsBySiteCategoryAndSiteSubCategory(
							sourceSite.getSiteCategory(), sourceSite.getSiteSubCategory());
					changeParserSubcribe(sourceSite.getSiteCategory(), sourceSite.getSiteSubCategory(), cValue, sValue)
							.subscribe(
									c -> log.debug("changed status for {}, {}", sourceSite.getSiteCategory(),
											sourceSite.getSiteSubCategory()),
									ex -> log.debug("failed to change parser status for {} {}",
											sourceSite.getSiteCategory(), sourceSite.getSiteSubCategory()));
				}
				m.success(ResponseEntity.ok().build());
			} else {
				m.success(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
			}
		});
	}
	
	public Flux<OrderDTO> showOrdersByModule(String uuid, Long moduleId) {
		Flux<OrderModules> modules = Flux.just(userRepository.findByUuidAndOrderModulesId(uuid, moduleId))
				.map(u -> u.get().getOrderModules().stream().findAny().get())
				.onErrorResume(NoSuchElementException.class,
						e -> Flux.error(new ResourceNotFoundException("user not found")));
		//TODO check if this logic works 
		Mono<UserFilter> currentFilter = modules.map(e -> e.getCurrentFilter()).next();
		Flux<OrderDTO> source = 
				modules.flatMapIterable(e -> e.getSources()).flatMap(s -> {
					return webClient.get().uri("http://parser:8017/api/orders", b -> {
						b.queryParam("site_id", s.getSiteSource());
						b.queryParam("category_id", s.getSiteCategory());
						if (Objects.nonNull(s.getSiteSubCategory())) 
							b.queryParam("sub_id", s.getSiteSubCategory());
						return b.build();
					}).retrieve().bodyToFlux(OrderDTO.class);
				});
		Flux<Tuple2<OrderDTO, UserFilter>> zip = Flux.zip(source, currentFilter);
		// f.getT1() - order, f.getT2() - user filter
		return zip.filter(f -> scheduler.isMatchUserFilter(f.getT1(), f.getT2())).map(e -> e.getT1());
	}
	
	private Mono<Void> changeParserSubcribe(Long category, Long subcategory, boolean cValue, boolean sValue) {
		return webClient.patch().uri("http://parser:8017/api/subscribe/sources", b -> {
			b.queryParam("category_id", category);
			b.queryParam("category_value", cValue);
			if (Objects.nonNull(subcategory)) {
				b.queryParam("subcategory_id", subcategory);
				b.queryParam("subcategory_value", sValue);
			}
			return b.build();
		}).retrieve().bodyToMono(Void.class);
	}
}