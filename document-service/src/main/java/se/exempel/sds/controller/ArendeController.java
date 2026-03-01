package se.exempel.sds.controller;

import se.exempel.sds.dto.ArendeResponse;
import se.exempel.sds.dto.CreateArendeRequest;
import se.exempel.sds.security.OrganisationPrincipal;
import se.exempel.sds.service.ArendeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Thin controller – no business or security logic here.
 * All decisions are delegated to {@link ArendeService} and
 * {@link se.exempel.sds.service.AuthorizationService}.
 */
@RestController
@RequestMapping("/api/arenden")
public class ArendeController {

    private final ArendeService arendeService;

    public ArendeController(ArendeService arendeService) {
        this.arendeService = arendeService;
    }

    @GetMapping
    public List<ArendeResponse> listArenden(
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return arendeService.listArenden(principal);
    }

    @GetMapping("/{id}")
    public ArendeResponse getArende(
            @PathVariable UUID id,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return arendeService.getArende(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArendeResponse createArende(
            @RequestBody @Valid CreateArendeRequest request,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return arendeService.createArende(request, principal);
    }

    /** Only ADMIN within the organisation may call this – enforced in ArendeService. */
    @PutMapping("/{id}/close")
    public ArendeResponse closeArende(
            @PathVariable UUID id,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return arendeService.closeArende(id, principal);
    }
}
