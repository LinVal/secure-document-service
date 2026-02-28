package com.example.sds.service;

import com.example.sds.security.OrganisationPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Writes structured audit entries for security-relevant operations.
 *
 * <h3>What IS logged</h3>
 * <ul>
 *   <li>Action type (enum – no user input)</li>
 *   <li>Actor (userId) and organisation (organisationId) – sanitized</li>
 *   <li>Resource identifier (UUID – safe to log as-is)</li>
 *   <li>Outcome: SUCCESS or FAILURE</li>
 * </ul>
 *
 * <h3>What is NOT logged</h3>
 * <ul>
 *   <li>Document titles, descriptions, filenames – may contain PII or business secrets</li>
 *   <li>JWT tokens or credentials</li>
 *   <li>File contents</li>
 * </ul>
 *
 * <h3>Log-injection protection</h3>
 * All user-supplied strings are passed through {@link #sanitize(String)} before
 * being written to the log. This strips CR, LF and other control characters that
 * an attacker could use to forge log lines.
 *
 * <p>A dedicated {@code "AUDIT"} logger is used so audit events can be routed
 * to a separate appender (e.g. a SIEM or immutable log store) via Logback config.
 */
@Service
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    /**
     * Logs a successful, authorized operation.
     *
     * @param action     what happened
     * @param principal  who did it
     * @param resourceId which resource was affected
     */
    public void log(AuditAction action, OrganisationPrincipal principal, UUID resourceId) {
        AUDIT.info("action={} actor={} organisation={} resourceId={} outcome=SUCCESS",
                action.name(),
                sanitize(principal.userId()),
                sanitize(principal.organisationId()),
                resourceId);
    }

    /**
     * Logs an access-denied event.
     *
     * @param principal null-safe – covers unauthenticated requests too
     * @param resource  the URI or resource description being accessed
     */
    public void logAccessDenied(@Nullable OrganisationPrincipal principal, String resource) {
        String actor  = principal != null ? sanitize(principal.userId())  : "(unauthenticated)";
        String organisation = principal != null ? sanitize(principal.organisationId()) : "(none)";

        AUDIT.warn("action={} actor={} organisation={} resource={} outcome=FAILURE",
                AuditAction.ACCESS_DENIED.name(),
                actor,
                organisation,
                sanitize(resource));
    }

    /**
     * Strips CR, LF and all other ASCII control characters from a user-supplied
     * string to prevent log-injection attacks.
     *
     * <p>Package-private so it can be exercised directly in {@code AuditServiceTest}
     * without mocking the logger.
     */
    static String sanitize(String input) {
        if (input == null) {
            return "(null)";
        }
        // \p{Cntrl} covers U+0000–U+001F and U+007F (all ASCII control chars incl. CR/LF/TAB)
        return input.replaceAll("\\p{Cntrl}", "_");
    }
}
