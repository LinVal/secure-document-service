package com.example.sds.service;

import com.example.sds.domain.CaseStatus;
import com.example.sds.domain.Classification;
import com.example.sds.domain.Document;
import com.example.sds.domain.DocumentCase;
import com.example.sds.dto.CreateDocumentRequest;
import com.example.sds.dto.DocumentResponse;
import com.example.sds.dto.UpdateDocumentRequest;
import com.example.sds.repository.CaseRepository;
import com.example.sds.repository.DocumentRepository;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Uses the REAL AuthorizationService (no mock) to test the full integration
 * between DocumentService and its access-control logic.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    // Real authorization logic – not mocked
    private final AuthorizationService authorizationService = new AuthorizationService();
    private final AuditService auditService = new AuditService();
    private final FileValidator fileValidator = mock(FileValidator.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final CaseRepository caseRepository = mock(CaseRepository.class);

    private final DocumentService documentService = new DocumentService(
            documentRepository, caseRepository, authorizationService,
            auditService, fileValidator, fileStorageService);

    // ── getDocument ──────────────────────────────────────────────────────────

    @Test
    void getDocument_shouldReturn404_whenDocumentBelongsToOtherOrganisation() {
        UUID id = UUID.randomUUID();
        // Repository returns empty because organisationId doesn't match
        when(documentRepository.findByIdAndOrganisationId(id, "organisation-B"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> documentService.getDocument(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getDocument_shouldDeny_forSecretDocumentWithoutSuperAdmin() {
        UUID id = UUID.randomUUID();
        Document secretDoc = document(id, Classification.SECRET);
        when(documentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(secretDoc));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> documentService.getDocument(id, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    @Test
    void getDocument_shouldAllow_forSecretDocumentWithSuperAdmin() {
        UUID id = UUID.randomUUID();
        Document secretDoc = document(id, Classification.SECRET);
        when(documentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(secretDoc));

        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        DocumentResponse response = documentService.getDocument(id, principal);
        assertThat(response.classification()).isEqualTo(Classification.SECRET);
    }

    // ── listDocuments ────────────────────────────────────────────────────────

    @Test
    void listDocuments_shouldSilentlyFilterOut_secretDocumentsForNonSuperAdmin() {
        Document publicDoc = document(UUID.randomUUID(), Classification.PUBLIC);
        Document secretDoc = document(UUID.randomUUID(), Classification.SECRET);
        when(documentRepository.findAllByOrganisationId("organisation-A"))
                .thenReturn(List.of(publicDoc, secretDoc));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        List<DocumentResponse> result = documentService.listDocuments(principal);

        // SECRET document must not appear – not even as a 403
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().classification()).isEqualTo(Classification.PUBLIC);
    }

    @Test
    void listDocuments_shouldShowAllClassifications_forSuperAdmin() {
        Document publicDoc = document(UUID.randomUUID(), Classification.PUBLIC);
        Document secretDoc = document(UUID.randomUUID(), Classification.SECRET);
        when(documentRepository.findAllByOrganisationId("organisation-A"))
                .thenReturn(List.of(publicDoc, secretDoc));

        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        List<DocumentResponse> result = documentService.listDocuments(principal);

        assertThat(result).hasSize(2);
    }

    // ── createDocument ───────────────────────────────────────────────────────

    @Test
    void createDocument_shouldSucceed_whenNoCaseLinked() {
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Title", null, null, null, null, Classification.PUBLIC, null);

        assertThatNoException().isThrownBy(() -> documentService.createDocument(request, principal));

        verify(caseRepository, never()).findByIdAndOrganisationId(any(), any());
    }

    @Test
    void createDocument_shouldThrow_whenLinkedCaseIsClosed() {
        UUID caseId = UUID.randomUUID();
        DocumentCase closedCase = documentCase(caseId);
        when(caseRepository.findByIdAndOrganisationId(caseId, "organisation-A"))
                .thenReturn(Optional.of(closedCase));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Title", null, null, null, null, Classification.PUBLIC, caseId);

        assertThatThrownBy(() -> documentService.createDocument(request, principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void createDocument_shouldThrow_whenCaseBelongsToOtherOrganisation() {
        UUID caseId = UUID.randomUUID();
        // Repository returns empty – case exists but in a different organisation
        when(caseRepository.findByIdAndOrganisationId(caseId, "organisation-A"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Title", null, null, null, null, Classification.PUBLIC, caseId);

        assertThatThrownBy(() -> documentService.createDocument(request, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── updateDocument ───────────────────────────────────────────────────────

    @Test
    void updateDocument_shouldReturn404_whenDocumentNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        UpdateDocumentRequest request = new UpdateDocumentRequest("New title", null, Classification.PUBLIC);

        assertThatThrownBy(() -> documentService.updateDocument(id, request, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateDocument_shouldDeny_forSecretDocument_withRegularUser() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(document(id, Classification.SECRET)));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        UpdateDocumentRequest request = new UpdateDocumentRequest("New title", null, Classification.SECRET);

        assertThatThrownBy(() -> documentService.updateDocument(id, request, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    // ── deleteDocument ───────────────────────────────────────────────────────

    @Test
    void deleteDocument_shouldReturn404_whenDocumentNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> documentService.deleteDocument(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteDocument_shouldSucceed_andCallRepositoryDelete() {
        UUID id = UUID.randomUUID();
        Document doc = document(id, Classification.PUBLIC);
        when(documentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(doc));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException().isThrownBy(() -> documentService.deleteDocument(id, principal));

        verify(documentRepository).delete(doc);
    }

    // ── listDocumentsByCase ───────────────────────────────────────────────────

    @Test
    void listDocumentsByCase_shouldReturn404_whenCaseNotFound() {
        UUID caseId = UUID.randomUUID();
        when(caseRepository.findByIdAndOrganisationId(caseId, "organisation-A"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> documentService.listDocumentsByCase(caseId, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void listDocumentsByCase_shouldFilterSecretDocuments_forNonSuperAdmin() {
        UUID caseId = UUID.randomUUID();
        DocumentCase openCase = DocumentCase.builder()
                .id(caseId).organisationId("organisation-A")
                .title("Case").status(CaseStatus.OPEN).createdBy("test").build();

        when(caseRepository.findByIdAndOrganisationId(caseId, "organisation-A"))
                .thenReturn(Optional.of(openCase));
        when(documentRepository.findAllByLinkedCase_IdAndOrganisationId(caseId, "organisation-A"))
                .thenReturn(List.of(
                        document(UUID.randomUUID(), Classification.INTERNAL),
                        document(UUID.randomUUID(), Classification.SECRET)));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        List<DocumentResponse> result = documentService.listDocumentsByCase(caseId, principal);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().classification()).isEqualTo(Classification.INTERNAL);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static OrganisationPrincipal principal(String userId, String organisationId, String... roles) {
        var authorities = Stream.of(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                .toList();
        return new OrganisationPrincipal(userId, organisationId, authorities);
    }

    private static Document document(UUID id, Classification classification) {
        return Document.builder()
                .id(id)
                .organisationId("organisation-A")
                .title("Test doc")
                .classification(classification)
                .createdBy("test-user")
                .build();
    }

    private static DocumentCase documentCase(UUID id) {
        return DocumentCase.builder()
                .id(id)
                .organisationId("organisation-A")
                .title("Test case")
                .status(CaseStatus.CLOSED)
                .createdBy("test-user")
                .build();
    }
}
