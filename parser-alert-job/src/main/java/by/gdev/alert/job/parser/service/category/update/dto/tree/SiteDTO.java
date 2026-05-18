package by.gdev.alert.job.parser.service.category.update.dto.tree;

import lombok.Data;

import java.util.List;

@Data
public class SiteDTO {
    private Long id;
    private String name;
    private List<CategoryDTO> categories;
}


