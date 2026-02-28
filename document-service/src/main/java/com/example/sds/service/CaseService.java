package com.example.sds.service;

import com.example.sds.domain.CaseStatus;
import com.example.sds.domain.DocumentCase;
import com.example.sds.dto.CaseResponse;
import com.example.sds.dto.CreateCaseRequest;
import com.example.sds.repository.CaseRepository;
import com.example.sds.security.OrganisationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class CaseService {

    private final CaseRepository caseRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public CaseService(CaseRepository caseRepository,
                       AuthorizationService authorizationService,
                       AuditService auditService) {
        this.caseRepository = caseRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(UUID id, OrganisationPrincipal principal) {
        DocumentCase documentCase = requireCase(id, principal.organisationId());
        authorizationService.assertReadAccess(documentCase, principal);
        return CaseResponse.from(documentCase);
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> listCases(OrganisationPrincipal principal) {
        return caseRepository.findAllByOrganisationId(principal.organisationId()).stream()
                .map(CaseResponse::from)
                .toList();
    }

    public CaseResponse createCase(CreateCaseRequest request, OrganisationPrincipal principal) {
        DocumentCase documentCase = DocumentCase.builder()
                .organisationId(principal.organisationId())
                .title(request.title())
                .description(request.description())
                .status(CaseStatus.OPEN)
                .createdBy(principal.userId())
                .build();

        CaseResponse response = CaseResponse.from(caseRepository.save(documentCase));
        auditService.log(AuditAction.CASE_CREATE, principal, response.id());
        return response;
    }

    public CaseResponse closeCase(UUID id, OrganisationPrincipal principal) {
        DocumentCase documentCase = requireCase(id, principal.organisationId());
        authorizationService.assertCloseAccess(documentCase, principal);

        if (documentCase.getStatus() == CaseStatus.CLOSED) {
            throw new IllegalStateException("Case is already CLOSED");
        }

        documentCase.setStatus(CaseStatus.CLOSED);
        CaseResponse response = CaseResponse.from(caseRepository.save(documentCase));
        auditService.log(AuditAction.CASE_CLOSED, principal, id);
        return response;
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private DocumentCase requireCase(UUID id, String organisationId) {
        return caseRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Case not found: " + id));
    }
}
