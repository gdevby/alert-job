package by.gdev.alert.job.parser.service.order.search.converter;

import java.util.List;
import java.util.Objects;

public interface Converter<S, T> {

    T convert(S source);

    default List<T> convertAll(List<S> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }

        return sources.stream()
                .filter(Objects::nonNull)
                .map(this::convert)
                .toList();
    }
}
