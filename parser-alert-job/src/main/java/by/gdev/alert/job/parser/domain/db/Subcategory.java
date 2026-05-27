package by.gdev.alert.job.parser.domain.db;

import jakarta.persistence.*;

import lombok.Data;

import java.util.Objects;

@Data
@Entity(name = "parser_sub_category")
public class Subcategory extends BasicId {
	
	private String name;
	private String link;
	private boolean parse;
	private String nativeLocName;

    @Column(name = "order_index")
    private Integer order;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Category category;

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subcategory)) return false;
        return Objects.equals(getId(), ((Subcategory) o).getId());
    }

}
