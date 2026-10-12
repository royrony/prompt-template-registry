package dev.ssec2.ptr.repository;

import dev.ssec2.ptr.domain.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for {@link PromptTemplate} versions. All reads are version-aware; there is no
 * update path (versioning is insert-only).
 */
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    /** Latest version of a given name, or empty if the name was never registered. */
    Optional<PromptTemplate> findTopByNameOrderByVersionDesc(String name);

    /** All versions of a name, oldest first. Empty list if the name is unknown. */
    List<PromptTemplate> findByNameOrderByVersionAsc(String name);

    /** Highest existing version for a name, or null if none exists yet. */
    @Query("select max(t.version) from PromptTemplate t where t.name = :name")
    Integer findMaxVersion(String name);

    /**
     * One row per distinct name - the highest-version row for each. Used by the "list all
     * templates" endpoint so each name appears once at its latest version.
     */
    @Query("""
            select t from PromptTemplate t
            where t.version = (select max(t2.version) from PromptTemplate t2 where t2.name = t.name)
            order by t.name asc
            """)
    List<PromptTemplate> findLatestPerName();
}
