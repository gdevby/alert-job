package by.gdev.alert.job.llm.domain;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class LlmUser extends BasicId {
    private String uuid;
}

