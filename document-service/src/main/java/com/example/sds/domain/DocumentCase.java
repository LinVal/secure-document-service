package com.example.sds.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a case that groups related documents within a single organisation.
 * Table name is "document_cases" because CASE is a reserved SQL keyword.
 */
@Entity
@Table(name = "document_cases")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String organisationId;

    @NotBlank
    @Column(nullable = false)
    private String title;

    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @NotBlank
    @Column(nullable = false, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
