package by.gdev.alert.job.core.service;

import java.util.Comparator;
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

import by.gdev.alert.job.core.configuration.ApplicationProperty;
import by.gdev.alert.job.core.model.AlertTime;
import by.gdev.alert.job.core.model.AppUserDTO;
import by.gdev.alert.job.core.model.Modules;
import by.gdev.alert.job.core.model.OrderModulesDTO;
import by.gdev.alert.job.core.model.Source;
import by.gdev.alert.job.core.model.SourceDTO;
import by.gdev.alert.job.core.model.UserAlertTimeDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.UserAlertTime;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.SourceSiteRepository;
import by.gdev.alert.job.core.repository.UserAlertTimeRepository;
import by.gdev.common.exeption.CollectionLimitExeption;
import by.gdev.common.exeption.ConflictExeption;
import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SourceSiteDTO;
import by.gdev.common.model.SubCategoryDTO;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreService {

    private final WebClient webClient;
    private final AppUserRepository userRepository;
    private final SourceSiteRepository sourceRepository;
    private final OrderModulesRepository modulesRepository;
    private final UserAlertTimeRepository alertTimeRepository;

    private final Scheduler scheduler;
    private final ModelMapper mapper;
    private final ApplicationProperty appProperty;

    public Mono<ResponseEntity<String>> authentication(String uuid, String mail) {
	return Mono.create(m -> {
	    if (Objects.isNull(uuid)) {
		m.success(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
	    } else {
		Optional<AppUser> appUser = userRepository.findByUuid(uuid);
		if (appUser.isPresent()) {
		    m.success(ResponseEntity.ok("ok"));
		} else {
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
				ex -> log.debug("can't send message on user mail {}, cause {}", user.getEmail(),
					ex.getMessage()));
	    } else {
		UserNotification un = new UserNotification(String.valueOf(user.getTelegram()),
			"Test message from alert");
		webClient.post().uri("http://notification:8019/telegram").bodyValue(un).retrieve()
			.bodyToMono(Void.class)
			.subscribe(e -> log.debug("successfully sent message on user mail {}", user.getEmail()),
				ex -> log.debug("can't send message on user mail {}, cause {}", user.getEmail(),
					ex.getMessage()));
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

    public Mono<Boolean> changeDefaultSendType(String uuid, boolean defaultSend) {
	return Mono.create(m -> {
	    AppUser user = userRepository.findByUuid(uuid)
		    .orElseThrow(() -> new ResourceNotFoundException("user not found"));
	    user.setDefaultSendType(defaultSend);
	    user = userRepository.save(user);
	    m.success(user.isDefaultSendType());
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

    public Mono<UserAlertTimeDTO> addAlertTime(String uuid, AlertTime alerTime) {
	return Mono.create(m -> {
	    AppUser user = userRepository.findByUuid(uuid)
		    .orElseThrow(() -> new ResourceNotFoundException("user not found"));
	    UserAlertTime uat = mapper.map(alerTime, UserAlertTime.class);
	    uat.setUser(user);
	    uat = alertTimeRepository.save(uat);
	    m.success(mapper.map(uat, UserAlertTimeDTO.class));
	});
    }

    public Mono<ResponseEntity<Void>> removeAlertTime(String uuid, Long alertId) {
	return Mono.justOrEmpty(alertTimeRepository.findByIdAndUserUuid(alertId, uuid))
		.switchIfEmpty(Mono.error(new ResourceNotFoundException("not found alert with id " + alertId)))
		.map(e -> {
		    alertTimeRepository.delete(e);
		    return ResponseEntity.ok().build();
		});
    }

    public Mono<AppUserDTO> showUserAlertInfo(String uuid) {
	return Mono.justOrEmpty(userRepository.findByUuidOneEagerUserAlertTimes(uuid))
		.switchIfEmpty(Mono.error(new ResourceNotFoundException("user not found"))).map(u -> {
		    AppUserDTO dto = mapper.map(u, AppUserDTO.class);
		    dto.setAlertTimeDTO(
			    u.getUserAlertTimes().stream().map(e -> mapper.map(e, UserAlertTimeDTO.class)).toList());
		    return dto;
		});

    }

    public Mono<OrderModulesDTO> createOrderModules(String uuid, Modules modules) {
	return Mono.justOrEmpty(userRepository.findByUuidOneEagerModules(uuid))
		.switchIfEmpty(Mono.error(new ResourceNotFoundException("user not found"))).map(u -> {
		    if (u.getOrderModules().size() >= appProperty.getLimitOrders())
			throw new CollectionLimitExeption("the limit for added modules");
		    if (modulesRepository.existsByNameAndUserUuid(modules.getName(), uuid))
			throw new ConflictExeption(String.format("module with name %s exists", modules.getName()));
		    OrderModules module = mapper.map(modules, OrderModules.class);
		    module.setUser(u);
		    module = modulesRepository.save(module);
		    return mapper.map(module, OrderModulesDTO.class);
		});
    }

    public Mono<OrderModulesDTO> updateOrderModules(String uuid, Long moduleId, Modules modules) {
	return Mono.justOrEmpty(modulesRepository.findByIdAndUserUuid(moduleId, uuid))
		.switchIfEmpty(Mono.error(new ResourceNotFoundException("not found module with id " + moduleId)))
		.map(e -> {
		    if (Objects.nonNull(modules.getName())
			    && modulesRepository.existsByNameAndUserUuid(modules.getName(), uuid))
			throw new ConflictExeption(String.format("module with name %s exists", modules.getName()));
		    mapper.map(modules, e);
		    e = modulesRepository.save(e);
		    return mapper.map(e, OrderModulesDTO.class);
		});
    }

    public Flux<OrderModulesDTO> showOrderModules(String uuid) {
	return Flux.fromIterable(modulesRepository.findAllByUserUuid(uuid))
		.map(e -> mapper.map(e, OrderModulesDTO.class));
    }

    public Mono<ResponseEntity<Void>> removeOrderModules(String uuid, Long moduleId) {
	return Mono.justOrEmpty(modulesRepository.findByIdAndUserUuidOneEagerSources(moduleId, uuid))
		.switchIfEmpty(Mono.error(new ResourceNotFoundException("not found module with id " + moduleId)))
		.map(e -> {
		    modulesRepository.delete(e);
		    e.getSources().forEach(sourceSite -> diactiveteSource(sourceSite));
		    return ResponseEntity.ok().build();
		});
    }

    public Flux<SourceDTO> showSourceSite(String uuid, Long id) {
	return Flux.just(modulesRepository.findByIdAndUserUuidOneEagerSources(id, uuid))
		.flatMapIterable(m -> m.get().getSources()).map(s -> {
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
		}).sort(Comparator.comparing(SourceDTO::getSiteSourceDTO, (s1, s2) -> s1.getId().compareTo(s2.getId())))
		.onErrorResume(NoSuchElementException.class,
			e -> Mono.error(new ResourceNotFoundException("not found module with id " + id)));
    }

    public Mono<ResponseEntity<SourceSiteDTO>> createSourceSite(String uuid, Long id, Source source) {
	return Mono.defer(() -> {
	    OrderModules module = modulesRepository.findByIdAndUserUuidOneEagerSources(id, uuid)
		    .orElseThrow(() -> new ResourceNotFoundException("not found module with id " + id));
	    if (module.getSources().size() >= appProperty.getLimitSources())
		throw new CollectionLimitExeption("the limit for added sources");
	    if (CollectionUtils.isEmpty(module.getSources())) {
		module.setSources(Sets.newHashSet());
	    }
	    Optional<SourceSite> existSource = module.getSources().stream()
		    .filter(s -> s.getSiteCategory().equals(source.getSiteCategory())
			    && Objects.equals(s.getSiteSubCategory(), source.getSiteSubCategory()))
		    .findAny();
	    if (existSource.isPresent())
		return Mono.error(new ConflictExeption("source exists"));
	    Optional<SourceSite> s = Objects.isNull(source.getSiteSubCategory())
		    ? sourceRepository.findBySourceSubCategoryIsNull(source.getSiteSource(), source.getSiteCategory())
		    : sourceRepository.findBySource(source.getSiteSource(), source.getSiteCategory(),
			    source.getSiteSubCategory());
	    SourceSite sourceSite = s.isPresent() ? s.get() : mapper.map(source, SourceSite.class);
	    if (!sourceSite.isActive())
		sourceSite.setActive(true);
	    sourceSite = sourceRepository.save(sourceSite);
	    module.getSources().add(sourceSite);
	    modulesRepository.save(module);

	    boolean categoryValue = Objects.nonNull(sourceSite.getSiteCategory())
		    && Objects.nonNull(sourceSite.getSiteSubCategory()) ? false : true;
	    changeParserSubcribe(sourceSite.getSiteCategory(), sourceSite.getSiteSubCategory(), categoryValue, true)
		    .subscribe();
	    return Mono.just(ResponseEntity.ok(mapper.map(sourceSite, SourceSiteDTO.class)));
	});
    }

    public Mono<ResponseEntity<Void>> removeSourceSite(String uuid, Long id, Long sourceId) {
	return Mono.create(m -> {
	    SourceSite sourceSite = sourceRepository.findById(sourceId)
		    .orElseThrow(() -> new ResourceNotFoundException("not found site source with id " + sourceId));
	    OrderModules module = modulesRepository.findByIdAndUserUuidOneEagerSources(id, uuid)
		    .orElseThrow(() -> new ResourceNotFoundException("not found module with id " + id));
	    if (module.getSources().removeIf(s -> s.equals(sourceSite))) {
		modulesRepository.save(module);
		diactiveteSource(sourceSite);
		m.success(ResponseEntity.ok().build());
	    } else {
		m.success(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
	    }
	});
    }

    public Flux<OrderDTO> showTrueFilterOrders(String uuid, Long moduleId, Long period) {
	Mono<OrderModules> modules = Mono
		.justOrEmpty(modulesRepository.findByIdAndUserUuidOneEagerSources(moduleId, uuid))
		.switchIfEmpty(Mono.error(new ResourceNotFoundException("user not found")));
	Flux<OrderDTO> source = modules.flatMapIterable(e -> e.getSources()).flatMap(s -> {
	    return webClient.get().uri("http://parser:8017/api/orders", b -> {
		b.queryParam("site_id", s.getSiteSource());
		b.queryParam("category_id", s.getSiteCategory());
		if (Objects.nonNull(s.getSiteSubCategory()))
		    b.queryParam("sub_id", s.getSiteSubCategory());
		b.queryParam("period", period);
		return b.build();
	    }).retrieve().bodyToFlux(OrderDTO.class);
	});
	Mono<UserFilter> currentFilter = modules.map(e -> e.getCurrentFilter()).onErrorResume(
		NullPointerException.class, ex -> Mono.error(new ResourceNotFoundException("current filter is empty")));
	return source.distinct(OrderDTO::getLink)
		.filterWhen(m -> currentFilter.map(e -> scheduler.isMatchUserFilter(m, e)))
		.sort(Comparator.comparing(OrderDTO::getDateTime).reversed());
    }

    public Flux<OrderDTO> showFalseFilterOrders(String uuid, Long moduleId, Long period) {
	Mono<OrderModules> modules = Mono
		.justOrEmpty(modulesRepository.findByIdAndUserUuidOneEagerSources(moduleId, uuid))
		.switchIfEmpty(Mono.error(new ResourceNotFoundException("user not found")));
	Flux<OrderDTO> source = modules.flatMapIterable(e -> e.getSources()).flatMap(s -> {
	    return webClient.get().uri("http://parser:8017/api/orders", b -> {
		b.queryParam("site_id", s.getSiteSource());
		b.queryParam("category_id", s.getSiteCategory());
		if (Objects.nonNull(s.getSiteSubCategory()))
		    b.queryParam("sub_id", s.getSiteSubCategory());
		b.queryParam("period", period);
		return b.build();
	    }).retrieve().bodyToFlux(OrderDTO.class);
	});
	Mono<UserFilter> currentFilter = modules.map(e -> e.getCurrentFilter()).onErrorResume(
		NullPointerException.class, ex -> Mono.error(new ResourceNotFoundException("current filter is empty")));
	return source.distinct(OrderDTO::getLink)
		.filterWhen(m -> currentFilter.map(e -> !scheduler.isMatchUserFilter(m, e)))
		.sort(Comparator.comparing(OrderDTO::getDateTime).reversed());
    }

    private void diactiveteSource(SourceSite sourceSite) {
	if (!modulesRepository.existsBySourcesAndSourcesActive(sourceSite, true)) {
	    sourceSite.setActive(false);
	    sourceRepository.save(sourceSite);
	    boolean cValue = sourceRepository.existsBySiteCategoryAndActive(sourceSite.getSiteCategory(), true);
	    boolean sValue = sourceRepository.existsBySiteCategoryAndSiteSubCategoryAndActive(
		    sourceSite.getSiteCategory(), sourceSite.getSiteSubCategory(), true);
	    changeParserSubcribe(sourceSite.getSiteCategory(), sourceSite.getSiteSubCategory(), cValue, sValue)
		    .subscribe();
	}
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
	}).retrieve().bodyToMono(Void.class).onErrorResume(ex -> Mono.error(ex));
    }
}