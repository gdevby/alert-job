package by.gdev.alert.job.parser.service.order.search.converter.impl;

import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.SubCategoryRepository;
import by.gdev.alert.job.parser.service.order.search.converter.Converter;
import by.gdev.common.model.OrderSearchDTO;
import by.gdev.common.model.PriceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class SearchRowToOrderDTOConverter implements Converter<Object[], OrderSearchDTO> {

    private final Converter<Price, PriceDTO> priceConverter;

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    enum OrderColumn {
        ID(0),
        TITLE(1),
        MESSAGE(2),
        LINK(3),
        DATE_TIME(4),
        PRICE(5),
        CATEGORY_ID(6),
        SUBCATEGORY_ID(7);

        private final int index;

        OrderColumn(int index) {
            this.index = index;
        }

        public int idx() {
            return index;
        }
    }


    @Override
    public OrderSearchDTO convert(Object[] row) {
        OrderSearchDTO dto = new OrderSearchDTO();

        dto.setTitle(asString(row[OrderColumn.TITLE.idx()]));
        dto.setMessage(asString(row[OrderColumn.MESSAGE.idx()]));
        dto.setLink(asString(row[OrderColumn.LINK.idx()]));
        dto.setDateTime(asDate(row[OrderColumn.DATE_TIME.idx()]));
        dto.setPrice(priceConverter.convert(parsePrice((asString(row[5])))));

        // ---- CATEGORY ----
        Long categoryId = asLong(row[OrderColumn.CATEGORY_ID.idx()]);
        if (categoryId != null) {
            categoryRepository.findById(categoryId).ifPresent(cat -> dto.setCategory(cat.getNativeLocName()));
        }

        // ---- SUBCATEGORY ----
        Long subCategoryId = asLong(row[OrderColumn.SUBCATEGORY_ID.idx()]);
        if (subCategoryId != null) {
            subCategoryRepository.findById(subCategoryId) .ifPresent(sub -> dto.setSubCategory(sub.getNativeLocName()));
        }
        return dto;
    }

    public Price parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();

        String numberOnly = trimmed.replaceAll("[^0-9]", "");
        if (numberOnly.isEmpty()) {
            return null;
        }

        int value = Integer.parseInt(numberOnly);
        return new Price(trimmed, value);
    }

    private String asString(Object o) {
        return o != null ? o.toString() : null;
    }

    private Long asLong(Object o) {
        return o != null ? ((Number) o).longValue() : null;
    }

    private Date asDate(Object o) {
        if (o == null) return null;

        if (o instanceof Timestamp ts) {
            return new Date(ts.getTime());
        }

        if (o instanceof Date d) {
            return d;
        }

        if (o instanceof LocalDateTime ldt) {
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        }
        return null;
    }

}

