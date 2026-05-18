package by.gdev.alert.job.core.service.change.dto;

import java.util.Objects;

public record RemovedCategoryInfo(String categoryName, String subcategoryName) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemovedCategoryInfo other)) return false;
        return Objects.equals(categoryName, other.categoryName)
                && Objects.equals(subcategoryName, other.subcategoryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryName, subcategoryName);
    }
}

