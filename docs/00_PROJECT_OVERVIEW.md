# Builders Companion - Project Overview

**Version:** 1.0.0-dev  
**Minecraft:** 1.20.1  
**Forge:** 47.3.10  
**Java:** 17  
**License:** MIT  

## What This Is

Builders Companion is a suite of interconnected Minecraft mods designed to give modpack creators god-tier control over world generation, portals, and biome distribution.

## The Problem We're Solving

**Current State of Biome Mods:**
- Multiple biome mods (Biomes O' Plenty, Terralith, Regions Unexplored, etc.) fight for the same overworld
- Terrablender tries to blend them but creates "biome soup" - chaotic, unpredictable distribution
- No control over WHERE biomes appear geographically
- Can't isolate mods into separate dimensions
- Can't create realistic climate zones (north=cold, south=hot)

**Our Solution:**
Create custom dimensions where modpack creators have complete control over:
- Which biomes appear (per-mod or individual biomes)
- How common/rare biomes are (weight system)
- WHERE biomes appear (optional climate grid)
- Portal blocks, world borders, terrain generation
- Structure/feature placement rules

## Mod Suite Structure
```
Builders Companion: Core (BC-Core)
├─ Shared APIs for all BC mods
├─ Multi-file YAML config system
├─ Event bus for inter-mod communication
└─ Common utilities

Builders Companion: Dimensions (BC-Dimensions) [PRIORITY]
├─ Custom dimension creation and registration
├─ Three generation modes: Region, Climate Grid, Layered
├─ Biome composition and weight control
├─ Portal system with custom blocks
├─ World borders per dimension
└─ Integration hooks for other BC mods

Builders Companion: Portals (BC-Portals) [FUTURE]
├─ Advanced custom portal system
├─ Overrides BC-Dimensions default portals
└─ Fancy effects and linking options

Builders Companion: Geodes (BC-Geodes) [FUTURE]
├─ Geode variants per biome mod
└─ Dimension-specific geode injection

Builders Companion: Fluids (BC-Fluids) [FUTURE]
├─ Colored water variants (all dyes)
├─ Water properties (luminescent, healing, etc.)
└─ Biome-specific water tinting
```

## Development Phases

### Phase 1: BC-Core (1 week)
Foundation library with APIs, config system, event bus

### Phase 2: BC-Dimensions Foundation (2 weeks)
Basic dimension creation, biome composition, portal system

**Milestone:** Simple dimensions working (Avalon, Terra, Sand World)

### Phase 3: Advanced Dimension Types (1 week)
Nether/End-type dimensions, mirror dimensions

**Milestone:** Nether/End mirrors working

### Phase 4: Advanced Generation (1-2 weeks)
Climate grid mode, layered mode, noise configuration

**Milestone:** Mining dimension and climate-controlled dimensions working

### Phase 5: Integration & Polish (1 week)
Hot reload, documentation, BC:Portals hooks

**Milestone:** BC-Dimensions v1.0 release-ready

## Target User: Modpack Creators

**Use Case Example (Builders Delight 4):**
- Avalon dimension: Magic mods (BWG + Hexerei + Ars Nouveau)
- Terra dimension: Pure Regions Unexplored
- Sand World: Only desert/badlands for resource farming
- Mining Dimension: Layered with hollow world cavern

## Core Principles

1. **Educational Code** - MIT licensed, others learn from this
2. **Comprehensive Documentation** - Javadoc, inline comments, config examples
3. **No Breaking Changes** - Stable APIs for other mods to depend on
4. **Configuration Over Code** - Power users control via YAML, not coding
5. **Fail Gracefully** - Helpful error messages, validate configs, never crash

## Key Innovation: Three Generation Modes

**Region Mode (Terrablender Compatible):**
- Uses existing Terrablender region system
- Control region weights and biome weights
- Traditional Minecraft biome distribution

**Climate Grid Mode (Innovation):**
- Geographic climate control (north=cold, south=hot, west=wet, east=dry)
- Vintage Story-style world boundaries with reversal
- Predictable, realistic biome distribution
- Organic blob shapes (no grid patterns)

**Layered Mode (Vertical Control):**
- Vertical biome layers (for mining dimensions)
- Per-layer ore generation
- Hollow world layers (entirely open caverns)

## Repository Structure (When Created)
```
builders-companion/
├─ bc-core/
│  ├─ src/main/java/
│  └─ src/main/resources/
├─ bc-dimensions/
│  ├─ src/main/java/
│  └─ src/main/resources/
├─ docs/
│  ├─ architecture/
│  ├─ api/
│  └─ examples/
├─ config-examples/
│  ├─ avalon.yml
│  ├─ mining_world.yml
│  └─ ...
└─ README.md
```

## Current Status

**Status:** Architecture Complete, Development Starting  
**Next Step:** Implement BC-Core  
**Timeline:** ~6 weeks to BC-Dimensions v1.0