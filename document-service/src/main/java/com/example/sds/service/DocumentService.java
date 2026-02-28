package com.example.sds.service;

import com.example.sds.domain.CaseStatus;
import com.example.sds.domain.Document;
import com.example.sds.domain.DocumentCase;
import com.example.sds.dto.CreateDocumentRequest;
import com.example.sds.dto.DocumentResponse;
import com.example.sds.dto.UpdateDocumentRequest;
import com.example.sds.repository.CaseRepository;
import com.example.sds.repository.DocumentRepository;
import com.example.sds.security.OrganisationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CaseRepository caseRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final FileValidator fileValidator;
    private final FileStorageService fileStorageService;

    public DocumentService(DocumentRepository documentRepository,
                           CaseRepository caseRepository,
                           AuthorizationService authorizationService,
                           AuditService auditService,
                           FileValidator fileValidator,
                           FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.caseRepository = caseRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.fileValidator = fileValidator;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID id, OrganisationPrincipal principal) {
        Document doc = requireDocument(id, principal.organisationId());
        authorizationService.assertReadAccess(doc, principal);
        auditService.log(AuditAction.DOCUMENT_READ, principal, id);
        return DocumentResponse.from(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(OrganisationPrincipal principal) {
        return documentRepository.findAllByOrganisationId(principal.organisationId()).stream()
                // Filter out classifications the caller cannot see (e.g. SECRET for non-SUPER_ADMIN)
                .filter(doc -> authorizationService.isReadPermitted(doc, principal))
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocumentsByCase(UUID caseId, OrganisationPrincipal principal) {
        // Verify the case exists in this organisation before listing its documents
        DocumentCase documentCase = requireCase(caseId, principal.organisationId());
        authorizationService.assertReadAccess(documentCase, principal);

        return documentRepository.findAllByLinkedCase_IdAndOrganisationId(caseId, principal.organisationId())
                .stream()
                .filter(doc -> authorizationService.isReadPermitted(doc, principal))
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentResponse createDocument(CreateDocumentRequest request,
                                           OrganisationPrincipal principal) {
        DocumentCase linkedCase = null;

        if (request.caseId() != null) {
            linkedCase = requireCase(request.caseId(), principal.organisationId());
            // Cross-organisation link check (case already verified above, but be explicit)
            authorizationService.assertSameOrganisationForLink(
                    Document.builder().organisationId(principal.organisationId()).build(), linkedCase);

            if (linkedCase.getStatus() == CaseStatus.CLOSED) {
                throw new IllegalStateException(
                        "Cannot add a document to a CLOSED case");
            }
        }

        Document doc = Document.builder()
                .organisationId(principal.organisationId())
                .title(request.title())
                .description(request.description())
                .filename(request.filename())
                .contentType(request.contentType())
                .fileSize(request.fileSize())
                .classification(request.classification())
                .linkedCase(linkedCase)
                .createdBy(principal.userId())
                .build();

        DocumentResponse response = DocumentResponse.from(documentRepository.save(doc));
        auditService.log(AuditAction.DOCUMENT_CREATE, principal, response.id());
        return response;
    }

    public DocumentResponse updateDocument(UUID id,
                                           UpdateDocumentRequest request,
                                           OrganisationPrincipal principal) {
        Document doc = requireDocument(id, principal.organisationId());
        authorizationService.assertWriteAccess(doc, principal);

        doc.setTitle(request.title());
        doc.setDescription(request.description());
        doc.setClassification(request.classification());
        doc.setUpdatedBy(principal.userId());

        DocumentResponse response = DocumentResponse.from(documentRepository.save(doc));
        auditService.log(AuditAction.DOCUMENT_UPDATE, principal, id);
        return response;
    }

    public void deleteDocument(UUID id, OrganisationPrincipal principal) {
        Document doc = requireDocument(id, principal.organisationId());
        authorizationService.assertWriteAccess(doc, principal);
        documentRepository.delete(doc);
        auditService.log(AuditAction.DOCUMENT_DELETE, principal, id);
    }

    /**
     * Attaches or replaces the physical file for an existing document.
     * The original filename is sanitized to its basename before storage.
     * The actual on-disk name is always a fresh UUID – never the original filename.
     */
    public DocumentResponse uploadFile(UUID id, MultipartFile file, OrganisationPrincipal principal) {
        Document doc = requireDocument(id, principal.organisationId());
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

        DocumentResponse response = DocumentResponse.from(documentRepository.save(doc));
        auditService.log(AuditAction.DOCUMENT_UPDATE, principal, id);
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

    private Document requireDocument(UUID id, String organisationId) {
        return documentRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Document not found: " + id));
    }

    private DocumentCase requireCase(UUID id, String organisationId) {
        return caseRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Case not found: " + id));
    }
}
