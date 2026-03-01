package se.exempel.sds.service;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Dokument;
import se.exempel.sds.domain.DokumentArende;
import se.exempel.sds.dto.CreateDokumentRequest;
import se.exempel.sds.dto.DokumentResponse;
import se.exempel.sds.dto.UpdateDokumentRequest;
import se.exempel.sds.repository.ArendeRepository;
import se.exempel.sds.repository.DokumentRepository;
import se.exempel.sds.security.OrganisationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class DokumentService {

    private final DokumentRepository dokumentRepository;
    private final ArendeRepository arendeRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final FileValidator fileValidator;
    private final FileStorageService fileStorageService;

    public DokumentService(DokumentRepository dokumentRepository,
                           ArendeRepository arendeRepository,
                           AuthorizationService authorizationService,
                           AuditService auditService,
                           FileValidator fileValidator,
                           FileStorageService fileStorageService) {
        this.dokumentRepository = dokumentRepository;
        this.arendeRepository = arendeRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.fileValidator = fileValidator;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public DokumentResponse getDokument(UUID id, OrganisationPrincipal principal) {
        Dokument doc = requireDokument(id, principal.organisationId());
        authorizationService.assertReadAccess(doc, principal);
        auditService.log(AuditAction.DOKUMENT_READ, principal, id);
        return DokumentResponse.from(doc);
    }

    @Transactional(readOnly = true)
    public List<DokumentResponse> listDokuments(OrganisationPrincipal principal) {
        return dokumentRepository.findAllByOrganisationId(principal.organisationId()).stream()
                // Filter out classifications the caller cannot see (e.g. SECRET for non-SUPER_ADMIN)
                .filter(doc -> authorizationService.isReadPermitted(doc, principal))
                .map(DokumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DokumentResponse> listDokumentsByArende(UUID arendeId, OrganisationPrincipal principal) {
        // Verify the arende exists in this organisation before listing its dokuments
        DokumentArende dokumentArende = requireArende(arendeId, principal.organisationId());
        authorizationService.assertReadAccess(dokumentArende, principal);

        return dokumentRepository.findAllByLinkedArende_IdAndOrganisationId(arendeId, principal.organisationId())
                .stream()
                .filter(doc -> authorizationService.isReadPermitted(doc, principal))
                .map(DokumentResponse::from)
                .toList();
    }

    public DokumentResponse createDokument(CreateDokumentRequest request,
                                           OrganisationPrincipal principal) {
        DokumentArende linkedArende = null;

        if (request.arendeId() != null) {
            linkedArende = requireArende(request.arendeId(), principal.organisationId());
            // Cross-organisation link check (arende already verified above, but be explicit)
            authorizationService.assertSameOrganisationForLink(
                    Dokument.builder().organisationId(principal.organisationId()).build(), linkedArende);

            if (linkedArende.getStatus() == ArendeStatus.CLOSED) {
                throw new IllegalStateException(
                        "Cannot add a dokument to a CLOSED arende");
            }
        }

        Dokument doc = Dokument.builder()
                .organisationId(principal.organisationId())
                .title(request.title())
                .description(request.description())
                .filename(request.filename())
                .contentType(request.contentType())
                .fileSize(request.fileSize())
                .classification(request.classification())
                .linkedArende(linkedArende)
                .createdBy(principal.userId())
                .build();

        DokumentResponse response = DokumentResponse.from(dokumentRepository.save(doc));
        auditService.log(AuditAction.DOKUMENT_CREATE, principal, response.id());
        return response;
    }

    public DokumentResponse updateDokument(UUID id,
                                           UpdateDokumentRequest request,
                                           OrganisationPrincipal principal) {
        Dokument doc = requireDokument(id, principal.organisationId());
        authorizationService.assertWriteAccess(doc, principal);

        doc.setTitle(request.title());
        doc.setDescription(request.description());
        doc.setClassification(request.classification());
        doc.setUpdatedBy(principal.userId());

        DokumentResponse response = DokumentResponse.from(dokumentRepository.save(doc));
        auditService.log(AuditAction.DOKUMENT_UPDATE, principal, id);
        return response;
    }

    public void deleteDokument(UUID id, OrganisationPrincipal principal) {
        Dokument doc = requireDokument(id, principal.organisationId());
        authorizationService.assertWriteAccess(doc, principal);
        dokumentRepository.delete(doc);
        auditService.log(AuditAction.DOKUMENT_DELETE, principal, id);
    }

    /**
     * Attaches or replaces the physical file for an existing dokument.
     * The original filename is sanitized to its basename before storage.
     * The actual on-disk name is always a fresh UUID – never the original filename.
     */
    public DokumentResponse uploadFile(UUID id, MultipartFile file, OrganisationPrincipal principal) {
        Dokument doc = requireDokument(id, principal.organisationId());
        authorizationService.assertWriteAccess(doc, principal);

        fileValidator.validate(file);

        try {
            // Delete the previous file before writing the new one
            if (doc.getStoragePath() != null) {
                fileStorageService.delete(doc.getStoragePath());
            }
            String storagePath = fileStorageService.store(file, principal.organisationId());

            doc.setFilename(safeDisplayName(file.getOriginalFilename()));
            doc.setContentType(file.getContentType());
            doc.setFileSize(file.getSize());
            doc.setStoragePath(storagePath);
            doc.setUpdatedBy(principal.userId());
        } catch (IOException e) {
            throw new RuntimeException("File storage operation failed", e);
        }

        DokumentResponse response = DokumentResponse.from(dokumentRepository.save(doc));
        auditService.log(AuditAction.DOKUMENT_UPDATE, principal, id);
        return response;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns only the final component of a filename, stripping any directory prefix.
     * Prevents displaying a raw path like "../../etc/passwd" to the user.
     */
    private static String safeDisplayName(String filename) {
        if (filename == null) return null;
        // Take only the part after the last slash/backslash
        int lastSep = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        return lastSep >= 0 ? filename.substring(lastSep + 1) : filename;
    }

    private Dokument requireDokument(UUID id, String organisationId) {
        return dokumentRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Dokument not found: " + id));
    }

    private DokumentArende requireArende(UUID id, String organisationId) {
        return arendeRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Arende not found: " + id));
    }
}
