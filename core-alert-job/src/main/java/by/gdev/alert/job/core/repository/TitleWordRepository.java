package by.gdev.alert.job.core.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.key.TitleWord;

public interface TitleWordRepository extends CrudRepository<TitleWord, Long> {

    Optional<TitleWord> findByNameAndUuid(String name, String uuid);

    Optional<TitleWord> findByNameAndSourceSite(String name, SourceSite sourceSite);

    @Query("select t from TitleWord t where t.sourceSite.id IN :sourceSite or t.uuid =:uuid order by t.counter desc")
    Page<TitleWord> findByNameAndSourceSiteInOrUuid(String uuid, Set<Long> sourceSite, Pageable p);

    @Query("select t from TitleWord t where (t.name like %:name% and t.sourceSite.id IN :sourceSite) or (t.name like %:name% and t.uuid =:uuid) order by t.counter desc")
    Page<TitleWord> findByNameAndSourceSiteInOrNameAndUuid(String name, String uuid, Set<Long> sourceSite, Pageable p);
}