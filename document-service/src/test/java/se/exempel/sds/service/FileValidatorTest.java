package se.exempel.sds.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    private static final long MAX_BYTES = 1024; // 1 KB for tests

    private final FileStorageProperties properties = new FileStorageProperties(
            java.nio.file.Path.of("/tmp/test"),
            MAX_BYTES,
            Set.of("application/pdf", "text/plain")
    );

    private final FileValidator validator = new FileValidator(properties);

    // ── Empty file ────────────────────────────────────────────────────────────

    @Test
    void validate_shouldReject_emptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    // ── File size ─────────────────────────────────────────────────────────────

    @Test
    void validate_shouldReject_fileThatExceedsMaxSize() {
        byte[] oversized = new byte[(int) MAX_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", oversized);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum allowed size");
    }

    @Test
    void validate_shouldAccept_fileExactlyAtMaxSize() {
        byte[] exactly = new byte[(int) MAX_BYTES];
        exactly[0] = 1; // non-empty
        MockMultipartFile file = new MockMultipartFile(
                "file", "exact.pdf", "application/pdf", exactly);

        assertThatNoException().isThrownBy(() -> validator.validate(file));
    }

    @Test
    void validate_shouldReject_fileSizeOneByteOverMax() {
        // Boundary: MAX_BYTES + 1 must be rejected
        byte[] oneTooMany = new byte[(int) MAX_BYTES + 1];
        oneTooMany[0] = 1;
        MockMultipartFile file = new MockMultipartFile(
                "file", "borderline.pdf", "application/pdf", oneTooMany);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Content-type whitelist ────────────────────────────────────────────────

    @Test
    void validate_shouldReject_contentTypeNotOnWhitelist() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/x-msdownload", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not permitted");
    }

    @Test
    void validate_shouldReject_nullContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "file.pdf", null, new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_shouldReject_executableContentType_octetStream() {
        // application/octet-stream is a common disguise for executables
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.bin", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_shouldReject_htmlContentType() {
        // text/html could enable XSS if ever served back
        MockMultipartFile file = new MockMultipartFile(
                "file", "xss.html", "text/html", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_shouldAccept_whitelistedContentType_pdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatNoException().isThrownBy(() -> validator.validate(file));
    }

    @Test
    void validate_shouldAccept_whitelistedContentType_textPlain() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", new byte[]{1});

        assertThatNoException().isThrownBy(() -> validator.validate(file));
    }

    // ── Filename safety ───────────────────────────────────────────────────────

    @Test
    void validate_shouldReject_pathTraversalWithDotDot() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid filename");
    }

    @Test
    void validate_shouldReject_pathTraversalWithForwardSlash() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "subdir/evil.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid filename");
    }

    @Test
    void validate_shouldReject_pathTraversalWithBackslash() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "subdir\\evil.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid filename");
    }

    @Test
    void validate_shouldReject_nullFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_shouldReject_emptyFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_shouldAccept_normalFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report-2025.pdf", "application/pdf", new byte[]{1});

        assertThatNoException().isThrownBy(() -> validator.validate(file));
    }

    @Test
    void validate_shouldAccept_filenameWithMultipleDots() {
        // Multiple dots in a name are fine – only ".." (double-dot) is forbidden
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.final.v2.pdf", "application/pdf", new byte[]{1});

        assertThatNoException().isThrownBy(() -> validator.validate(file));
    }
}
