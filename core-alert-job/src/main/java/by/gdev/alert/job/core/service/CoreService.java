package by.gdev.alert.job.core.service;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.common.collect.Sets;

import by.gdev.alert.job.core.model.Filter;
import by.gdev.alert.job.core.model.FilterDTO;
import by.gdev.alert.job.core.model.KeyWord;
import by.gdev.alert.job.core.model.Modules;
import by.gdev.alert.job.core.model.OrderModulesDTO;
import by.gdev.alert.job.core.model.Source;
import by.gdev.alert.job.core.model.SourceDTO;
import by.gdev.alert.job.core.model.WordDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import by.gdev.alert.job.core.model.db.key.TitleWord;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.DescriptionWordRepository;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.SourceSiteRepository;
import by.gdev.alert.job.core.repository.TechnologyWordRepository;
import by.gdev.alert.job.core.repository.TitleWordRepository;
import by.gdev.alert.job.core.repository.UserFilterRepository;
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
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreService {

	private final WebClient webClient;
	private final AppUserRepository userRepository;
	private final UserFilterRepository filterRepository;
	private final TitleWordRepository titleRepository;
	private final DescriptionWordRepository descriptionRepository;
	private final TechnologyWordRepository technologyRepository;
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
	
	public Mono<Page<WordDTO>> showTitleWords(String name, Integer page) {
		return Mono.defer(() -> {
			PageRequest p = PageRequest.of(page, 10);
			Function<TitleWord, WordDTO> func = word -> mapper.map(word, WordDTO.class);
			return StringUtils.isEmpty(name) ? Mono.just(titleRepository.findAll(p).map(func))
					: Mono.just(titleRepository.findByNameIsStartingWith(name, p).map(func));
		});
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
	
	public Mono<Page<WordDTO>> showTechnologyWords(String name, Integer page) {
		return Mono.defer(() -> {
			PageRequest p = PageRequest.of(page, 10);
			Function<TechnologyWord, WordDTO> func = word -> mapper.map(word, WordDTO.class);
			return StringUtils.isEmpty(name) ? Mono.just(technologyRepository.findAll(p).map(func))
					: Mono.just(technologyRepository.findByNameIsStartingWith(name, p).map(func));
		});
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
	
	public Mono<Page<WordDTO>> showDescriptionWords(String name, Integer page) {
		return Mono.defer(() -> {
			PageRequest p = PageRequest.of(page, 10);
			Function<DescriptionWord, WordDTO> func = word -> mapper.map(word, WordDTO.class);
			return StringUtils.isEmpty(name) ? Mono.just(descriptionRepository.findAll(p).map(func))
					: Mono.just(descriptionRepository.findByNameIsStartingWith(name, p).map(func));
		});
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
	
	public Mono<ResponseEntity<OrderModulesDTO>> createOrderModules(String uuid, Modules modules) {
		return Mono.create(e -> {
			AppUser user = userRepository.findOneEagerOrderModules(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			if (!CollectionUtils.isEmpty(user.getOrderModules())) {
				for (OrderModules f : user.getOrderModules()) {
					if (f.getName().equals(modules.getName()))
						e.success(ResponseEntity.status(HttpStatus.CONFLICT).build());
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
	
	public Flux<FilterDTO> showUserFilters(String uuid, Long moduleId) {
		return Flux.just(userRepository.findByUuidAndOrderModulesIdOneEagerFilters(uuid, moduleId)).publishOn(Schedulers.boundedElastic())
			.flatMapIterable(u -> u.get().getOrderModules()).flatMapIterable(e -> e.getFilters()).map(e -> {
				FilterDTO dto = mapper.map(e, FilterDTO.class);
				dto.setTitlesDTO(e.getTitles().stream().map(e1 -> mapper.map(e1, WordDTO.class)).toList());
				dto.setDescriptionsDTO(
						e.getDescriptions().stream().map(e1 -> mapper.map(e1, WordDTO.class)).toList());
				dto.setTechnologiesDTO(
						e.getTechnologies().stream().map(e1 -> mapper.map(e1, WordDTO.class)).toList());
				return dto;
			}).onErrorResume(NoSuchElementException.class,
					e -> Mono.error(new ResourceNotFoundException("order not found")));
	}

	public Mono<ResponseEntity<FilterDTO>> createUserFilter(String uuid, Long moduleId, Filter filter){
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuidAndOrderModulesIdOneEagerFilters(uuid, moduleId).orElseThrow(() -> new ResourceNotFoundException("user not found"));
			OrderModules modules = user.getOrderModules().stream().findAny().get();
			if (!CollectionUtils.isEmpty(modules.getFilters())) {
				for (UserFilter f : modules.getFilters()) {
					if (f.getName().equals(filter.getName()))
						m.success(ResponseEntity.status(HttpStatus.CONFLICT).build());
				}
			}else
				modules.setFilters(Sets.newHashSet());
			UserFilter userFilter = new UserFilter();
			userFilter.setName(filter.getName());
			userFilter.setMaxValue(filter.getMaxValue());
			userFilter.setMinValue(filter.getMinValue());
			userFilter.setModule(modules);
			userFilter.setModule(modules);
			filterRepository.save(userFilter);
			m.success(ResponseEntity.ok(mapper.map(userFilter, FilterDTO.class)));
		});
	}
	
	public Mono<ResponseEntity<Void>> removeUserFilter(String uuid, Long moduleId, Long filterId) {
		Mono<AppUser> user = Mono.justOrEmpty(userRepository.findOneEagerOrderModules(uuid))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException("user not found")));
		Mono<UserFilter> userFilterTest = Mono.justOrEmpty(filterRepository.findByIdAndOrderModuleId(filterId, moduleId))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException("user filter not found")));
		return Mono.zip(user, userFilterTest).map(e -> {
			OrderModules module = e.getT1().getOrderModules().stream().findAny().get();
			UserFilter filter = e.getT2();
			if (module.getFilters().removeIf(f -> f.getId().equals(filter.getId()))) {
				modulesRepository.save(module);
				filterRepository.delete(filter);
				return ResponseEntity.ok().build();
			} else
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		});
	}
	
	public Mono<Void> replaceCurrentFilter(String uuid, Long moduleId, Long filterId){
		return Mono.create(m -> {
			AppUser user = userRepository.findByUuidAndOrderModulesIdOneEagerCurrentFilter(uuid, moduleId)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			UserFilter userFilterTest = filterRepository.findById(filterId).orElseThrow(() -> new ResourceNotFoundException("user filter not found"));
			OrderModules module = user.getOrderModules().stream().findAny().get();
			module.setCurrentFilter(userFilterTest);
			modulesRepository.save(module);
			m.success();
		});
	}
	
	public Mono<FilterDTO> showUserCurrentFilter(String uuid, Long moduleId) {
		return Mono.just(userRepository.findByUuidAndOrderModulesIdOneEagerCurrentFilter(uuid, moduleId))
				.map(o -> o.get().getOrderModules().stream().findAny().get()).map(e -> {
					FilterDTO dto = mapper.map(e.getCurrentFilter(), FilterDTO.class);
					return dto;
				}).onErrorResume(NoSuchElementException.class,
						e -> Mono.error(new ResourceNotFoundException("current filter not found")));
	}
	
	public Mono<FilterDTO> updateCurrentFilter(String uuid, Long moduleId, Long filterId, Filter filter){
	return Mono.create(m -> {
		AppUser user = userRepository.findByUuidAndOrderModulesIdOneEagerFilters(uuid, moduleId)
				.orElseThrow(() -> new ResourceNotFoundException("not found user with"));
		UserFilter currentFilter = user.getOrderModules().stream().findAny().get().getCurrentFilter();
		mapper.map(filter, currentFilter);
		currentFilter = filterRepository.save(currentFilter);
		m.success(mapper.map(currentFilter, FilterDTO.class));
	});
}
	
	public Mono<Void> createTitleWordToFilter(Long filterId, Long wordId){
		return Mono.create(m ->{
			UserFilter filter = filterRepository.findOneEagerTitleWords(filterId)
					.orElseThrow(() -> new ResourceNotFoundException("not found filter with id " + filterId));
			TitleWord word = titleRepository.findById(wordId)
					.orElseThrow(() -> new ResourceNotFoundException("not found word with id " + wordId));
			if (CollectionUtils.isEmpty(filter.getTitles())) {
				filter.setTitles(Sets.newHashSet());
			}
			filter.getTitles().add(word);
			filterRepository.save(filter);
			m.success();
		});
	}
	
	public Mono<ResponseEntity<Void>> removeTitleWordFromFilter(Long filterId, Long wordId) {
		return Mono.create(m -> {
			UserFilter filter = filterRepository.findOneEagerTitleWords(filterId)
					.orElseThrow(() -> new ResourceNotFoundException("not found filter with id " + filterId));
			TitleWord word = titleRepository.findById(wordId)
					.orElseThrow(() -> new ResourceNotFoundException("not found word with id " + wordId));
			if (filter.getTitles().removeIf(e -> e.equals(word))) {
				filterRepository.save(filter);
				m.success(ResponseEntity.ok().build());
			} else {
				m.success(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
			}
		});
	}
	
	public Mono<Void> createTechnologyWordToFilter(Long filterId, Long wordId){
		return Mono.create(m ->{
			UserFilter filter = filterRepository.findOneEagerTechnologyWords(filterId)
					.orElseThrow(() -> new ResourceNotFoundException("not found filter with id " + filterId));
			TechnologyWord word = technologyRepository.findById(wordId)
					.orElseThrow(() -> new ResourceNotFoundException("not found word with id " + wordId));
			if (CollectionUtils.isEmpty(filter.getTechnologies())) {
				filter.setTechnologies(Sets.newHashSet());
			}
			filter.getTechnologies().add(word);
			filterRepository.save(filter);
			m.success();
		});
	}
	
	public Mono<ResponseEntity<Void>> removeTechnologyWordFromFilter(Long filterId, Long wordId) {
		return Mono.create(m -> {
			UserFilter filter = filterRepository.findOneEagerTechnologyWords(filterId)
					.orElseThrow(() -> new ResourceNotFoundException("not found filter with id " + filterId));
			TechnologyWord word = technologyRepository.findById(wordId)
					.orElseThrow(() -> new ResourceNotFoundException("not found word with id " + wordId));
			if (filter.getTechnologies().removeIf(e -> e.equals(word))) {
				filterRepository.save(filter);
				m.success(ResponseEntity.ok().build());
			} else {
				m.success(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
			}
		});
	}
	
	public Mono<Void> createDescriptionWordToFilter(Long filterId, Long wordId){
		return Mono.create(m ->{
			UserFilter filter = filterRepository.findOneEagerDescriptionWords(filterId)
					.orElseThrow(() -> new ResourceNotFoundException("not found filter with id " + filterId));
			DescriptionWord word = descriptionRepository.findById(wordId)
					.orElseThrow(() -> new ResourceNotFoundException("not found word with id " + wordId));
			if (CollectionUtils.isEmpty(filter.getDescriptions())) {
				filter.setDescriptions(Sets.newHashSet());
			}
			filter.getDescriptions().add(word);
			filterRepository.save(filter);
			m.success();
		});
	}
	
	public Mono<ResponseEntity<Void>> removeDescriptionWordFromFilter(Long filterId, Long wordId) {
		return Mono.create(m -> {
			UserFilter filter = filterRepository.findOneEagerDescriptionWords(filterId)
					.orElseThrow(() -> new ResourceNotFoundException("not found filter with id " + filterId));
			DescriptionWord word = descriptionRepository.findById(wordId)
					.orElseThrow(() -> new ResourceNotFoundException("not found word with id " + wordId));
			if (filter.getDescriptions().removeIf(e -> e.equals(word))) {
				filterRepository.save(filter);
				m.success(ResponseEntity.ok().build());
			} else {
				m.success(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
			}
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