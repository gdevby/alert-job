package by.gdev.alert.job.core.repository;

import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface TechnologyWordRepository extends CrudRepository<TechnologyWord, Long> {

    Optional<TechnologyWord> findByNameAndUuid(String name, String uuid);

    Optional<TechnologyWord> findByNameAndSourceSite(String name, SourceSite sourceSite);

    @Query("select t from TechnologyWord t where (t.sourceSite.id IN :sourceSite and t.hidden = false) "
	    + "or (t.uuid =:uuid and t.hidden = false) order by t.counter desc")
    Page<TechnologyWord> findByNameAndSourceSiteInOrUuid(@Param("uuid")String uuid, @Param("sourceSite")Set<Long> sourceSite, Pageable p);

    @Query("select t from TechnologyWord t where (t.name like concat('%', :name, '%') and t.sourceSite.id IN :sourceSite and t.hidden = false) "
	    + "or (t.name like concat('%', :name, '%') and t.uuid =:uuid and t.hidden = false)  order by t.counter desc")
    Page<TechnologyWord> findByNameAndSourceSiteInOrNameAndUuid(@Param("name")String name, @Param("uuid")String uuid, @Param("sourceSite")Set<Long> sourceSite,
	    Pageable p);
}