package com.example.sds.service;

import com.example.sds.domain.Classification;
import com.example.sds.domain.CaseStatus;
import com.example.sds.domain.Document;
import com.example.sds.domain.DocumentCase;
import com.example.sds.security.OrganisationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for all access-control decisions.
 *
 * <p>Rules enforced here:
 * <ul>
 *   <li>Organisation isolation – a principal can never access another organisation's data.</li>
 *   <li>Classification – SECRET documents require the SUPER_ADMIN role.</li>
 *   <li>Case status – documents in a CLOSED case cannot be modified.</li>
 *   <li>Case admin – only ADMIN can close a case.</li>
 * </ul>
 *
 * <p>Controllers and other services must NOT duplicate these checks.
 */
@Service
public class AuthorizationService {

    // ── Document ────────────────────────────────────────────────────────────

    /**
     * Verifies that {@code principal} may read {@code doc}.
     * Throws {@link AccessDeniedException} on any violation.
     */
    public void assertReadAccess(Document doc, OrganisationPrincipal principal) {
        assertSameOrganisation(doc.getOrganisationId(), principal);
        assertClassificationAccess(doc.getClassification(), principal);
    }

    /**
     * Returns {@code true} when the principal is permitted to read {@code doc},
     * {@code false} otherwise. Use for list filtering where throwing would be wrong.
     */
    public boolean isReadPermitted(Document doc, OrganisationPrincipal principal) {
        if (!doc.getOrganisationId().equals(principal.organisationId())) return false;
        return doc.getClassification() != Classification.SECRET
                || principal.hasRole("SUPER_ADMIN");
    }

    /**
     * Verifies that {@code principal} may create or modify {@code doc}.
     * Includes the classification check (write implies read permission) and
     * the closed-case guard.
     */
    public void assertWriteAccess(Document doc, OrganisationPrincipal principal) {
        assertSameOrganisation(doc.getOrganisationId(), principal);
        assertClassificationAccess(doc.getClassification(), principal);
        assertCaseOpen(doc);
    }

    // ── Case ────────────────────────────────────────────────────────────────

    /**
     * Verifies that {@code principal} may read {@code documentCase}.
     */
    public void assertReadAccess(DocumentCase documentCase, OrganisationPrincipal principal) {
        assertSameOrganisation(documentCase.getOrganisationId(), principal);
    }

    /**
     * Verifies that {@code principal} may close {@code documentCase}.
     * Requires ADMIN role within the same organisation.
     */
    public void assertCloseAccess(DocumentCase documentCase, OrganisationPrincipal principal) {
        assertSameOrganisation(documentCase.getOrganisationId(), principal);
        assertRole(principal, "ADMIN", "Only ADMIN may close a case");
    }

    // ── Cross-entity ────────────────────────────────────────────────────────

    /**
     * Verifies that a document and a case belong to the same organisation before linking.
     */
    public void assertSameOrganisationForLink(Document doc, DocumentCase documentCase) {
        if (!doc.getOrganisationId().equals(documentCase.getOrganisationId())) {
            throw new AccessDeniedException(
                    "Document and case must belong to the same organisation");
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void assertSameOrganisation(String resourceOrganisationId, OrganisationPrincipal principal) {
        if (!resourceOrganisationId.equals(principal.organisationId())) {
            throw new AccessDeniedException("Cross-organisation access denied");
        }
    }

    private void assertClassificationAccess(Classification classification,
                                            OrganisationPrincipal principal) {
        if (classification == Classification.SECRET) {
            assertRole(principal, "SUPER_ADMIN", "SECRET documents require SUPER_ADMIN clearance");
        }
    }

    private void assertRole(OrganisationPrincipal principal, String role, String message) {
        if (!principal.hasRole(role)) {
            throw new AccessDeniedException(message);
        }
    }

    private void assertCaseOpen(Document doc) {
        if (doc.getLinkedCase() != null
                && doc.getLinkedCase().getStatus() == CaseStatus.CLOSED) {
            throw new IllegalStateException(
                    "Cannot modify a document that is linked to a CLOSED case");
        }
    }
}
