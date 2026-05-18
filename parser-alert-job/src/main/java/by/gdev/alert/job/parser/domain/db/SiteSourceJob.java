package by.gdev.alert.job.parser.domain.db;

import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class SiteSourceJob extends BasicId {

    private String name;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "siteSourceJob")
    private Set<Category> categories;
    private String parsedURI;
    private boolean parse;
    private boolean active;

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SiteSourceJob)) return false;
        return Objects.equals(getId(), ((SiteSourceJob) o).getId());
    }
}
