package se.exempel.sds.service;

import se.exempel.sds.domain.Classification;
import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Dokument;
import se.exempel.sds.domain.DokumentArende;
import se.exempel.sds.security.OrganisationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for all access-control decisions.
 *
 * <p>Rules enforced here:
 * <ul>
 *   <li>Organisation isolation – a principal can never access another organisation's data.</li>
 *   <li>Classification – SECRET dokuments require the SUPER_ADMIN role.</li>
 *   <li>Arende status – dokuments in a CLOSED arende cannot be modified.</li>
 *   <li>Arende admin – only ADMIN can close a arende.</li>
 * </ul>
 *
 * <p>Controllers and other services must NOT duplicate these checks.
 */
@Service
public class AuthorizationService {

    // ── Dokument ────────────────────────────────────────────────────────────

    /**
     * Verifies that {@code principal} may read {@code doc}.
     * Throws {@link AccessDeniedException} on any violation.
     */
    public void assertReadAccess(Dokument doc, OrganisationPrincipal principal) {
        assertSameOrganisation(doc.getOrganisationId(), principal);
        assertClassificationAccess(doc.getClassification(), principal);
    }

    /**
     * Returns {@code true} when the principal is permitted to read {@code doc},
     * {@code false} otherwise. Use for list filtering where throwing would be wrong.
     */
    public boolean isReadPermitted(Dokument doc, OrganisationPrincipal principal) {
        if (!doc.getOrganisationId().equals(principal.organisationId())) return false;
        return doc.getClassification() != Classification.SECRET
                || principal.hasRole("SUPER_ADMIN");
    }

    /**
     * Verifies that {@code principal} may create or modify {@code doc}.
     * Includes the classification check (write implies read permission) and
     * the closed-arende guard.
     */
    public void assertWriteAccess(Dokument doc, OrganisationPrincipal principal) {
        assertSameOrganisation(doc.getOrganisationId(), principal);
        assertClassificationAccess(doc.getClassification(), principal);
        assertArendeOpen(doc);
    }

    // ── Arende ────────────────────────────────────────────────────────────────

    /**
     * Verifies that {@code principal} may read {@code dokumentArende}.
     */
    public void assertReadAccess(DokumentArende dokumentArende, OrganisationPrincipal principal) {
        assertSameOrganisation(dokumentArende.getOrganisationId(), principal);
    }

    /**
     * Verifies that {@code principal} may close {@code dokumentArende}.
     * Requires ADMIN role within the same organisation.
     */
    public void assertCloseAccess(DokumentArende dokumentArende, OrganisationPrincipal principal) {
        assertSameOrganisation(dokumentArende.getOrganisationId(), principal);
        assertRole(principal, "ADMIN", "Only ADMIN may close a arende");
    }

    // ── Cross-entity ────────────────────────────────────────────────────────

    /**
     * Verifies that a dokument and a arende belong to the same organisation before linking.
     */
    public void assertSameOrganisationForLink(Dokument doc, DokumentArende dokumentArende) {
        if (!doc.getOrganisationId().equals(dokumentArende.getOrganisationId())) {
            throw new AccessDeniedException(
                    "Dokument and arende must belong to the same organisation");
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
            assertRole(principal, "SUPER_ADMIN", "SECRET dokuments require SUPER_ADMIN clearance");
        }
    }

    private void assertRole(OrganisationPrincipal principal, String role, String message) {
        if (!principal.hasRole(role)) {
            throw new AccessDeniedException(message);
        }
    }

    private void assertArendeOpen(Dokument doc) {
        if (doc.getLinkedArende() != null
                && doc.getLinkedArende().getStatus() == ArendeStatus.CLOSED) {
            throw new IllegalStateException(
                    "Cannot modify a dokument that is linked to a CLOSED arende");
        }
    }
}
