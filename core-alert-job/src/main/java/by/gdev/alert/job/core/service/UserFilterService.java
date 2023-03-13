package by.gdev.alert.job.core.service;

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
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import by.gdev.alert.job.core.model.db.key.TitleWord;
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

@Service
@RequiredArgsConstructor
public class UserFilterService {
	
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
	return 	Flux.fromIterable(filterRepository.findAllByModuleIdAndUserUuid(moduleId, uuid)).map(e -> {
			FilterDTO dto = mapper.map(e, FilterDTO.class);
			dto.setTitlesDTO(e.getTitles().stream().map(e1 -> mapper.map(e1, WordDTO.class)).toList());
			dto.setDescriptionsDTO(e.getDescriptions().stream().map(e1 -> mapper.map(e1, WordDTO.class)).toList());
			dto.setTechnologiesDTO(e.getTechnologies().stream().map(e1 -> mapper.map(e1, WordDTO.class)).toList());
			return dto;
		});
	}

	public Mono<FilterDTO> createUserFilter(String uuid, Long moduleId, Filter filter){
		return Mono.justOrEmpty(modulesRepository.findByIdAndUserUuid(moduleId, uuid))
		.switchIfEmpty(Mono.error(new ResourceNotFoundException("not found module with id " + moduleId)))
		.map(e -> {
			if (filterRepository.existsByNameAndModule(filter.getName(), e))
				throw new ConflictExeption(String.format("filter with name %s exists", filter.getName()));
			UserFilter userFilter = new UserFilter();
			userFilter.setName(filter.getName());
			userFilter.setMaxValue(filter.getMaxValue());
			userFilter.setMinValue(filter.getMinValue());
			userFilter.setModule(e);
			userFilter = filterRepository.save(userFilter);
			return mapper.map(userFilter, FilterDTO.class);
			
		});
	}
	
	public Mono<ResponseEntity<Void>> removeUserFilter(String uuid, Long moduleId, Long filterId) {
		return Mono.justOrEmpty(filterRepository.findByIdAndModuleIdAndUserUuid(filterId, moduleId, uuid))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found filter by module %s and filter %s", moduleId, filterId))))
				.map(e -> {
					filterRepository.delete(e);
					return ResponseEntity.ok().build();
				});
	}
	
	public Mono<FilterDTO> replaceCurrentFilter(String uuid, Long moduleId, Long filterId){
		Mono<UserFilter> filter = Mono.justOrEmpty(filterRepository.findByIdAndModuleId(filterId, moduleId))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(String.format("not found filter by module %s and filter", moduleId, filterId))));
		Mono<OrderModules> modules = Mono.justOrEmpty(modulesRepository.findByIdAndUserUuidOneEagerCurrentFilter(filterId, uuid))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(String.format("not found module by %s", moduleId))));
		return Mono.zip(filter, modules).map(tuple -> {
			UserFilter f = tuple.getT1();
			OrderModules o = tuple.getT2();
			o.setCurrentFilter(f);
			modulesRepository.save(o);
			return mapper.map(f, FilterDTO.class);
		});
	}
	
	public Mono<FilterDTO> showUserCurrentFilter(String uuid, Long moduleId) {
		return Mono.justOrEmpty(modulesRepository.findByIdAndUserUuidOneEagerCurrentFilter(moduleId, uuid))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException("current filter not found")))
				.filter(f -> Objects.nonNull(f.getCurrentFilter())).map(e -> {
					return mapper.map(e.getCurrentFilter(), FilterDTO.class);
				});
	}
	
	public Mono<FilterDTO> updateFilter(String uuid, Long moduleId, Long filterId, Filter filter) {
		return Mono.justOrEmpty(filterRepository.findByIdAndModuleIdAndUserUuid(filterId, moduleId, uuid))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found filter by module %s and filter %s", moduleId, filterId))))
				.map(e -> {
					if (filterRepository.existsByNameAndModule(filter.getName(), e.getModule()))
						throw new ConflictExeption(String.format("filter with name %s exists", filter.getName()));
					UserFilter userFilter = e;
					mapper.map(filter, userFilter);
					userFilter = filterRepository.save(userFilter);
					return mapper.map(userFilter, FilterDTO.class);
				});
	}

	public Mono<FilterDTO> createTitleWordToFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findById(filterId))
				.zipWith(Mono.justOrEmpty(titleRepository.findById(wordId))).map(tuple -> {
					UserFilter f = tuple.getT1();
					TitleWord w = tuple.getT2();
					if (CollectionUtils.isEmpty(f.getTitles()))
						f.setTitles(Sets.newHashSet());
					f.getNegativeTitles().add(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				}).switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))));
	}

	public Mono<FilterDTO> createTitleWordToNegativeFilter(Long filterId, Long wordId) {
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
		return Mono.justOrEmpty(filterRepository.findById(filterId))
				.zipWith(Mono.justOrEmpty(titleRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TitleWord t = tuple.getT2();
					if (f.getTitles().removeIf(e -> e.getId().equals(t.getId()))) {
						filterRepository.save(f);
						return ResponseEntity.ok().build();
					} else
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
				});
	}
	
	public Mono<ResponseEntity<Void>> removeTitleWordFromNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findById(filterId))
				.zipWith(Mono.justOrEmpty(titleRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter filter = tuple.getT1();
					TitleWord word = tuple.getT2();
					if (filter.getNegativeTitles().removeIf(e -> e.equals(word))) {
						filterRepository.save(filter);
						return ResponseEntity.ok().build();
					} else
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
				});
	}
	
	public Mono<FilterDTO> createTechnologyWordToFilter(Long filterId, Long wordId){
		return Mono.justOrEmpty(filterRepository.findById(filterId))
				.zipWith(Mono.justOrEmpty(technologyRepository.findById(wordId))).map(tuple -> {
					UserFilter f = tuple.getT1();
					TechnologyWord w = tuple.getT2();
					if (CollectionUtils.isEmpty(f.getTechnologies()))
						f.setTechnologies(Sets.newHashSet());
					f.getTechnologies().add(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				}).switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))));
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
		return Mono.justOrEmpty(filterRepository.findById(filterId))
				.zipWith(Mono.justOrEmpty(technologyRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TechnologyWord t = tuple.getT2();
					if (f.getTechnologies().removeIf(e -> e.getId().equals(t.getId()))) {
						filterRepository.save(f);
						return ResponseEntity.ok().build();
					} else
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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
	
	public Mono<FilterDTO> createDescriptionWordToFilter(Long filterId, Long wordId){
		return Mono.justOrEmpty(filterRepository.findById(filterId))
				.zipWith(Mono.justOrEmpty(descriptionRepository.findById(wordId))).map(tuple -> {
					UserFilter f = tuple.getT1();
					DescriptionWord w = tuple.getT2();
					if (CollectionUtils.isEmpty(f.getDescriptions()))
						f.setDescriptions(Sets.newHashSet());
					f.getDescriptions().add(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				}).switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))));
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
		return Mono.justOrEmpty(filterRepository.findById(filterId))
		.zipWith(Mono.justOrEmpty(descriptionRepository.findById(wordId))).map(tuple -> {
			UserFilter filter = tuple.getT1();
			DescriptionWord word = tuple.getT2();
			if (filter.getDescriptions().removeIf(e -> e.equals(word))) {
				filterRepository.save(filter);
				return ResponseEntity.ok().build();
			}else
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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