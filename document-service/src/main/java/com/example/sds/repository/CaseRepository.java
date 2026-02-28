package com.example.sds.repository;

import com.example.sds.domain.DocumentCase;
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
public interface CaseRepository extends JpaRepository<DocumentCase, UUID> {

    Optional<DocumentCase> findByIdAndOrganisationId(UUID id, String organisationId);

    List<DocumentCase> findAllByOrganisationId(String organisationId);
}
