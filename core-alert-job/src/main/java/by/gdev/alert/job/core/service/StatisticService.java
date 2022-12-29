package by.gdev.alert.job.core.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import by.gdev.alert.job.core.model.key.DescriptionWord;
import by.gdev.alert.job.core.model.key.TechnologyWord;
import by.gdev.alert.job.core.model.key.TitleWord;
import by.gdev.alert.job.core.repository.DescriptionWordRepository;
import by.gdev.alert.job.core.repository.TechnologyWordRepository;
import by.gdev.alert.job.core.repository.TitleWordRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticService {
	
	private static final String REGX = "[^A-Za-zА-Яа-я]+";
	
	private final TitleWordRepository titleRepository;
	private final DescriptionWordRepository descriptionRepository;
	private final TechnologyWordRepository technologyRepository;
	
	public void statisticTitleWord(String title) {
		Stream.of(title.split(REGX)).map(String::toLowerCase).forEach(e -> {
			Optional<TitleWord> titleWord = titleRepository.findByName(e);
			if (titleWord.isPresent()) {
				TitleWord tw = titleWord.get();
				Long counter = tw.getCounter() +1L;
				tw.setCounter(counter);
				titleRepository.save(tw);
			}else {
				TitleWord tw = new TitleWord();
				tw.setCounter(1L);
				tw.setName(e);
				titleRepository.save(tw);
			}
		});
	}
	
	public void statisticDescriptionWord(String description) {
		Stream.of(description.split(REGX)).map(String::toLowerCase).forEach(e -> {
			Optional<DescriptionWord> descriptionWord = descriptionRepository.findByName(e);
			if (descriptionWord.isPresent()) {
				DescriptionWord dw = descriptionWord.get();
				Long counter = dw.getCounter() +1L;
				dw.setCounter(counter);
				descriptionRepository.save(dw);
			}else {
				DescriptionWord dw = new DescriptionWord();
				dw.setCounter(1L);
				dw.setName(e);
				descriptionRepository.save(dw);
			}
		});
	}
	
	public void statisticTechnologyWord(List<String> technologies) {
		technologies.forEach(e -> {
			Optional<TechnologyWord> technoWord = technologyRepository.findByName(e);
			if (technoWord.isPresent()) {
				TechnologyWord tw = technoWord.get();
				Long counter = tw.getCounter() +1L;
				tw.setCounter(counter);
				technologyRepository.save(tw);
			}else {
				TechnologyWord tw = new TechnologyWord();
				tw.setCounter(1L);
				tw.setName(e);
				technologyRepository.save(tw);
			}
		});
	}
}