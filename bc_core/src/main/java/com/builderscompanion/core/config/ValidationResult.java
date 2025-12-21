/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */
package com.builderscompanion.core.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating a dimension configuration.
 *
 * <p>Contains lists of errors (must be fixed) and warnings (should be reviewed).
 * A configuration is considered valid only if it has no errors.
 *
 * <p>Example usage:
 * <pre>{@code
 * ValidationResult result = validator.validate(config);
 * if (!result.isValid()) {
 *     for (String error : result.getErrors()) {
 *         LOGGER.error("Config error: {}", error);
 *     }
 *     return null;
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class ValidationResult {

    private final List<String> errors;
    private final List<String> warnings;

    /**
     * Creates a new validation result.
     */
    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    /**
     * Adds an error to this validation result.
     *
     * <p>Errors indicate critical issues that prevent the config from being used.
     *
     * @param error the error message to add
     */
    public void addError(String error) {
        this.errors.add(error);
    }

    /**
     * Adds a warning to this validation result.
     *
     * <p>Warnings indicate potential issues that may not prevent the config from working,
     * but suggest review or correction.
     *
     * @param warning the warning message to add
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Checks if this configuration is valid.
     *
     * <p>A configuration is valid if it has no errors. Warnings do not affect validity.
     *
     * @return true if there are no errors, false otherwise
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Gets the list of errors.
     *
     * @return unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return List.copyOf(errors);
    }

    /**
     * Gets the list of warnings.
     *
     * @return unmodifiable list of warning messages
     */
    public List<String> getWarnings() {
        return List.copyOf(warnings);
    }

    /**
     * Checks if this result has any warnings.
     *
     * @return true if there are warnings, false otherwise
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
