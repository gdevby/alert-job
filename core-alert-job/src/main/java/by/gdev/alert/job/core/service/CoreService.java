package by.gdev.alert.job.core.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.common.collect.Sets;

import by.gdev.alert.job.core.model.Filter;
import by.gdev.alert.job.core.model.FilterDTO;
import by.gdev.alert.job.core.model.KeyWord;
import by.gdev.alert.job.core.model.Source;
import by.gdev.alert.job.core.model.SourceDTO;
import by.gdev.alert.job.core.model.WordDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import by.gdev.alert.job.core.model.db.key.TitleWord;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.DescriptionWordRepository;
import by.gdev.alert.job.core.repository.SourceSiteRepository;
import by.gdev.alert.job.core.repository.TechnologyWordRepository;
import by.gdev.alert.job.core.repository.TitleWordRepository;
import by.gdev.alert.job.core.repository.UserFilterRepository;
import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.NotificationAlertType;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SourceSiteDTO;
import by.gdev.common.model.SubCategoryDTO;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

@Service
@RequiredArgsConstructor
public class CoreService {

	private final WebClient webClient;
	private final AppUserRepository userRepository;
	private final UserFilterRepository filterRepository;
	private final TitleWordRepository titleRepository;
	private final DescriptionWordRepository descriptionRepository;
	private final TechnologyWordRepository technologyRepository;
	private final SourceSiteRepository sourceRepository;
	
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
	
	@Transactional(readOnly = true)
	public Flux<FilterDTO> showUserFilters(String uuid){
		return Flux.just(userRepository.findOneEagerUserFilters(uuid).orElseThrow(() -> new ResourceNotFoundException()))
				.flatMapIterable(u -> u.getFilters()).map(m ->  {
					FilterDTO f = mapper.map(m, FilterDTO.class);
					List<WordDTO> title = m.getTitles().stream().map(e -> mapper.map(e, WordDTO.class)).toList();
					f.setTitlesDTO(title);
					List<WordDTO> des = m.getDescriptions().stream().map(e -> mapper.map(e, WordDTO.class)).toList();
					f.setDescriptionsDTO(des);
					List<WordDTO> tech = m.getTechnologies().stream().map(e -> mapper.map(e, WordDTO.class)).toList();
					f.setTechnologiesDTO(tech);
					return f;
				});
	}
	
	public Mono<FilterDTO> createUserFilter(String uuid, Filter filter){
		return Mono.create(m -> {
			AppUser user = userRepository.findOneEagerUserFilters(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			UserFilter userFilter = new UserFilter();
			userFilter.setName(filter.getName());
			userFilter.setMaxValue(filter.getMaxValue());
			userFilter.setMinValue(filter.getMinValue());
			filterRepository.save(userFilter);
			Set<UserFilter> set = CollectionUtils.isEmpty(user.getFilters()) ? Sets.newHashSet()
					: Sets.newHashSet(user.getFilters());
			set.add(userFilter);
			user.setFilters(set);
			userRepository.save(user);
			m.success(mapper.map(userFilter, FilterDTO.class));
		});
	}
	
	public Mono<ResponseEntity<FilterDTO>> updateUserFilter(String uuid, Long filterId, Filter filter){
		return Mono.create(m -> {
			AppUser user = userRepository.findOneEagerUserFilters(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			UserFilter userFilter = filterRepository.findUserFilterById(filterId)
					.orElseThrow(() -> new ResourceNotFoundException("not found filter with id " + filterId));
			if (!user.getFilters().contains(userFilter))
				m.success(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
			mapper.map(filter, userFilter);
			userFilter = filterRepository.save(userFilter);
				m.success(ResponseEntity.ok(mapper.map(userFilter, FilterDTO.class)));
		});
	}
	
	public Mono<ResponseEntity<Void>> removeUserFilter(String uuid, Long filterId) {
		return Mono.create(m -> {
			AppUser user = userRepository.findOneEagerUserFilters(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			UserFilter userFilter = filterRepository.findById(filterId)
					.orElseThrow(() -> new ResourceNotFoundException("not found filter with id " + filterId));
			if (user.getFilters().removeIf(f -> f.getId() == filterId)) {
				filterRepository.delete(userFilter);
				userRepository.save(user);
				m.success(ResponseEntity.ok().build());
			} else {
				m.success(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
			}
		});
	}
	
	public Mono<Void> currentFilter(String uuid, Long filterId){
		return Mono.create(m -> {
			AppUser user = userRepository.findOneEagerUserFilters(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			UserFilter userFilter = filterRepository.findById(filterId)
					.orElseThrow(() -> new ResourceNotFoundException("not found filter with id " + filterId));
			user.setCurrentFilter(userFilter);
			userRepository.save(user);
			m.success();
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
	
	public Flux<SourceDTO> showSourceSite(String uuid) {
		return Flux
				.just(userRepository.findOneEagerSourceSite(uuid)
						.orElseThrow(() -> new ResourceNotFoundException("user not found")))
				.flatMapIterable(u -> u.getSources()).map(s -> {
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
					Long subcategoryId = s.getSiteSubCategoryDTO().getId();
					Mono<SiteSourceDTO> m1 = webClient.get()
							.uri(String.format("http://parser-alert-job:8017/api/site/%s", s.getSiteSourceDTO().getId()))
							.retrieve().bodyToMono(SiteSourceDTO.class);
					Mono<CategoryDTO> m2 = webClient.get()
							.uri(String.format("http://parser-alert-job:8017/api/site/%s/category/%s", sourceId, categoryId))
							.retrieve().bodyToMono(CategoryDTO.class);
					Mono<SubCategoryDTO> m3 = webClient.get().uri(String
							.format("http://parser-alert-job:8017/api/category/%s/subcategory/%s", categoryId, subcategoryId))
							.retrieve().bodyToMono(SubCategoryDTO.class);
					Mono<Tuple3<SiteSourceDTO, CategoryDTO, SubCategoryDTO>> tuple = Mono.zip(m1, m2, m3);
					return tuple.map(t -> {
						SourceDTO dto = s;
						dto.setSiteSourceDTO(t.getT1());
						dto.setSiteCategoryDTO(t.getT2());
						dto.setSiteSubCategoryDTO(t.getT3());
						return s;
					});
				});
	}
	
	public Mono<SourceSiteDTO> createSourceSite(String uuid, Source source){
		return Mono.create(m -> {
			AppUser user = userRepository.findOneEagerSourceSite(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			if (CollectionUtils.isEmpty(user.getSources())) {
				user.setSources(Sets.newHashSet());
			}
			SourceSite sourceSite = mapper.map(source, SourceSite.class);
			sourceRepository.save(sourceSite);
			user.getSources().add(sourceSite);
			userRepository.save(user);
			m.success(mapper.map(sourceSite, SourceSiteDTO.class));
		});
	}
	
	public Mono<ResponseEntity<Void>> removeSourceSite(String uuid, Long sourceId){
		return Mono.create(m -> {
			AppUser user = userRepository.findOneEagerSourceSite(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("user not found"));
			SourceSite sourceSite = sourceRepository.findById(sourceId)
					.orElseThrow(() -> new ResourceNotFoundException("not found site source with id " + sourceId));
			
			if (user.getSources().removeIf(s -> s.equals(sourceSite))) {
				userRepository.save(user);
				m.success(ResponseEntity.ok().build());
			}else {
				m.success(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
			}
		});
	}
}