package by.gdev.alert.job.parser.domain.db;

import jakarta.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class FlCategory extends BasicId {
	String nativeLocName;
}
