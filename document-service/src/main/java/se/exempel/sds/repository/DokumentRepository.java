package se.exempel.sds.repository;

import se.exempel.sds.domain.Dokument;
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
public interface DokumentRepository extends JpaRepository<Dokument, UUID> {

    Optional<Dokument> findByIdAndOrganisationId(UUID id, String organisationId);

    List<Dokument> findAllByOrganisationId(String organisationId);

    /** All dokuments linked to a specific arende, still scoped to the organisation. */
    List<Dokument> findAllByLinkedArende_IdAndOrganisationId(UUID arendeId, String organisationId);
}
