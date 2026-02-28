package com.example.sds.repository;

import com.example.sds.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * All query methods include organisationId to enforce organisation isolation at the
 * database level. Plain {@code findById(id)} is intentionally absent –
 * every lookup must be scoped to the caller's organisation.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndOrganisationId(UUID id, String organisationId);

    List<Document> findAllByOrganisationId(String organisationId);

    /** All documents linked to a specific case, still scoped to the organisation. */
    List<Document> findAllByLinkedCase_IdAndOrganisationId(UUID caseId, String organisationId);
}
