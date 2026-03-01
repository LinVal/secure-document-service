package se.exempel.sds.repository;

import se.exempel.sds.domain.DokumentArende;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * All query methods include organisationId to enforce organisation isolation at the
 * database level. Plain {@code findById(id)} is intentionally absent.
 */
@Repository
public interface ArendeRepository extends JpaRepository<DokumentArende, UUID> {

    Optional<DokumentArende> findByIdAndOrganisationId(UUID id, String organisationId);

    List<DokumentArende> findAllByOrganisationId(String organisationId);
}
