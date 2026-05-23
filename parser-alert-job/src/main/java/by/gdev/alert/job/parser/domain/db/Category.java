package by.gdev.alert.job.parser.domain.db;

import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "parser_category")
public class Category extends BasicId {

	private String name;
	private String nativeLocName;
	private String link;
	private boolean parse;

    @Column(name = "order_index")
    private Integer order;

    @ManyToOne(cascade = CascadeType.ALL , fetch = FetchType.LAZY)
	private SiteSourceJob siteSourceJob;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "category")
	private Set<Subcategory> subCategories;

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        return Objects.equals(getId(), ((Category) o).getId());
    }

}
