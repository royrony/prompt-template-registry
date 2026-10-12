package dev.ssec2.ptr.repository;

import dev.ssec2.ptr.domain.PromptTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T3 - repository behavior against H2 (spec FR-004 versioning, FR-008 reads).
 */
@DataJpaTest
class PromptTemplateRepositoryTest {

    @Autowired
    private PromptTemplateRepository repository;

    @Test
    void latestVersion_isHighestForName() {
        repository.save(new PromptTemplate("greeting", 1, "Hello {{name}} v1"));
        repository.save(new PromptTemplate("greeting", 2, "Hello {{name}} v2"));

        PromptTemplate latest = repository.findTopByNameOrderByVersionDesc("greeting").orElseThrow();

        assertThat(latest.getVersion()).isEqualTo(2);
        assertThat(latest.getBody()).isEqualTo("Hello {{name}} v2");
    }

    @Test
    void allVersions_areReturnedOldestFirst() {
        repository.save(new PromptTemplate("greeting", 1, "v1"));
        repository.save(new PromptTemplate("greeting", 2, "v2"));

        List<PromptTemplate> versions = repository.findByNameOrderByVersionAsc("greeting");

        assertThat(versions).extracting(PromptTemplate::getVersion).containsExactly(1, 2);
    }

    @Test
    void findMaxVersion_returnsHighest_orNullForUnknown() {
        repository.save(new PromptTemplate("greeting", 1, "v1"));
        repository.save(new PromptTemplate("greeting", 2, "v2"));

        assertThat(repository.findMaxVersion("greeting")).isEqualTo(2);
        assertThat(repository.findMaxVersion("does-not-exist")).isNull();
    }

    @Test
    void unknownName_hasNoLatestAndEmptyVersions() {
        assertThat(repository.findTopByNameOrderByVersionDesc("missing")).isEmpty();
        assertThat(repository.findByNameOrderByVersionAsc("missing")).isEmpty();
    }

    @Test
    void latestPerName_returnsEachNameOnceAtHighestVersion() {
        repository.save(new PromptTemplate("greeting", 1, "g1"));
        repository.save(new PromptTemplate("greeting", 2, "g2"));
        repository.save(new PromptTemplate("farewell", 1, "f1"));

        List<PromptTemplate> latest = repository.findLatestPerName();

        assertThat(latest).extracting(PromptTemplate::getName, PromptTemplate::getVersion)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("greeting", 2),
                        org.assertj.core.groups.Tuple.tuple("farewell", 1));
    }

    @Test
    void createdAt_isPopulatedOnPersist() {
        PromptTemplate saved = repository.save(new PromptTemplate("greeting", 1, "v1"));
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void duplicateNameVersion_isRejectedByUniqueConstraint() {
        repository.save(new PromptTemplate("greeting", 1, "first"));
        assertThatThrownBy(() -> repository.saveAndFlush(new PromptTemplate("greeting", 1, "dup")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
