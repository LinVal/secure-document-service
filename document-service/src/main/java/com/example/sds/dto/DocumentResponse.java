package com.example.sds.dto;

import com.example.sds.domain.Classification;
import com.example.sds.domain.Document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String organisationId,
        String title,
        String description,
        String filename,
        String contentType,
        Long fileSize,
        Classification classification,
        UUID caseId,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public static DocumentResponse from(Document doc) {
        // Accessing getId() on a LAZY Hibernate proxy is safe – Hibernate
        // stores the FK value in the proxy without issuing a SELECT.
        UUID caseId = doc.getLinkedCase() != null
                ? doc.getLinkedCase().getId()
                : null;

        return new DocumentResponse(
                doc.getId(),
                doc.getOrganisationId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getFilename(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getClassification(),
                caseId,
                doc.getCreatedBy(),
                doc.getCreatedAt(),
                doc.getUpdatedBy(),
                doc.getUpdatedAt()
        );
    }
}
