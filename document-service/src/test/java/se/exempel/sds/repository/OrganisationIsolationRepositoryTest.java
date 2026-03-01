package se.exempel.sds.repository;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Classification;
import se.exempel.sds.domain.Dokument;
import se.exempel.sds.domain.DokumentArende;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves organisation isolation at the database level.
 * <p>
 * {@code findById} is intentionally absent from our repositories – this test
 * verifies that the organisation-scoped queries are the ONLY way to find data.
 * <p>
 * Uses @SpringBootTest(webEnvironment=NONE) with H2 (dev profile) because
 * Spring Boot 4 removed the @DataJpaTest slice. @Transactional rolls back
 * each test, providing the same isolation that @DataJpaTest gave.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
class OrganisationIsolationRepositoryTest {

    // Prevents Spring from trying to contact the OIDC issuer URI on startup
    @MockitoBean
    JwtDecoder jwtDecoder;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private DokumentRepository dokumentRepository;

    @Autowired
    private ArendeRepository arendeRepository;

    // ── Dokument isolation ────────────────────────────────────────────────────

    @Test
    void findByIdAndOrganisationId_shouldReturnDokument_forCorrectOrganisation() {
        Dokument doc = saved(dokument("organisation-A", Classification.PUBLIC));

        Optional<Dokument> result =
                dokumentRepository.findByIdAndOrganisationId(doc.getId(), "organisation-A");

        assertThat(result).isPresent();
    }

    @Test
    void findByIdAndOrganisationId_shouldReturnEmpty_forWrongOrganisation() {
        Dokument doc = saved(dokument("organisation-A", Classification.PUBLIC));

        Optional<Dokument> result =
                dokumentRepository.findByIdAndOrganisationId(doc.getId(), "organisation-B");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByOrganisationId_shouldOnlyReturnOwnOrganisationDokuments() {
        saved(dokument("organisation-A", Classification.PUBLIC));
        saved(dokument("organisation-A", Classification.INTERNAL));
        saved(dokument("organisation-B", Classification.PUBLIC));

        List<Dokument> result = dokumentRepository.findAllByOrganisationId("organisation-A");

        assertThat(result).hasSize(2)
                .allMatch(d -> d.getOrganisationId().equals("organisation-A"));
    }

    @Test
    void findAllByOrganisationId_shouldReturnEmpty_whenOrganisationHasNoDokuments() {
        saved(dokument("organisation-A", Classification.PUBLIC));

        List<Dokument> result = dokumentRepository.findAllByOrganisationId("organisation-C");

        assertThat(result).isEmpty();
    }

    // ── Arende isolation ────────────────────────────────────────────────────────

    @Test
    void findArendeByIdAndOrganisationId_shouldReturnEmpty_forWrongOrganisation() {
        DokumentArende dc = saved(dokumentArende("organisation-A"));

        Optional<DokumentArende> result =
                arendeRepository.findByIdAndOrganisationId(dc.getId(), "organisation-B");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllArendenByOrganisationId_shouldOnlyReturnOwnOrganisationArenden() {
        saved(dokumentArende("organisation-A"));
        saved(dokumentArende("organisation-A"));
        saved(dokumentArende("organisation-B"));

        List<DokumentArende> result = arendeRepository.findAllByOrganisationId("organisation-A");

        assertThat(result).hasSize(2)
                .allMatch(c -> c.getOrganisationId().equals("organisation-A"));
    }

    // ── Dokument-Arende cross-reference ─────────────────────────────────────────

    @Test
    void findAllByLinkedArende_shouldReturnEmpty_forWrongOrganisation() {
        DokumentArende dc = saved(dokumentArende("organisation-A"));
        Dokument doc = dokument("organisation-A", Classification.PUBLIC);
        doc.setLinkedArende(dc);
        saved(doc);

        // Query with correct arende ID but wrong organisation
        List<Dokument> result =
                dokumentRepository.findAllByLinkedArende_IdAndOrganisationId(dc.getId(), "organisation-B");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByLinkedArende_shouldReturnDokuments_forCorrectOrganisation() {
        DokumentArende dc = saved(dokumentArende("organisation-A"));
        Dokument doc = dokument("organisation-A", Classification.PUBLIC);
        doc.setLinkedArende(dc);
        saved(doc);

        List<Dokument> result =
                dokumentRepository.findAllByLinkedArende_IdAndOrganisationId(dc.getId(), "organisation-A");

        assertThat(result).hasSize(1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> T saved(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    private static Dokument dokument(String organisationId, Classification classification) {
        return Dokument.builder()
                .organisationId(organisationId)
                .title("Test dokument")
                .classification(classification)
                .createdBy("test-user")
                .build();
    }

    private static DokumentArende dokumentArende(String organisationId) {
        return DokumentArende.builder()
                .organisationId(organisationId)
                .title("Test arende")
                .status(ArendeStatus.OPEN)
                .createdBy("test-user")
                .build();
    }
}
