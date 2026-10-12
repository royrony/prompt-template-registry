package dev.ssec2.ptr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One immutable version of a named prompt template.
 *
 * <p>Versioning is insert-only: re-registering a name inserts a new row with the next version
 * rather than mutating an existing one, so every historical version stays byte-identical
 * (spec FR-004 / SC-003). {@code declaredVariables} is intentionally NOT stored - it is derived
 * from {@link #body} by the {@link VariableParser} on read, so it can never drift from the body.
 */
@Entity
@Table(
        name = "prompt_template",
        uniqueConstraints = @UniqueConstraint(name = "uq_prompt_template_name_version",
                columnNames = {"name", "version"}))
@Getter
@Setter
@NoArgsConstructor
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, length = 10_000)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PromptTemplate(String name, int version, String body) {
        this.name = name;
        this.version = version;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
