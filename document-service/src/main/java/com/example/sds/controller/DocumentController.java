package com.example.sds.controller;

import com.example.sds.dto.CreateDocumentRequest;
import com.example.sds.dto.DocumentResponse;
import com.example.sds.dto.UpdateDocumentRequest;
import com.example.sds.security.OrganisationPrincipal;
import com.example.sds.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Thin controller – no business or security logic here.
 * All decisions are delegated to {@link DocumentService} and
 * {@link com.example.sds.service.AuthorizationService}.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentResponse> listDocuments(
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return documentService.listDocuments(principal);
    }

    @GetMapping("/{id}")
    public DocumentResponse getDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return documentService.getDocument(id, principal);
    }

    @GetMapping("/by-case/{caseId}")
    public List<DocumentResponse> listDocumentsByCase(
            @PathVariable UUID caseId,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return documentService.listDocumentsByCase(caseId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse createDocument(
            @RequestBody @Valid CreateDocumentRequest request,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return documentService.createDocument(request, principal);
    }

    @PutMapping("/{id}")
    public DocumentResponse updateDocument(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateDocumentRequest request,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return documentService.updateDocument(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        documentService.deleteDocument(id, principal);
    }

    /**
     * Attach or replace the physical file for an existing document.
     * File validation (size, content-type, filename safety) happens in the service layer.
     */
    @PostMapping("/{id}/upload")
    public DocumentResponse uploadFile(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return documentService.uploadFile(id, file, principal);
    }
}
