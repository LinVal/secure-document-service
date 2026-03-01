package se.exempel.sds.service;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Classification;
import se.exempel.sds.domain.Dokument;
import se.exempel.sds.domain.DokumentArende;
import se.exempel.sds.dto.CreateDokumentRequest;
import se.exempel.sds.dto.DokumentResponse;
import se.exempel.sds.dto.UpdateDokumentRequest;
import se.exempel.sds.repository.ArendeRepository;
import se.exempel.sds.repository.DokumentRepository;
import se.exempel.sds.security.OrganisationPrincipal;
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
 * between DokumentService and its access-control logic.
 */
@ExtendWith(MockitoExtension.class)
class DokumentServiceTest {

    // Real authorization logic – not mocked
    private final AuthorizationService authorizationService = new AuthorizationService();
    private final AuditService auditService = new AuditService();
    private final FileValidator fileValidator = mock(FileValidator.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final DokumentRepository dokumentRepository = mock(DokumentRepository.class);
    private final ArendeRepository arendeRepository = mock(ArendeRepository.class);

    private final DokumentService dokumentService = new DokumentService(
            dokumentRepository, arendeRepository, authorizationService,
            auditService, fileValidator, fileStorageService);

    // ── getDokument ──────────────────────────────────────────────────────────

    @Test
    void getDokument_shouldReturn404_whenDokumentBelongsToOtherOrganisation() {
        UUID id = UUID.randomUUID();
        // Repository returns empty because organisationId doesn't match
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-B"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> dokumentService.getDokument(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getDokument_shouldDeny_forSecretDokumentWithoutSuperAdmin() {
        UUID id = UUID.randomUUID();
        Dokument secretDoc = dokument(id, Classification.SECRET);
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(secretDoc));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> dokumentService.getDokument(id, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    @Test
    void getDokument_shouldAllow_forSecretDokumentWithSuperAdmin() {
        UUID id = UUID.randomUUID();
        Dokument secretDoc = dokument(id, Classification.SECRET);
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(secretDoc));

        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        DokumentResponse response = dokumentService.getDokument(id, principal);
        assertThat(response.classification()).isEqualTo(Classification.SECRET);
    }

    // ── listDokuments ────────────────────────────────────────────────────────

    @Test
    void listDokuments_shouldSilentlyFilterOut_secretDokumentsForNonSuperAdmin() {
        Dokument publicDoc = dokument(UUID.randomUUID(), Classification.PUBLIC);
        Dokument secretDoc = dokument(UUID.randomUUID(), Classification.SECRET);
        when(dokumentRepository.findAllByOrganisationId("organisation-A"))
                .thenReturn(List.of(publicDoc, secretDoc));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        List<DokumentResponse> result = dokumentService.listDokuments(principal);

        // SECRET dokument must not appear – not even as a 403
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().classification()).isEqualTo(Classification.PUBLIC);
    }

    @Test
    void listDokuments_shouldShowAllClassifications_forSuperAdmin() {
        Dokument publicDoc = dokument(UUID.randomUUID(), Classification.PUBLIC);
        Dokument secretDoc = dokument(UUID.randomUUID(), Classification.SECRET);
        when(dokumentRepository.findAllByOrganisationId("organisation-A"))
                .thenReturn(List.of(publicDoc, secretDoc));

        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        List<DokumentResponse> result = dokumentService.listDokuments(principal);

        assertThat(result).hasSize(2);
    }

    // ── createDokument ───────────────────────────────────────────────────────

    @Test
    void createDokument_shouldSucceed_whenNoArendeLinked() {
        when(dokumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        CreateDokumentRequest request = new CreateDokumentRequest(
                "Title", null, null, null, null, Classification.PUBLIC, null);

        assertThatNoException().isThrownBy(() -> dokumentService.createDokument(request, principal));

        verify(arendeRepository, never()).findByIdAndOrganisationId(any(), any());
    }

    @Test
    void createDokument_shouldThrow_whenLinkedArendeIsClosed() {
        UUID arendeId = UUID.randomUUID();
        DokumentArende closedArende = dokumentArende(arendeId);
        when(arendeRepository.findByIdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(Optional.of(closedArende));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        CreateDokumentRequest request = new CreateDokumentRequest(
                "Title", null, null, null, null, Classification.PUBLIC, arendeId);

        assertThatThrownBy(() -> dokumentService.createDokument(request, principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void createDokument_shouldThrow_whenArendeBelongsToOtherOrganisation() {
        UUID arendeId = UUID.randomUUID();
        // Repository returns empty – arende exists but in a different organisation
        when(arendeRepository.findByIdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        CreateDokumentRequest request = new CreateDokumentRequest(
                "Title", null, null, null, null, Classification.PUBLIC, arendeId);

        assertThatThrownBy(() -> dokumentService.createDokument(request, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── updateDokument ───────────────────────────────────────────────────────

    @Test
    void updateDokument_shouldReturn404_whenDokumentNotFound() {
        UUID id = UUID.randomUUID();
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        UpdateDokumentRequest request = new UpdateDokumentRequest("New title", null, Classification.PUBLIC);

        assertThatThrownBy(() -> dokumentService.updateDokument(id, request, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateDokument_shouldDeny_forSecretDokument_withRegularUser() {
        UUID id = UUID.randomUUID();
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(dokument(id, Classification.SECRET)));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        UpdateDokumentRequest request = new UpdateDokumentRequest("New title", null, Classification.SECRET);

        assertThatThrownBy(() -> dokumentService.updateDokument(id, request, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    // ── deleteDokument ───────────────────────────────────────────────────────

    @Test
    void deleteDokument_shouldReturn404_whenDokumentNotFound() {
        UUID id = UUID.randomUUID();
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> dokumentService.deleteDokument(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteDokument_shouldSucceed_andCallRepositoryDelete() {
        UUID id = UUID.randomUUID();
        Dokument doc = dokument(id, Classification.PUBLIC);
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(doc));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException().isThrownBy(() -> dokumentService.deleteDokument(id, principal));

        verify(dokumentRepository).delete(doc);
    }

    // ── listDokumentsByArende ───────────────────────────────────────────────────

    @Test
    void listDokumentsByArende_shouldReturn404_whenArendeNotFound() {
        UUID arendeId = UUID.randomUUID();
        when(arendeRepository.findByIdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> dokumentService.listDokumentsByArende(arendeId, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void listDokumentsByArende_shouldFilterSecretDokuments_forNonSuperAdmin() {
        UUID arendeId = UUID.randomUUID();
        DokumentArende openArende = DokumentArende.builder()
                .id(arendeId).organisationId("organisation-A")
                .title("Arende").status(ArendeStatus.OPEN).createdBy("test").build();

        when(arendeRepository.findByIdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(Optional.of(openArende));
        when(dokumentRepository.findAllByLinkedArende_IdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(List.of(
                        dokument(UUID.randomUUID(), Classification.INTERNAL),
                        dokument(UUID.randomUUID(), Classification.SECRET)));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        List<DokumentResponse> result = dokumentService.listDokumentsByArende(arendeId, principal);

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

    private static Dokument dokument(UUID id, Classification classification) {
        return Dokument.builder()
                .id(id)
                .organisationId("organisation-A")
                .title("Test doc")
                .classification(classification)
                .createdBy("test-user")
                .build();
    }

    private static DokumentArende dokumentArende(UUID id) {
        return DokumentArende.builder()
                .id(id)
                .organisationId("organisation-A")
                .title("Test arende")
                .status(ArendeStatus.CLOSED)
                .createdBy("test-user")
                .build();
    }
}
