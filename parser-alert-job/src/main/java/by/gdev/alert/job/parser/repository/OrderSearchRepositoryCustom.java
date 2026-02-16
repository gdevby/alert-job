package by.gdev.alert.job.parser.repository;

import java.util.List;

public interface OrderSearchRepositoryCustom {
    List<Object[]> searchOrdersDynamic(List<Long> siteIds, String mode, List<String> words, int offset, int size);
    long countOrdersDynamic(List<Long> siteIds, String mode, List<String> words);
}
