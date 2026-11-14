package com.cse309.cache.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when configuration validation fails, carrying the complete list of validation errors.
 */
public class CacheConfigurationException extends CacheException {

    private static final long serialVersionUID = 1L;

    private final List<String> validationErrors;

    public CacheConfigurationException(List<String> errors) {
        super(
            "Configuration validation failed: " + String.join("; ", errors != null ? errors : Collections.emptyList()),
            "Configuration contains invalid settings:\n - " + String.join("\n - ", errors != null ? errors : Collections.emptyList())
        );
        this.validationErrors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
