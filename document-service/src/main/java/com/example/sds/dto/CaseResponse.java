package com.example.sds.dto;

import com.example.sds.domain.CaseStatus;
import com.example.sds.domain.DocumentCase;

import java.time.Instant;
import java.util.UUID;

public record CaseResponse(
        UUID id,
        String organisationId,
        String title,
        String description,
        CaseStatus status,
        String createdBy,
        Instant createdAt
) {
    public static CaseResponse from(DocumentCase c) {
        return new CaseResponse(
                c.getId(),
                c.getOrganisationId(),
                c.getTitle(),
                c.getDescription(),
                c.getStatus(),
                c.getCreatedBy(),
                c.getCreatedAt()
        );
    }
}
