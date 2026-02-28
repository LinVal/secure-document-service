package com.example.sds.service;

import com.example.sds.domain.CaseStatus;
import com.example.sds.domain.Classification;
import com.example.sds.domain.Document;
import com.example.sds.domain.DocumentCase;
import com.example.sds.security.OrganisationPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for the centralized access-control rules in {@link AuthorizationService}.
 * <p>
 * Senior signal: focus on negative cases – these are what prevent real security incidents.
 */
class AuthorizationServiceTest {

    private final AuthorizationService authorizationService = new AuthorizationService();

    // ── Organisation Isolation ─────────────────────────────────────────────────────

    @Test
    void readDocument_shouldDenyAccess_whenOrganisationMismatch() {
        Document doc = document(Classification.PUBLIC);
        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> authorizationService.assertReadAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-organisation");
    }

    @Test
    void writeDocument_shouldDenyAccess_whenOrganisationMismatch() {
        Document doc = document(Classification.PUBLIC);
        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> authorizationService.assertWriteAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-organisation");
    }

    @Test
    void readCase_shouldDenyAccess_whenOrganisationMismatch() {
        DocumentCase documentCase = documentCase("organisation-A");
        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> authorizationService.assertReadAccess(documentCase, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-organisation");
    }

    @Test
    void readCase_shouldAllowAccess_whenSameOrganisation() {
        DocumentCase documentCase = documentCase("organisation-A");
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertReadAccess(documentCase, principal));
    }

    @Test
    void crossOrganisationLink_shouldBeDenied() {
        Document doc = document(Classification.PUBLIC);
        DocumentCase documentCase = documentCase("organisation-B");

        assertThatThrownBy(() -> authorizationService.assertSameOrganisationForLink(doc, documentCase))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void sameOrganisationLink_shouldBeAllowed() {
        Document doc = document(Classification.PUBLIC);
        DocumentCase documentCase = documentCase("organisation-A");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertSameOrganisationForLink(doc, documentCase));
    }

    // ── Classification ───────────────────────────────────────────────────────

    @Test
    void readSecretDocument_shouldDenyAccess_forRegularUser() {
        Document doc = document(Classification.SECRET);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> authorizationService.assertReadAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    @Test
    void readSecretDocument_shouldDenyAccess_forAdmin() {
        Document doc = document(Classification.SECRET);
        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        assertThatThrownBy(() -> authorizationService.assertReadAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    @Test
    void readSecretDocument_shouldAllowAccess_forSuperAdmin() {
        Document doc = document(Classification.SECRET);
        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertReadAccess(doc, principal));
    }

    @Test
    void readConfidentialDocument_shouldAllowAccess_forRegularUser() {
        Document doc = document(Classification.CONFIDENTIAL);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertReadAccess(doc, principal));
    }

    @Test
    void writeSecretDocument_shouldDenyAccess_forRegularUser() {
        // Write also enforces the classification check – users cannot modify SECRET docs
        Document doc = document(Classification.SECRET);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> authorizationService.assertWriteAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    @Test
    void writeSecretDocument_shouldAllowAccess_forSuperAdmin() {
        Document doc = document(Classification.SECRET);
        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertWriteAccess(doc, principal));
    }

    // ── isReadPermitted (list filtering) ─────────────────────────────────────

    @Test
    void isReadPermitted_shouldReturnFalse_forSecretAndRegularUser() {
        Document doc = document(Classification.SECRET);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isFalse();
    }

    @Test
    void isReadPermitted_shouldReturnFalse_forSecretAndAdmin() {
        // ADMIN is NOT SUPER_ADMIN – must not see SECRET documents in lists
        Document doc = document(Classification.SECRET);
        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isFalse();
    }

    @Test
    void isReadPermitted_shouldReturnTrue_forSecretAndSuperAdmin() {
        Document doc = document(Classification.SECRET);
        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isTrue();
    }

    @Test
    void isReadPermitted_shouldReturnFalse_forCrossOrganisationAccess() {
        // Even SUPER_ADMIN cannot see another organisation's documents in a list
        Document doc = document(Classification.PUBLIC);
        OrganisationPrincipal principal = principal("user1", "organisation-B", "SUPER_ADMIN");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isFalse();
    }

    @Test
    void isReadPermitted_shouldReturnTrue_forPublicDocument() {
        Document doc = document(Classification.PUBLIC);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isTrue();
    }

    // ── Closed Case ──────────────────────────────────────────────────────────

    @Test
    void writeDocument_shouldDenyAccess_whenLinkedCaseIsClosed() {
        DocumentCase closedCase = documentCase("organisation-A");
        closedCase.setStatus(CaseStatus.CLOSED);

        Document doc = document(Classification.PUBLIC);
        doc.setLinkedCase(closedCase);

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> authorizationService.assertWriteAccess(doc, principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void writeDocument_shouldAllowAccess_whenLinkedCaseIsOpen() {
        DocumentCase openCase = documentCase("organisation-A");
        openCase.setStatus(CaseStatus.OPEN);

        Document doc = document(Classification.PUBLIC);
        doc.setLinkedCase(openCase);

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertWriteAccess(doc, principal));
    }

    @Test
    void writeDocument_shouldAllowAccess_whenDocumentHasNoLinkedCase() {
        Document doc = document(Classification.PUBLIC);
        // linkedCase is null – no case check should fire

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertWriteAccess(doc, principal));
    }

    // ── Role Escalation ──────────────────────────────────────────────────────

    @Test
    void closeCase_shouldDenyAccess_forUserRole() {
        DocumentCase documentCase = documentCase("organisation-A");
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> authorizationService.assertCloseAccess(documentCase, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void closeCase_shouldDenyAccess_whenNoRoles() {
        DocumentCase documentCase = documentCase("organisation-A");
        OrganisationPrincipal principal = principal("user1", "organisation-A" /* no roles */);

        assertThatThrownBy(() -> authorizationService.assertCloseAccess(documentCase, principal))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void closeCase_shouldDenyAccess_forSuperAdminWithoutAdminRole() {
        // SUPER_ADMIN alone does NOT grant case-close rights – explicit ADMIN required
        DocumentCase documentCase = documentCase("organisation-A");
        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        assertThatThrownBy(() -> authorizationService.assertCloseAccess(documentCase, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void closeCase_shouldAllowAccess_forAdminRole() {
        DocumentCase documentCase = documentCase("organisation-A");
        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertCloseAccess(documentCase, principal));
    }

    @Test
    void closeCase_shouldDenyAccess_forAdminInDifferentOrganisation() {
        DocumentCase documentCase = documentCase("organisation-A");
        OrganisationPrincipal principal = principal("admin1", "organisation-B", "ADMIN");

        assertThatThrownBy(() -> authorizationService.assertCloseAccess(documentCase, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-organisation");
    }

    // ── Test helpers ─────────────────────────────────────────────────────────

    private static OrganisationPrincipal principal(String userId, String organisationId, String... roles) {
        var authorities = Stream.of(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                .toList();
        return new OrganisationPrincipal(userId, organisationId, authorities);
    }

    private static Document document(Classification classification) {
        return Document.builder()
                .organisationId("organisation-A")
                .title("Test document")
                .classification(classification)
                .createdBy("test-user")
                .build();
    }

    private static DocumentCase documentCase(String organisationId) {
        return DocumentCase.builder()
                .organisationId(organisationId)
                .title("Test case")
                .status(CaseStatus.OPEN)
                .createdBy("test-user")
                .build();
    }
}
