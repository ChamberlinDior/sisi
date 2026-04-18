package com.pnis.backend.intelligence.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Alias / variante orthographique d'une entité §7.4 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "entity_aliases", indexes = {
        @Index(name = "idx_alias_entity", columnList = "entity_id"),
        @Index(name = "idx_alias_value",  columnList = "alias_value")
})
public class EntityAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    private EntityRecord entity;

    @Column(name = "alias_value", nullable = false, length = 300)
    private String aliasValue;

    /** PHONETIC, ORTHOGRAPHIC, NICKNAME, FORMER_NAME, DOCUMENT_ID */
    @Column(name = "alias_type", length = 30)
    private String aliasType;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); }
}
