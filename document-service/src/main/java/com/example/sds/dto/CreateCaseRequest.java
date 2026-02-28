package com.example.sds.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCaseRequest(

        @NotBlank
        String title,

        String description
) {}
