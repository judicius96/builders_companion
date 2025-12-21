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

/**
 * Global configuration settings for Builders Companion.
 *
 * <p>Loaded from {@code config/builderscompanion/global.yml}.
 *
 * <p>These settings provide defaults and limits that apply to all dimensions
 * unless overridden in individual dimension configs.
 *
 * @since 1.0.0
 */
public class GlobalConfig {

    // Performance & Safety
    public int maxDimensions = 10;
    public int chunkLoadRadius = 8;

    // Default World Border
    public WorldBorderConfig defaultWorldBorder = new WorldBorderConfig();

    // Portal Defaults
    public int defaultPortalOffset = 1;

    // Cave System Defaults
    public String defaultCarver = "minecraft:default";

    // Debugging
    public boolean debugMode = false;
    public boolean logDimensionCreation = true;
    public boolean logBiomePlacement = false;
    public boolean logRegionCompetition = false;

    // Auto-Generation Settings
    public boolean generateRegionReports = true;
    public boolean generateBiomeWeightReports = true;
    public boolean generateDistributionReports = true;

    /**
     * Default world border configuration.
     */
    public static class WorldBorderConfig {
        public int centerX = 0;
        public int centerZ = 0;
        public int radius = 10000;  // Chunks (160,000 blocks)
        public double damagePerBlock = 0.2;
        public int warningDistance = 100;  // Blocks from border
        public int warningTime = 15;  // Seconds
    }
}
