package com.example.sds.dto;

import com.example.sds.domain.Classification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateDocumentRequest(

        @NotBlank
        String title,

        String description,

        @NotNull
        Classification classification
) {}
