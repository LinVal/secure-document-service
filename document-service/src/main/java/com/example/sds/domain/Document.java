package com.example.sds.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

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

    /** Original filename as uploaded – stored separately from the physical path. */
    private String filename;

    private String contentType;

    private Long fileSize;

    /**
     * Internal storage path relative to the configured upload directory.
     * Never included in API responses – clients never learn where files live on disk.
     */
    @Column(name = "storage_path")
    private String storagePath;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Classification classification;

    /**
     * Optional link to a case. Document and case must share the same organisationId –
     * enforced in the service layer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private DocumentCase linkedCase;

    @NotBlank
    @Column(nullable = false, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private String updatedBy;

    @LastModifiedDate
    private Instant updatedAt;
}
