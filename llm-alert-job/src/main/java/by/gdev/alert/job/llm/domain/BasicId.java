package by.gdev.alert.job.llm.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@MappedSuperclass
@Data
@EqualsAndHashCode
@ToString
public class BasicId {

    @Access(AccessType.PROPERTY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}
