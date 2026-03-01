package se.exempel.sds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates an uploaded {@link MultipartFile} against security constraints.
 *
 * <h3>Checks performed</h3>
 * <ol>
 *   <li><b>Not empty</b> – reject zero-byte uploads.</li>
 *   <li><b>Max size</b> – defence-in-depth alongside {@code spring.servlet.multipart.max-file-size}.</li>
 *   <li><b>Content-type whitelist</b> – only explicitly allowed MIME types are accepted.</li>
 *   <li><b>Filename safety</b> – path-traversal characters ({@code ..}, {@code /}, {@code \})
 *       are rejected immediately. The original filename is NEVER used as the storage name.</li>
 * </ol>
 *
 * <p><b>Limitation:</b> content-type is taken from the HTTP header, which can be spoofed.
 * For higher assurance, combine this check with magic-byte inspection (e.g. Apache Tika).
 */
@Component
public class FileValidator {

    private static final Logger log = LoggerFactory.getLogger(FileValidator.class);

    private final FileStorageProperties properties;

    public FileValidator(FileStorageProperties properties) {
        this.properties = properties;
    }

    public void validate(MultipartFile file) {
        rejectIfEmpty(file);
        rejectIfTooLarge(file);
        rejectIfContentTypeNotAllowed(file);
        rejectIfFilenameUnsafe(file.getOriginalFilename());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void rejectIfEmpty(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }
    }

    private void rejectIfTooLarge(MultipartFile file) {
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new IllegalArgumentException(
                    "File exceeds the maximum allowed size of "
                    + properties.maxFileSizeBytes() / (1024 * 1024) + " MB");
        }
    }

    private void rejectIfContentTypeNotAllowed(MultipartFile file) {
        String declared = file.getContentType();
        if (declared == null || !properties.allowedContentTypes().contains(declared)) {
            // Log the rejected type server-side for ops visibility but do NOT echo it
            // back to the client (avoids confirming what types are accepted).
            log.warn("File upload rejected: content-type '{}' is not on the whitelist", declared);
            throw new IllegalArgumentException("File type is not permitted");
        }
    }

    private void rejectIfFilenameUnsafe(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File must have a name");
        }
        // Reject any traversal attempt before the filename touches the filesystem
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            log.warn("Path traversal attempt in filename: '{}'", filename);
            throw new IllegalArgumentException("Invalid filename");
        }
    }
}
