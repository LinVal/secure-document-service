package com.example.sds.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores uploaded files outside the web root in a per-organisation directory tree.
 *
 * <h3>Security properties</h3>
 * <ul>
 *   <li><b>UUID filenames</b> – the original filename is NEVER used as the on-disk name.
 *       A random UUID prevents collisions, enumeration, and injection of executable filenames.</li>
 *   <li><b>Per-organisation directories</b> – files are partitioned by organisationId to limit blast radius
 *       if a path-construction bug ever surfaces.</li>
 *   <li><b>Path traversal guard</b> – every constructed path is normalized and verified to stay
 *       inside the configured upload directory. Any attempt to escape results in a rejected
 *       request, not a file written to an unexpected location.</li>
 *   <li><b>No web-root serving</b> – the upload directory is not mapped to any URL.
 *       Files must be served via an authenticated endpoint.</li>
 * </ul>
 */
@Service
public class FileStorageService {

    private final FileStorageProperties properties;

    public FileStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * Persists {@code file} under the configured upload directory.
     *
     * @param file     the validated multipart upload
     * @param organisationId used as a subdirectory name to isolate each organisation on disk
     * @return a relative storage reference ({@code "<organisationId>/<uuid>[.ext]"})
     *         that can be stored in the database and later passed to {@link #delete}
     */
    public String store(MultipartFile file, String organisationId) throws IOException {
        Path base = properties.uploadDirectory().toAbsolutePath().normalize();

        // Create a per-organisation subdirectory and verify it stays inside base
        Path organisationDir = safeResolve(base, organisationId);
        Files.createDirectories(organisationDir);

        // Build storage name from a fresh UUID + original extension (for ops readability)
        String storageName = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        Path target = safeResolve(organisationDir, storageName);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Return a relative path; the base directory is server-side config, not exposed to clients
        return organisationId + "/" + storageName;
    }

    /**
     * Deletes the file identified by the given storage reference.
     * Silently succeeds if the file no longer exists.
     *
     * @param storageRef the value previously returned by {@link #store}
     */
    public void delete(String storageRef) throws IOException {
        Path base = properties.uploadDirectory().toAbsolutePath().normalize();
        Path target = safeResolve(base, storageRef);
        Files.deleteIfExists(target);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Resolves {@code child} against {@code base} and then verifies the result
     * still starts with {@code base}. Throws if a traversal is detected.
     */
    Path safeResolve(Path base, String child) {
        Path resolved = base.resolve(child).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException(
                    "Path traversal detected – access outside upload directory is not allowed");
        }
        return resolved;
    }

    /**
     * Extracts the file extension (including the dot) from {@code filename}.
     * Returns an empty string if no extension is present.
     */
    private static String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        // Only take the last segment after the final dot, capped at 10 chars
        String ext = filename.substring(dot);
        return ext.length() <= 10 ? ext : "";
    }
}
