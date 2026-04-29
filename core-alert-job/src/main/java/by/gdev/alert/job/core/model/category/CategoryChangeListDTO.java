package by.gdev.alert.job.core.model.category;

import java.util.List;

public record CategoryChangeListDTO(
        List<CategoryChangeDTO> changes
) {}
