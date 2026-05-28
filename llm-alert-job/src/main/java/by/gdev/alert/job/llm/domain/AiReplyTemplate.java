package by.gdev.alert.job.llm.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AiReplyTemplate extends BasicId {

    @ManyToOne
    private LlmUser user;

    private Long moduleId;

    @Lob
    private String htmlTemplate;
}

