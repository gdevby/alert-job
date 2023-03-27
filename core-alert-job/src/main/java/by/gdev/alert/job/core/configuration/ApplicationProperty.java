package by.gdev.alert.job.core.configuration;

import org.springframework.beans.factory.annotation.Value;

import lombok.Data;

@Data
public class ApplicationProperty {

    @Value("${limit.filters}")
    private Long limitFilters;

    @Value("${limit.key.words}")
    private Long limitKeyWords;

    @Value("${limit.key.words.price}")
    private Long limitKeyWordsPrice;

    @Value("${limit.key.words.price}")
    private Long wordPerPage;

    @Value("${limit.sources}")
    private Long limitSources;

    @Value("${limit.orders}")
    private Long limitOrders;

    @Value("${words.elements.per.page}")
    private Integer wordsPerPage;
}
