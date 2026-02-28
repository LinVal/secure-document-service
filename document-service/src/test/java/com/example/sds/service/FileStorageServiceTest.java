package com.example.sds.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService storageService() {
        FileStorageProperties props = new FileStorageProperties(
                tempDir, 10 * 1024 * 1024L, Set.of("application/pdf"));
        return new FileStorageService(props);
    }

    // ── store ─────────────────────────────────────────────────────────────────

    @Test
    void store_shouldCreatePerOrganisationSubdirectory_andReturnRelativePath() throws IOException {
        FileStorageService service = storageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", new byte[]{1, 2, 3});

        String storagePath = service.store(file, "organisation-A");

        assertThat(storagePath).startsWith("organisation-A/");
        assertThat(Files.exists(tempDir.resolve(storagePath))).isTrue();
    }

    @Test
    void store_shouldGenerateUniqueFilenames_forTwoUploads() throws IOException {
        FileStorageService service = storageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "same.pdf", "application/pdf", new byte[]{1});

        String first  = service.store(file, "organisation-A");
        String second = service.store(file, "organisation-A");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void store_shouldPreserveFileExtension_inStoredName() throws IOException {
        FileStorageService service = storageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", new byte[]{1});

        String storagePath = service.store(file, "organisation-A");

        assertThat(storagePath).endsWith(".pdf");
    }

    @Test
    void store_shouldOmitExtension_forFileWithLongExtension() throws IOException {
        // Extensions longer than 10 characters are dropped for safety
        FileStorageService service = storageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "file.verylongextension", "application/pdf", new byte[]{1});

        String storagePath = service.store(file, "organisation-A");

        // No dot after the UUID portion – extension was dropped
        String filename = storagePath.substring(storagePath.lastIndexOf('/') + 1);
        assertThat(filename).doesNotContain(".");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldSilentlySucceed_whenFileDoesNotExist() {
        FileStorageService service = storageService();

        // Should not throw even if the file was never created
        assertThatNoException().isThrownBy(() ->
                service.delete("organisation-A/non-existent-uuid.pdf"));
    }

    @Test
    void delete_shouldRemoveFile_whenItExists() throws IOException {
        FileStorageService service = storageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "todelete.pdf", "application/pdf", new byte[]{1});

        String storagePath = service.store(file, "organisation-A");
        assertThat(Files.exists(tempDir.resolve(storagePath))).isTrue();

        service.delete(storagePath);

        assertThat(Files.exists(tempDir.resolve(storagePath))).isFalse();
    }

    // ── safeResolve – path traversal guard ───────────────────────────────────

    @Test
    void safeResolve_shouldReject_dotDotTraversal() {
        FileStorageService service = storageService();
        Path base = tempDir.toAbsolutePath().normalize();

        assertThatThrownBy(() -> service.safeResolve(base, "../outside"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    void safeResolve_shouldReject_absolutePathEscapingBase() {
        FileStorageService service = storageService();
        Path base = tempDir.toAbsolutePath().normalize();

        assertThatThrownBy(() -> service.safeResolve(base, "/etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void safeResolve_shouldReject_deepDotDotTraversal() {
        FileStorageService service = storageService();
        Path base = tempDir.toAbsolutePath().normalize();

        assertThatThrownBy(() -> service.safeResolve(base, "a/b/../../../../../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void safeResolve_shouldAllow_normalChildPath() {
        FileStorageService service = storageService();
        Path base = tempDir.toAbsolutePath().normalize();

        Path result = service.safeResolve(base, "organisation-A/somefile.pdf");

        assertThat(result.toString()).startsWith(base.toString());
    }

    @Test
    void safeResolve_shouldAllow_normalNestedPath() {
        FileStorageService service = storageService();
        Path base = tempDir.toAbsolutePath().normalize();

        Path result = service.safeResolve(base, "organisation-A");
        Path nested = service.safeResolve(result, "file.pdf");

        assertThat(nested.toString()).startsWith(base.toString());
    }
}
