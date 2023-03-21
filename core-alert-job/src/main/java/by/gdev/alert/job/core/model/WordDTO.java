package by.gdev.alert.job.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordDTO {

    private Long id;
    private String name;
    private Long counter;
}
