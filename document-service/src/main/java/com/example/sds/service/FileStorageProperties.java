package com.example.sds.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Set;

/**
 * Externalized configuration for file storage.
 * Bound from {@code app.storage.*} in application.yaml.
 *
 * <p>In production, override {@code STORAGE_DIR} with a path that is:
 * <ul>
 *   <li>Outside the servlet container's web root</li>
 *   <li>Not accessible via any URL mapping</li>
 *   <li>On a volume that is backed up / retained across pod restarts</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.storage")
public record FileStorageProperties(

        Path uploadDirectory,

        long maxFileSizeBytes,

        Set<String> allowedContentTypes
) {}
