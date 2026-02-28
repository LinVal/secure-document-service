package com.example.sds.controller;

import com.example.sds.dto.CaseResponse;
import com.example.sds.dto.CreateCaseRequest;
import com.example.sds.security.OrganisationPrincipal;
import com.example.sds.service.CaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Thin controller – no business or security logic here.
 * All decisions are delegated to {@link CaseService} and
 * {@link com.example.sds.service.AuthorizationService}.
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public List<CaseResponse> listCases(
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return caseService.listCases(principal);
    }

    @GetMapping("/{id}")
    public CaseResponse getCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return caseService.getCase(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseResponse createCase(
            @RequestBody @Valid CreateCaseRequest request,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return caseService.createCase(request, principal);
    }

    /** Only ADMIN within the organisation may call this – enforced in CaseService. */
    @PutMapping("/{id}/close")
    public CaseResponse closeCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return caseService.closeCase(id, principal);
    }
}
