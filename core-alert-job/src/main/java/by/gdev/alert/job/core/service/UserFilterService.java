package by.gdev.alert.job.core.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;

import by.gdev.alert.job.core.configuration.ApplicationProperty;
import by.gdev.alert.job.core.model.Filter;
import by.gdev.alert.job.core.model.FilterDTO;
import by.gdev.alert.job.core.model.KeyWord;
import by.gdev.alert.job.core.model.WordDTO;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.DescriptionWordPrice;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import by.gdev.alert.job.core.model.db.key.TitleWord;
import by.gdev.alert.job.core.model.db.key.Word;
import by.gdev.alert.job.core.repository.DescriptionWordPriceRepository;
import by.gdev.alert.job.core.repository.DescriptionWordRepository;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.TechnologyWordRepository;
import by.gdev.alert.job.core.repository.TitleWordRepository;
import by.gdev.alert.job.core.repository.UserFilterRepository;
import by.gdev.common.exeption.CollectionLimitExeption;
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
	private final DescriptionWordPriceRepository descriptionWordPriceRepository;

	private final ModelMapper mapper;
	private final ApplicationProperty appProperty;

	public Mono<Page<WordDTO>> showTitleWords(Long moduleId, String uuid, String name, Integer page) {
		return Mono.defer(() -> {
			OrderModules om = modulesRepository.findByIdAndUserUuidOneEagerSources(moduleId, uuid)
					.orElseThrow(() -> new ResourceNotFoundException("not found module with id " + moduleId));
			PageRequest p = PageRequest.of(page, appProperty.getWordsPerPage());
			Set<Long> sources = om.getSources().stream().map(e -> e.getId()).collect(Collectors.toSet());
			Page<? extends Word> pageWord = StringUtils.isEmpty(name)
					? titleRepository.findByNameAndSourceSiteInOrUuid(uuid, sources, p)
					: titleRepository.findByNameAndSourceSiteInOrNameAndUuid(name, uuid, sources, p);
			List<WordDTO> сollection = pageWord.stream().map(keyWordsToDTO())
					.collect(Collectors.toMap(w -> w.getName(), Function.identity(),
							(w1, w2) -> mergeDuplicates(w1, w2)))
					.values().stream().sorted(Comparator.comparing(WordDTO::getCounter).reversed())
					.collect(Collectors.toList());
			return Mono.just(new PageImpl<>(сollection, pageWord.getPageable(), pageWord.getTotalElements()));
		});
	}

	public Mono<ResponseEntity<WordDTO>> addTitleWord(KeyWord keyWord, String uuid) {
		return Mono.create(m -> {
			Optional<TitleWord> t = titleRepository.findByNameAndUuid(keyWord.getName(), uuid);
			if (t.isPresent()) {
				m.success(ResponseEntity.status(HttpStatus.CONFLICT).build());
			} else {
				TitleWord titleWord = new TitleWord();
				titleWord.setName(keyWord.getName());
				titleWord.setUuid(uuid);
				titleWord = titleRepository.save(titleWord);
				m.success(ResponseEntity.ok(mapper.map(titleWord, WordDTO.class)));
			}
		});
	}

	public Mono<Page<WordDTO>> showTechnologyWords(Long moduleId, String uuid, String name, Integer page) {
		return Mono.defer(() -> {
			OrderModules om = modulesRepository.findByIdAndUserUuidOneEagerSources(moduleId, uuid)
					.orElseThrow(() -> new ResourceNotFoundException("not found module with id " + moduleId));
			PageRequest p = PageRequest.of(page, appProperty.getWordsPerPage());
			Set<Long> sources = om.getSources().stream().map(e -> e.getId()).collect(Collectors.toSet());
			Page<? extends Word> pageWord = StringUtils.isEmpty(name)
					? technologyRepository.findByNameAndSourceSiteInOrUuid(uuid, sources, p)
					: technologyRepository.findByNameAndSourceSiteInOrNameAndUuid(name, uuid, sources, p);
			List<WordDTO> сollection = pageWord.stream().map(keyWordsToDTO())
					.collect(Collectors.toMap(w -> w.getName(), Function.identity(),
							(w1, w2) -> mergeDuplicates(w1, w2)))
					.values().stream().sorted(Comparator.comparing(WordDTO::getCounter).reversed())
					.collect(Collectors.toList());
			return Mono.just(new PageImpl<>(сollection, pageWord.getPageable(), pageWord.getTotalElements()));
		});
	}

	public Mono<ResponseEntity<WordDTO>> addTechnologyWord(KeyWord keyWord, String uuid) {
		return Mono.create(m -> {
			Optional<TechnologyWord> t = technologyRepository.findByNameAndUuid(keyWord.getName(), uuid);
			if (t.isPresent()) {
				m.success(ResponseEntity.status(HttpStatus.CONFLICT).build());
			} else {
				TechnologyWord technologyWordWord = new TechnologyWord();
				technologyWordWord.setName(keyWord.getName());
				technologyWordWord.setUuid(uuid);
				technologyWordWord = technologyRepository.save(technologyWordWord);
				m.success(ResponseEntity.ok(mapper.map(technologyWordWord, WordDTO.class)));
			}
		});
	}

	public Mono<Page<WordDTO>> showDescriptionWords(String name, Integer page) {
		return Mono.defer(() -> {
			PageRequest p = PageRequest.of(page, appProperty.getWordsPerPage());
			Page<DescriptionWord> word = StringUtils.isEmpty(name)
					? descriptionRepository.findAllHiddenIsFalseByOrderByCounterDesc(p)
					: descriptionRepository.findByHiddenIsFalseAndNameIsStartingWith(name, p);
			return Mono.just(word.map(keyWordsToDTO()));
		});
	}

	public Mono<ResponseEntity<WordDTO>> addDescriptionWord(KeyWord keyWord) {
		return Mono.create(m -> {
			Optional<DescriptionWord> t = descriptionRepository.findByName(keyWord.getName());
			if (t.isPresent()) {
				m.success(ResponseEntity.status(HttpStatus.CONFLICT).build());
			} else {
				DescriptionWord descriptionWord = new DescriptionWord();
				descriptionWord.setName(keyWord.getName());
				descriptionWord = descriptionRepository.save(descriptionWord);
				m.success(ResponseEntity.ok(mapper.map(descriptionWord, WordDTO.class)));
			}
		});
	}

	public Mono<ResponseEntity<WordDTO>> addDescriptionWordPrice(KeyWord keyWord, String uuid) {
		return Mono.create(m -> {
			Optional<DescriptionWordPrice> t = descriptionWordPriceRepository.findByNameAndUuid(keyWord.getName(),
					uuid);
			if (t.isPresent()) {
				m.success(ResponseEntity.status(HttpStatus.CONFLICT).build());
			} else {
				DescriptionWordPrice dwp = new DescriptionWordPrice();
				dwp.setName(keyWord.getName());
				dwp.setUuid(uuid);
				dwp = descriptionWordPriceRepository.save(dwp);
				m.success(ResponseEntity.ok(mapper.map(dwp, WordDTO.class)));
			}
		});
	}

	public Mono<Page<WordDTO>> showDescriptionWordPrice(String name, Integer page) {
		return Mono.defer(() -> {
			PageRequest p = PageRequest.of(page, 30);
			Page<DescriptionWordPrice> word = StringUtils.isEmpty(name)
					? descriptionWordPriceRepository.findAllByOrderByCounterDesc(p)
					: descriptionWordPriceRepository.findByNameIsStartingWith(name, p);
			return Mono.just(word.map(keyWordsToDTO()));
		});
	}

	public Flux<FilterDTO> showUserFilters(String uuid, Long moduleId) {
		return Flux.fromIterable(filterRepository.findAllByModuleIdAndUserUuid(moduleId, uuid))
				.map(e -> mapper.map(e, FilterDTO.class));
	}

	public Mono<FilterDTO> createUserFilter(String uuid, Long moduleId, Filter filter) {
		return Mono.justOrEmpty(modulesRepository.findByIdAndUserUuidOneEagerFilters(moduleId, uuid))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException("not found module with id " + moduleId)))
				.map(e -> {
					if (e.getFilters().size() >= appProperty.getLimitFilters()) {
						throw new CollectionLimitExeption("the limit for added filters");
					}
					if (filterRepository.existsByNameAndModule(filter.getName(), e)) {
						throw new ConflictExeption(String.format("filter with name %s exists", filter.getName()));
					}
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
					OrderModules m = e.getModule();
					if (!m.getCurrentFilter().getId().equals(filterId)) {
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
					}
					m.setCurrentFilter(null);
					modulesRepository.save(m);
					filterRepository.delete(e);
					return ResponseEntity.ok().build();
				});
	}

	public Mono<FilterDTO> replaceCurrentFilter(String uuid, Long moduleId, Long filterId) {
		Mono<UserFilter> filter = Mono.justOrEmpty(filterRepository.findByIdAndModuleId(filterId, moduleId))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found filter by module %s and filter", moduleId, filterId))));
		Mono<OrderModules> modules = Mono
				.justOrEmpty(modulesRepository.findByIdAndUserUuidOneEagerCurrentFilter(moduleId, uuid)).switchIfEmpty(
						Mono.error(new ResourceNotFoundException(String.format("not found module by %s", moduleId))));
		return Mono.zip(filter, modules).map(tuple -> {
			UserFilter f = tuple.getT1();
			OrderModules o = tuple.getT2();
			o.setCurrentFilter(f);
			modulesRepository.save(o);
			return mapper.map(f, FilterDTO.class);
		});
	}

	public FilterDTO showUserCurrentFilter(String uuid, Long moduleId) {
		return modulesRepository.findByIdAndUserUuidOneEagerCurrentFilter(moduleId, uuid)
				.filter(f -> Objects.nonNull(f.getCurrentFilter())).map(e -> {
					UserFilter currentFilter = e.getCurrentFilter();
					FilterDTO dto = mapper.map(currentFilter, FilterDTO.class);
					dto.setTitlesDTO(
							currentFilter.getTitles().stream().map(e1 -> mapper.map(e1, WordDTO.class)).toList());
					dto.setDescriptionsDTO(
							currentFilter.getDescriptions().stream().map(e1 -> mapper.map(e1, WordDTO.class)).toList());
					dto.setTechnologiesDTO(
							currentFilter.getTechnologies().stream().map(e1 -> mapper.map(e1, WordDTO.class)).toList());
					dto.setActivatedNegativeFilters(currentFilter.isActivatedNegativeFilters());
					dto.setNegativeTitlesDTO(currentFilter.getNegativeTitles().stream()
							.map(e1 -> mapper.map(e1, WordDTO.class)).toList());
					dto.setNegativeDescriptionsDTO(currentFilter.getNegativeDescriptions().stream()
							.map(e1 -> mapper.map(e1, WordDTO.class)).toList());
					dto.setNegativeTechnologiesDTO(currentFilter.getNegativeTechnologies().stream()
							.map(e1 -> mapper.map(e1, WordDTO.class)).toList());
					dto.setDescriptionWordPrice(currentFilter.getDescriptionWordPrice().stream()
							.map(e1 -> mapper.map(e1, WordDTO.class)).toList());
					return dto;
				}).orElse(null);
	}

	public Mono<FilterDTO> updateFilter(String uuid, Long moduleId, Long filterId, Filter filter) {
		return Mono.justOrEmpty(filterRepository.findByIdAndModuleIdAndUserUuid(filterId, moduleId, uuid))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found filter by module %s and filter %s", moduleId, filterId))))
				.map(e -> {
					if (Objects.nonNull(filter.getName())
							&& filterRepository.existsByNameAndModule(filter.getName(), e.getModule())) {
						throw new ConflictExeption(String.format("filter with name %s exists", filter.getName()));
					}
					UserFilter userFilter = e;
					mapper.map(filter, userFilter);
					userFilter = filterRepository.save(userFilter);
					return mapper.map(userFilter, FilterDTO.class);
				});
	}

	public Mono<FilterDTO> createTitleWordToFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerTitles(filterId))
				.zipWith(Mono.justOrEmpty(titleRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TitleWord w = tuple.getT2();
					if (f.getTitles().size() >= appProperty.getLimitKeyWords()) {
						throw new CollectionLimitExeption("the limit for added titles");
					}
					if (CollectionUtils.isEmpty(f.getTitles())) {
						f.setTitles(Sets.newHashSet());
					}
					if (f.getTitles().contains(w)) {
						throw new ConflictExeption("exists title word with name " + w.getName());
					}
					f.getTitles().add(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				});
	}

	public Mono<ResponseEntity<Void>> removeTitleWordFromFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerTitles(filterId))
				.zipWith(Mono.justOrEmpty(titleRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TitleWord t = tuple.getT2();
					if (f.getTitles().removeIf(e -> e.getId().equals(t.getId()))) {
						filterRepository.save(f);
						return ResponseEntity.ok().build();
					} else {
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
					}
				});
	}

	public Mono<FilterDTO> createTechnologyWordToFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerTechnologies(filterId))
				.zipWith(Mono.justOrEmpty(technologyRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TechnologyWord w = tuple.getT2();
					if (f.getTechnologies().size() >= appProperty.getLimitKeyWords()) {
						throw new CollectionLimitExeption("the limit for added technologes");
					}
					if (CollectionUtils.isEmpty(f.getTechnologies())) {
						f.setTechnologies(Sets.newHashSet());
					}
					if (f.getTechnologies().contains(w)) {
						throw new ConflictExeption("exists technology word with name " + w.getName());
					}
					f.getTechnologies().add(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				});
	}

	public Mono<ResponseEntity<Void>> removeTechnologyWordFromFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerTechnologies(filterId))
				.zipWith(Mono.justOrEmpty(technologyRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TechnologyWord t = tuple.getT2();
					if (f.getTechnologies().removeIf(e -> e.getId().equals(t.getId()))) {
						filterRepository.save(f);
						return ResponseEntity.ok().build();
					} else {
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
					}
				});
	}

	public Mono<FilterDTO> createDescriptionWordToFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerDescriptions(filterId))
				.zipWith(Mono.justOrEmpty(descriptionRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					DescriptionWord w = tuple.getT2();
					if (f.getDescriptions().size() >= appProperty.getLimitKeyWords()) {
						throw new CollectionLimitExeption("the limit for added descriptions");
					}
					if (CollectionUtils.isEmpty(f.getDescriptions())) {
						f.setDescriptions(Sets.newHashSet());
					}
					if (f.getDescriptions().contains(w)) {
						throw new ConflictExeption("exists description word with name " + w.getName());
					}
					f.getDescriptions().add(w);
					w.setCounter(w.getCounter() + 1L);
					descriptionRepository.save(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				});
	}

	public Mono<ResponseEntity<Void>> removeDescriptionWordFromFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerDescriptions(filterId))
				.zipWith(Mono.justOrEmpty(descriptionRepository.findById(wordId))).map(tuple -> {
					UserFilter filter = tuple.getT1();
					DescriptionWord word = tuple.getT2();
					if (filter.getDescriptions().removeIf(e -> e.equals(word))) {
						word.setCounter(word.getCounter() - 1L);
						descriptionRepository.save(word);
						filterRepository.save(filter);
						return ResponseEntity.ok().build();
					} else {
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
					}
				});
	}

	public Mono<FilterDTO> createTitleWordToNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerNegativeTitles(filterId))
				.zipWith(Mono.justOrEmpty(titleRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TitleWord w = tuple.getT2();
					if (f.getNegativeTitles().size() >= appProperty.getLimitKeyWords()) {
						throw new CollectionLimitExeption("the limit for added negative titles");
					}
					if (CollectionUtils.isEmpty(f.getNegativeTitles())) {
						f.setNegativeTitles(Sets.newHashSet());
					}
					if (f.getNegativeTitles().contains(w)) {
						throw new ConflictExeption("exists title word with name " + w.getName());
					}
					f.getNegativeTitles().add(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				});
	}

	public Mono<ResponseEntity<Void>> removeTitleWordFromNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerNegativeTitles(filterId))
				.zipWith(Mono.justOrEmpty(titleRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TitleWord t = tuple.getT2();
					if (f.getNegativeTitles().removeIf(e -> e.getId().equals(t.getId()))) {
						filterRepository.save(f);
						return ResponseEntity.ok().build();
					} else {
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
					}
				});
	}

	public Mono<FilterDTO> createTechnologyWordToNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerNegativeTechnologies(filterId))
				.zipWith(Mono.justOrEmpty(technologyRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TechnologyWord w = tuple.getT2();
					if (f.getNegativeTechnologies().size() >= appProperty.getLimitKeyWords()) {
						throw new CollectionLimitExeption("the limit for added negative technologes");
					}
					if (CollectionUtils.isEmpty(f.getNegativeTechnologies())) {
						f.setNegativeTechnologies(Sets.newHashSet());
					}
					if (f.getNegativeTechnologies().contains(w)) {
						throw new ConflictExeption("exists technology word with name " + w.getName());
					}
					f.getNegativeTechnologies().add(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				});
	}

	public Mono<ResponseEntity<Void>> removeTechnologyWordFromNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerNegativeTechnologies(filterId))
				.zipWith(Mono.justOrEmpty(technologyRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					TechnologyWord t = tuple.getT2();
					if (f.getNegativeTechnologies().removeIf(e -> e.getId().equals(t.getId()))) {
						filterRepository.save(f);
						return ResponseEntity.ok().build();
					} else {
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
					}
				});
	}

	public Mono<FilterDTO> createDescriptionWordToNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerNegativeDescriptions(filterId))
				.zipWith(Mono.justOrEmpty(descriptionRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					DescriptionWord w = tuple.getT2();
					if (f.getNegativeDescriptions().size() >= appProperty.getLimitKeyWords()) {
						throw new CollectionLimitExeption("the limit for added negative descriptions");
					}
					if (CollectionUtils.isEmpty(f.getNegativeDescriptions())) {
						f.setDescriptions(Sets.newHashSet());
					}
					if (f.getNegativeDescriptions().contains(w)) {
						throw new ConflictExeption("exists description word with name " + w.getName());
					}
					f.getNegativeDescriptions().add(w);
					w.setCounter(w.getCounter() + 1L);
					descriptionRepository.save(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				});
	}

	public Mono<ResponseEntity<Void>> removeDescriptionWordFromNegativeFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerNegativeDescriptions(filterId))
				.zipWith(Mono.justOrEmpty(descriptionRepository.findById(wordId))).map(tuple -> {
					UserFilter filter = tuple.getT1();
					DescriptionWord word = tuple.getT2();
					if (filter.getNegativeDescriptions().removeIf(e -> e.equals(word))) {
						filterRepository.save(filter);
						word.setCounter(word.getCounter() - 1L);
						descriptionRepository.save(word);
						return ResponseEntity.ok().build();
					} else {
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
					}
				});
	}

	// TODO in sql join DescriptionWordPrice
	public Mono<FilterDTO> createDescriptionWordPriceToFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerDescriptionWordPrice(filterId))
				.zipWith(Mono.justOrEmpty(descriptionWordPriceRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					DescriptionWordPrice w = tuple.getT2();
					if (f.getDescriptionWordPrice().size() >= appProperty.getLimitKeyWordsPrice()) {
						throw new CollectionLimitExeption("the limit for added description words price");
					}
					if (CollectionUtils.isEmpty(f.getDescriptionWordPrice())) {
						f.setDescriptionWordPrice(Sets.newHashSet());
					}
					if (f.getDescriptionWordPrice().contains(w)) {
						throw new ConflictExeption("exists description word price with name " + w.getName());
					}
					f.getDescriptionWordPrice().add(w);
					filterRepository.save(f);
					return mapper.map(f, FilterDTO.class);
				});
	}

	public Mono<ResponseEntity<Void>> removeDescriptionWordPriceFromFilter(Long filterId, Long wordId) {
		return Mono.justOrEmpty(filterRepository.findByIdOneEagerDescriptionWordPrice(filterId))
				.zipWith(Mono.justOrEmpty(descriptionWordPriceRepository.findById(wordId)))
				.switchIfEmpty(Mono.error(new ResourceNotFoundException(
						String.format("not found by filter id %s or word id %s", filterId, wordId))))
				.map(tuple -> {
					UserFilter f = tuple.getT1();
					DescriptionWordPrice t = tuple.getT2();
					if (f.getDescriptionWordPrice().removeIf(e -> e.getId().equals(t.getId()))) {
						filterRepository.save(f);
						return ResponseEntity.ok().build();
					} else {
						return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
					}
				});
	}

	private <T> Function<T, WordDTO> keyWordsToDTO() {
		return word -> mapper.map(word, WordDTO.class);
	};

	private WordDTO mergeDuplicates(WordDTO a, WordDTO b) {
		return new WordDTO(a.getId(), a.getName(), a.getCounter() + b.getCounter());
	}
}