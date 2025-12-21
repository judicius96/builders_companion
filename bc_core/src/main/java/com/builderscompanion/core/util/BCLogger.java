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
package com.builderscompanion.core.util;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Centralized logging for the Builders Companion mod suite.
 *
 * <p>This class provides a consistent logging interface for all BC modules
 * with support for configurable debug mode. All BC mods should use this logger
 * instead of creating their own logger instances.
 *
 * <p>Debug logging can be enabled in global.yml:
 * <pre>{@code
 * debug_mode: true
 * }</pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * BCLogger.info("Loaded dimension: {}", dimensionName);
 * BCLogger.debug("Climate at ({}, {}): temp={}, moisture={}", x, z, temp, moisture);
 * BCLogger.error("Failed to load config: {}", e.getMessage());
 * }</pre>
 *
 * @since 1.0.0
 */
public class BCLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean debugMode = false;

    /**
     * Sets the debug mode.
     *
     * <p>When debug mode is enabled, calls to {@link #debug(String, Object...)}
     * will be logged. When disabled, debug messages are suppressed for performance.
     *
     * @param enabled true to enable debug logging, false to disable
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        if (enabled) {
            LOGGER.info("Debug mode enabled");
        }
    }

    /**
     * Checks if debug mode is currently enabled.
     *
     * @return true if debug mode is enabled, false otherwise
     */
    public static boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Logs an error message.
     *
     * <p>Use this for critical errors that prevent normal operation.
     *
     * @param message the message format string
     * @param args arguments to the message format
     */
    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }

    /**
     * Logs a warning message.
     *
     * <p>Use this for issues that may cause problems but don't prevent operation.
     *
     * @param message the message format string
     * @param args arguments to the message format
     */
    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    /**
     * Logs an informational message.
     *
     * <p>Use this for important events like dimension registration, config loading, etc.
     *
     * @param message the message format string
     * @param args arguments to the message format
     */
    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    /**
     * Logs a debug message.
     *
     * <p>Debug messages are only logged when debug mode is enabled.
     * Use this for detailed diagnostics like biome selection, climate calculation, etc.
     *
     * <p>Example:
     * <pre>{@code
     * BCLogger.debug("Selected biome {} for climate ({}, {})",
     *     biome.toString(), climate.temperature, climate.moisture);
     * }</pre>
     *
     * @param message the message format string
     * @param args arguments to the message format
     */
    public static void debug(String message, Object... args) {
        if (debugMode) {
            LOGGER.debug(message, args);
        }
    }

    /**
     * Logs a trace message.
     *
     * <p>Trace messages are only logged when trace logging is enabled in the logger configuration.
     * These are for extremely detailed diagnostics and are typically disabled in production.
     *
     * @param message the message format string
     * @param args arguments to the message format
     */
    public static void trace(String message, Object... args) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(message, args);
        }
    }
}
