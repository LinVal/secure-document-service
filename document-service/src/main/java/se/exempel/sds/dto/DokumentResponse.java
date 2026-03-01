package se.exempel.sds.dto;

import se.exempel.sds.domain.Classification;
import se.exempel.sds.domain.Dokument;

import java.time.Instant;
import java.util.UUID;

public record DokumentResponse(
        UUID id,
        String organisationId,
        String title,
        String description,
        String filename,
        String contentType,
        Long fileSize,
        Classification classification,
        UUID arendeId,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public static DokumentResponse from(Dokument doc) {
        // Accessing getId() on a LAZY Hibernate proxy is safe – Hibernate
        // stores the FK value in the proxy without issuing a SELECT.
        UUID arendeId = doc.getLinkedArende() != null
                ? doc.getLinkedArende().getId()
                : null;

        return new DokumentResponse(
                doc.getId(),
                doc.getOrganisationId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getFilename(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getClassification(),
                arendeId,
                doc.getCreatedBy(),
                doc.getCreatedAt(),
                doc.getUpdatedBy(),
                doc.getUpdatedAt()
        );
    }
}
