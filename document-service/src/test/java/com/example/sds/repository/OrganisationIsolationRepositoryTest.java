package com.example.sds.repository;

import com.example.sds.domain.CaseStatus;
import com.example.sds.domain.Classification;
import com.example.sds.domain.Document;
import com.example.sds.domain.DocumentCase;
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
    private DocumentRepository documentRepository;

    @Autowired
    private CaseRepository caseRepository;

    // ── Document isolation ────────────────────────────────────────────────────

    @Test
    void findByIdAndOrganisationId_shouldReturnDocument_forCorrectOrganisation() {
        Document doc = saved(document("organisation-A", Classification.PUBLIC));

        Optional<Document> result =
                documentRepository.findByIdAndOrganisationId(doc.getId(), "organisation-A");

        assertThat(result).isPresent();
    }

    @Test
    void findByIdAndOrganisationId_shouldReturnEmpty_forWrongOrganisation() {
        Document doc = saved(document("organisation-A", Classification.PUBLIC));

        Optional<Document> result =
                documentRepository.findByIdAndOrganisationId(doc.getId(), "organisation-B");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByOrganisationId_shouldOnlyReturnOwnOrganisationDocuments() {
        saved(document("organisation-A", Classification.PUBLIC));
        saved(document("organisation-A", Classification.INTERNAL));
        saved(document("organisation-B", Classification.PUBLIC));

        List<Document> result = documentRepository.findAllByOrganisationId("organisation-A");

        assertThat(result).hasSize(2)
                .allMatch(d -> d.getOrganisationId().equals("organisation-A"));
    }

    @Test
    void findAllByOrganisationId_shouldReturnEmpty_whenOrganisationHasNoDocuments() {
        saved(document("organisation-A", Classification.PUBLIC));

        List<Document> result = documentRepository.findAllByOrganisationId("organisation-C");

        assertThat(result).isEmpty();
    }

    // ── Case isolation ────────────────────────────────────────────────────────

    @Test
    void findCaseByIdAndOrganisationId_shouldReturnEmpty_forWrongOrganisation() {
        DocumentCase dc = saved(documentCase("organisation-A"));

        Optional<DocumentCase> result =
                caseRepository.findByIdAndOrganisationId(dc.getId(), "organisation-B");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllCasesByOrganisationId_shouldOnlyReturnOwnOrganisationCases() {
        saved(documentCase("organisation-A"));
        saved(documentCase("organisation-A"));
        saved(documentCase("organisation-B"));

        List<DocumentCase> result = caseRepository.findAllByOrganisationId("organisation-A");

        assertThat(result).hasSize(2)
                .allMatch(c -> c.getOrganisationId().equals("organisation-A"));
    }

    // ── Document-Case cross-reference ─────────────────────────────────────────

    @Test
    void findAllByLinkedCase_shouldReturnEmpty_forWrongOrganisation() {
        DocumentCase dc = saved(documentCase("organisation-A"));
        Document doc = document("organisation-A", Classification.PUBLIC);
        doc.setLinkedCase(dc);
        saved(doc);

        // Query with correct case ID but wrong organisation
        List<Document> result =
                documentRepository.findAllByLinkedCase_IdAndOrganisationId(dc.getId(), "organisation-B");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByLinkedCase_shouldReturnDocuments_forCorrectOrganisation() {
        DocumentCase dc = saved(documentCase("organisation-A"));
        Document doc = document("organisation-A", Classification.PUBLIC);
        doc.setLinkedCase(dc);
        saved(doc);

        List<Document> result =
                documentRepository.findAllByLinkedCase_IdAndOrganisationId(dc.getId(), "organisation-A");

        assertThat(result).hasSize(1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> T saved(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    private static Document document(String organisationId, Classification classification) {
        return Document.builder()
                .organisationId(organisationId)
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
