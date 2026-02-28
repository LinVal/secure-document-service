package com.example.sds.dto;

import com.example.sds.domain.Classification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDocumentRequest(

        @NotBlank
        String title,

        String description,

        String filename,

        String contentType,

        Long fileSize,

        @NotNull
        Classification classification,

        UUID caseId
) {}
