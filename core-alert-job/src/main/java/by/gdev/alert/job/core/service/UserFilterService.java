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

import com.google.common.collect.Sets;

import by.gdev.alert.job.core.model.Filter;
import by.gdev.alert.job.core.model.FilterDTO;
import by.gdev.alert.job.core.model.KeyWord;
import by.gdev.alert.job.core.model.WordDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import by.gdev.alert.job.core.model.db.key.TitleWord;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.DescriptionWordRepository;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.TechnologyWordRepository;
import by.gdev.alert.job.core.repository.TitleWordRepository;
import by.gdev.alert.job.core.repository.UserFilterRepository;
import by.gdev.common.exeption.ConflictExeption;
import by.gdev.common.exeption.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class UserFilterService {
	
	private final AppUserRepository userRepository;
	private final OrderModulesRepository modulesRepository;
	private final UserFilterRepository filterRepository;
	private final TitleWordRepository titleRepository;
	private final DescriptionWordRepository descriptionRepository;
	private final TechnologyWordRepository technologyRepository;
	
	private final ModelMapper mapper;
	
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
						throw new ConflictExeption(String.format("filter with name %s exists" , modules.getName()));
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
				.map(o -> o.get().getOrderModules().stream().findAny().get())
				.filter(e -> Objects.nonNull(e.getCurrentFilter())).map(e -> {
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
	
	public Mono<FilterDTO> createTitleWordToNegativeFilter(Long filterId, Long wordId){
		return Mono.justOrEmpty(filterRepository.findById(filterId))
				.zipWith(Mono.justOrEmpty(titleRepository.findById(wordId))).map(tuple -> {
					UserFilter filter = tuple.getT1();
					TitleWord word = tuple.getT2();
					if (CollectionUtils.isEmpty(filter.getNegativeTitles())) 
						filter.setNegativeTitles(Sets.newHashSet());
					filter.getNegativeTitles().add(word);
					filterRepository.save(filter);
					return mapper.map(filter, FilterDTO.class);
				}).switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))));
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
	
	public Mono<ResponseEntity<Void>> removeTitleWordFromNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findById(filterId))
		.zipWith(Mono.justOrEmpty(titleRepository.findById(wordId))).map(tuple -> {
			UserFilter filter = tuple.getT1();
			TitleWord word = tuple.getT2();
			if (filter.getNegativeTitles().removeIf(e -> e.equals(word))) {
				filterRepository.save(filter);
				return ResponseEntity.ok().build();
			}else
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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
	
	public Mono<FilterDTO> createTechnologyWordToNegativeFilter(Long filterId, Long wordId){
		return Mono.justOrEmpty(filterRepository.findById(filterId))
				.zipWith(Mono.justOrEmpty(technologyRepository.findById(wordId))).map(tuple -> {
					UserFilter filter = tuple.getT1();
					TechnologyWord word = tuple.getT2();
					if (CollectionUtils.isEmpty(filter.getNegativeTechnologies())) 
						filter.setNegativeTechnologies(Sets.newHashSet());
					filter.getNegativeTechnologies().add(word);
					filterRepository.save(filter);
					return mapper.map(filter, FilterDTO.class);
				}).switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))));
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
	
	public Mono<ResponseEntity<Void>> removeTechnologyWordFromNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findById(filterId))
		.zipWith(Mono.justOrEmpty(technologyRepository.findById(wordId))).map(tuple -> {
			UserFilter filter = tuple.getT1();
			TechnologyWord word = tuple.getT2();
			if (filter.getNegativeTechnologies().removeIf(e -> e.equals(word))) {
				filterRepository.save(filter);
				return ResponseEntity.ok().build();
			}else
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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
	
	public Mono<FilterDTO> createDescriptionWordToNegativeFilter(Long filterId, Long wordId){
		return Mono.justOrEmpty(filterRepository.findById(filterId))
				.zipWith(Mono.justOrEmpty(descriptionRepository.findById(wordId))).map(tuple -> {
					UserFilter filter = tuple.getT1();
					DescriptionWord word = tuple.getT2();
					if (CollectionUtils.isEmpty(filter.getNegativeDescriptions())) 
						filter.setNegativeDescriptions(Sets.newHashSet());
					filter.getNegativeDescriptions().add(word);
					filterRepository.save(filter);
					return mapper.map(filter, FilterDTO.class);
				}).switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))));
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
	
	public Mono<ResponseEntity<Void>> removeDescriptionWordFromNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findById(filterId))
		.zipWith(Mono.justOrEmpty(descriptionRepository.findById(wordId))).map(tuple -> {
			UserFilter filter = tuple.getT1();
			DescriptionWord word = tuple.getT2();
			if (filter.getNegativeDescriptions().removeIf(e -> e.equals(word))) {
				filterRepository.save(filter);
				return ResponseEntity.ok().build();
			}else
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		});
	}
}