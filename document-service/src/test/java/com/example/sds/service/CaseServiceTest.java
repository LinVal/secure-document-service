package com.example.sds.service;

import com.example.sds.domain.CaseStatus;
import com.example.sds.domain.DocumentCase;
import com.example.sds.dto.CaseResponse;
import com.example.sds.dto.CreateCaseRequest;
import com.example.sds.repository.CaseRepository;
import com.example.sds.security.OrganisationPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    private final AuthorizationService authorizationService = new AuthorizationService();
    private final AuditService auditService = new AuditService();
    private final CaseRepository caseRepository = mock(CaseRepository.class);

    private final CaseService caseService = new CaseService(caseRepository, authorizationService, auditService);

    // ── createCase ────────────────────────────────────────────────────────────

    @Test
    void createCase_shouldSetOrganisationFromPrincipal_andStatusOpen() {
        when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        CreateCaseRequest request = new CreateCaseRequest("New case", null);

        CaseResponse response = caseService.createCase(request, principal);

        assertThat(response.status()).isEqualTo(CaseStatus.OPEN);
        assertThat(response.organisationId()).isEqualTo("organisation-A");
    }

    @Test
    void createCase_shouldUseCreatedByFromPrincipal() {
        when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganisationPrincipal principal = principal("user-42", "organisation-A", "USER");
        CreateCaseRequest request = new CreateCaseRequest("New case", null);

        CaseResponse response = caseService.createCase(request, principal);

        assertThat(response.createdBy()).isEqualTo("user-42");
    }

    // ── getCase ──────────────────────────────────────────────────────────────

    @Test
    void getCase_shouldReturnCase_whenFound() {
        UUID id = UUID.randomUUID();
        DocumentCase existingCase = openCase(id);
        when(caseRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(existingCase));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        CaseResponse response = caseService.getCase(id, principal);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(CaseStatus.OPEN);
    }

    @Test
    void getCase_shouldReturn404_whenCaseBelongsToOtherOrganisation() {
        UUID id = UUID.randomUUID();
        when(caseRepository.findByIdAndOrganisationId(id, "organisation-B"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> caseService.getCase(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── listCases ─────────────────────────────────────────────────────────────

    @Test
    void listCases_shouldReturnAllCasesForOrganisation() {
        when(caseRepository.findAllByOrganisationId("organisation-A"))
                .thenReturn(List.of(openCase(UUID.randomUUID()), openCase(UUID.randomUUID())));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        List<CaseResponse> result = caseService.listCases(principal);

        assertThat(result).hasSize(2)
                .allMatch(c -> "organisation-A".equals(c.organisationId()));
    }

    @Test
    void listCases_shouldReturnEmpty_whenOrganisationHasNoCases() {
        when(caseRepository.findAllByOrganisationId("organisation-A")).thenReturn(List.of());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThat(caseService.listCases(principal)).isEmpty();
    }

    // ── closeCase ────────────────────────────────────────────────────────────

    @Test
    void closeCase_shouldDenyAccess_forUserRole() {
        UUID id = UUID.randomUUID();
        DocumentCase openCase = openCase(id);
        when(caseRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(openCase));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> caseService.closeCase(id, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void closeCase_shouldDenyAccess_whenNoRoles() {
        UUID id = UUID.randomUUID();
        DocumentCase openCase = openCase(id);
        when(caseRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(openCase));

        OrganisationPrincipal principal = principal("user1", "organisation-A" /* no roles */);

        assertThatThrownBy(() -> caseService.closeCase(id, principal))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void closeCase_shouldSucceed_forAdminRole() {
        UUID id = UUID.randomUUID();
        DocumentCase openCase = openCase(id);
        when(caseRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(openCase));
        when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        CaseResponse response = caseService.closeCase(id, principal);

        assertThat(response.status()).isEqualTo(CaseStatus.CLOSED);
    }

    @Test
    void closeCase_shouldThrow_whenCaseAlreadyClosed() {
        UUID id = UUID.randomUUID();
        DocumentCase closedCase = DocumentCase.builder()
                .id(id)
                .organisationId("organisation-A")
                .title("Case")
                .status(CaseStatus.CLOSED)
                .createdBy("test")
                .build();
        when(caseRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(closedCase));

        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        assertThatThrownBy(() -> caseService.closeCase(id, principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already CLOSED");
    }

    @Test
    void closeCase_shouldReturn404_whenCaseBelongsToOtherOrganisation() {
        UUID id = UUID.randomUUID();
        when(caseRepository.findByIdAndOrganisationId(id, "organisation-B"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("admin1", "organisation-B", "ADMIN");

        assertThatThrownBy(() -> caseService.closeCase(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void closeCase_shouldDenyAccess_forAdminFromDifferentOrganisation() {
        // An ADMIN in org-B must not reach close-logic for org-A cases.
        // The repository naturally returns empty for a cross-org lookup → 404.
        UUID id = UUID.randomUUID();
        when(caseRepository.findByIdAndOrganisationId(id, "organisation-B"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("admin-evil", "organisation-B", "ADMIN");

        assertThatThrownBy(() -> caseService.closeCase(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static OrganisationPrincipal principal(String userId, String organisationId, String... roles) {
        var authorities = Stream.of(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                .toList();
        return new OrganisationPrincipal(userId, organisationId, authorities);
    }

    private static DocumentCase openCase(UUID id) {
        return DocumentCase.builder()
                .id(id)
                .organisationId("organisation-A")
                .title("Test case")
                .status(CaseStatus.OPEN)
                .createdBy("test-user")
                .build();
    }
}
