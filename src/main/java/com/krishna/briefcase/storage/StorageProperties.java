package com.krishna.briefcase.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code storage.*} settings. @param rootDir Base directory for locally stored files. */
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String rootDir) {
}
