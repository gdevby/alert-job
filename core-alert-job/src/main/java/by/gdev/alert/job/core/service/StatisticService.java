package by.gdev.alert.job.core.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import by.gdev.alert.job.core.model.db.key.TitleWord;
import by.gdev.alert.job.core.repository.DescriptionWordRepository;
import by.gdev.alert.job.core.repository.SourceSiteRepository;
import by.gdev.alert.job.core.repository.TechnologyWordRepository;
import by.gdev.alert.job.core.repository.TitleWordRepository;
import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.model.SourceSiteDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private static final String REGX = "[^A-Za-zА-Яа-я]+";

    private final TitleWordRepository titleRepository;
    private final DescriptionWordRepository descriptionRepository;
    private final TechnologyWordRepository technologyRepository;

    private final SourceSiteRepository sourceSiteRepository;

    public void statisticTitleWord(String title, SourceSiteDTO sourceSite) {
	SourceSite site = findSourceSite(sourceSite);
	Stream.of(title.split(REGX)).map(String::toLowerCase).forEach(e -> {
	    Optional<TitleWord> titleWord = titleRepository.findByNameAndSourceSite(e, site);
	    if (titleWord.isPresent()) {
		TitleWord tw = titleWord.get();
		Long counter = tw.getCounter() + 1L;
		tw.setCounter(counter);
		titleRepository.save(tw);
	    } else {
		TitleWord tw = new TitleWord();
		tw.setCounter(1L);
		tw.setName(e);
		tw.setSourceSite(site);
		titleRepository.save(tw);
	    }
	});
    }

    public void statisticDescriptionWord(String description) {
	Stream.of(description.split(REGX)).map(String::toLowerCase).forEach(e -> {
	    Optional<DescriptionWord> descriptionWord = descriptionRepository.findByName(e);
	    if (descriptionWord.isPresent()) {
		DescriptionWord dw = descriptionWord.get();
		Long counter = dw.getCounter() + 1L;
		dw.setCounter(counter);
		descriptionRepository.save(dw);
	    } else {
		DescriptionWord dw = new DescriptionWord();
		dw.setCounter(1L);
		dw.setName(e);
		descriptionRepository.save(dw);
	    }
	});
    }

    public void statisticTechnologyWord(List<String> technologies, SourceSiteDTO sourceSite) {
	if (Objects.isNull(technologies))
	    return;
	SourceSite site = findSourceSite(sourceSite);
	technologies.forEach(e -> {
	    Optional<TechnologyWord> technoWord = technologyRepository.findByName(e);
	    if (technoWord.isPresent()) {
		TechnologyWord tw = technoWord.get();
		Long counter = tw.getCounter() + 1L;
		tw.setCounter(counter);
		technologyRepository.save(tw);
	    } else {
		TechnologyWord tw = new TechnologyWord();
		tw.setCounter(1L);
		tw.setName(e);
		tw.setSourceSite(site);
		technologyRepository.save(tw);
	    }
	});
    }

    private SourceSite findSourceSite(SourceSiteDTO sourceSite) {
	if (Objects.isNull(sourceSite.getSubCategory())) {
	    return sourceSiteRepository.findBySourceSubCategoryIsNull(sourceSite.getSource(), sourceSite.getCategory())
		    .orElseThrow(() -> new ResourceNotFoundException(String.format("don't found source by  %s %s",
			    sourceSite.getSource(), sourceSite.getCategory())));
	} else
	    return sourceSiteRepository
		    .findBySource(sourceSite.getSource(), sourceSite.getCategory(), sourceSite.getSubCategory())
		    .orElseThrow(() -> new ResourceNotFoundException(String.format("don't found source by  %s %s %s",
			    sourceSite.getSource(), sourceSite.getCategory(), sourceSite.getSubCategory())));
    }
}