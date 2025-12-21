# BC:Dimensions - Biome Systems

This document explains how the BiomeMetadataDB and BiomePoolBuilder systems work.

## Overview

BC:Dimensions implements a two-stage biome system:

1. **BiomeMetadataDB** - Captures and indexes all biomes from all mods
2. **BiomePoolBuilder** - Builds dimension-specific biome pools from configuration

## BiomeMetadataDB

### What It Does

The BiomeMetadataDB captures information about every biome registered by all mods. It stores:

- **Biome ID** - The resource location (e.g., `minecraft:plains`)
- **Source Mod** - Which mod registered this biome
- **Climate** - Temperature and moisture (normalized to -1.0 to 1.0)
- **Tags** - All tags applied to this biome
- **Category** - Biome category (PLAINS, FOREST, OCEAN, etc.)

### When It Runs

The database is populated during `ServerAboutToStartEvent`, which fires:
- After all mods have registered their biomes
- After data packs are loaded (so tags are available)
- Before world generation starts

### Example Usage

```java
// Get all biomes from Biomes O' Plenty
List<BiomeMetadata> bopBiomes = BiomeMetadataDB.getBiomesByMod("biomesoplenty");
System.out.println("BOP has " + bopBiomes.size() + " biomes");

// Get a specific biome
BiomeMetadata plains = BiomeMetadataDB.getBiome(
    new ResourceLocation("minecraft", "plains")
);
System.out.println("Plains temperature: " + plains.temperature);
System.out.println("Plains moisture: " + plains.moisture);

// Find biomes matching a climate
List<BiomeMetadata> coldWetBiomes = BiomeMetadataDB.getBiomesByClimate(
    -0.5f,  // Cold temperature
    0.5f,   // High moisture
    0.3     // Tolerance
);
// Results: taiga, snowy_taiga, etc.
```

## BiomePoolBuilder

### What It Does

The BiomePoolBuilder takes a dimension configuration and builds a set of biomes that should generate in that dimension. It handles:

- **Vanilla inclusion** - Include vanilla overworld/nether/end biomes
- **Wildcard patterns** - `biomesoplenty:*` includes all BOP biomes
- **Individual biomes** - `minecraft:plains` includes just plains
- **Exclusions** - Remove specific biomes from the pool
- **Validation** - Warns about missing mods or biomes

### Processing Order

1. Add vanilla biomes (if `include_vanilla: true`)
2. Add wildcard mod biomes (`biomesoplenty:*`)
3. Add individual biomes (`minecraft:plains`)
4. Remove excluded biomes

### Example Configuration

```yaml
biomes:
  mode: "region"

  # Include vanilla overworld biomes
  include_vanilla: true
  vanilla_dimension: "overworld"

  # Include all biomes from these mods
  include_mods:
    - "biomesoplenty:*"
    - "terralith:*"
    - "hexerei:willow_forest"  # Single biome

  # Exclude specific biomes
  exclude_biomes:
    - "biomesoplenty:origin_valley"
    - "minecraft:mushroom_fields"
```

### Example Usage

```java
// Load dimension config
DimensionConfig config = BCConfigLoader.loadDimensionConfig("avalon");

// Build biome pool
Set<Holder<Biome>> biomePool = BiomePoolBuilder.build(config);
System.out.println("Dimension has " + biomePool.size() + " biomes");

// Get statistics
BiomePoolStats stats = BiomePoolBuilder.getStats(biomePool);
System.out.println(stats.getSummary());
// Output:
// Total Biomes: 254
// By Mod:
//   minecraft: 70
//   biomesoplenty: 85
//   terralith: 95
//   hexerei: 4
// By Category:
//   PLAINS: 45
//   FOREST: 62
//   OCEAN: 18
//   ...
```

## Climate System

### Temperature Scale

Minecraft temperatures range from -0.5 (frozen) to 2.0 (hot). We normalize to -1.0 to 1.0:

- **-1.0** - Coldest (ice spikes, frozen peaks)
- **-0.5** - Cold (taiga, snowy plains)
- **0.0** - Moderate (plains, forest)
- **0.5** - Warm (savanna, jungle)
- **1.0** - Hottest (desert, badlands)

### Moisture Scale

Minecraft downfall ranges from 0.0 (dry) to 1.0 (wet). We normalize to -1.0 to 1.0:

- **-1.0** - Driest (desert, badlands)
- **-0.5** - Dry (savanna, plains)
- **0.0** - Moderate (forest)
- **0.5** - Wet (dark forest, swamp)
- **1.0** - Wettest (jungle, rainforest)

### Climate Distance

The climate distance formula is:

```
distance = sqrt((temp1 - temp2)² + (moisture1 - moisture2)²)
```

Examples:
- Plains (temp=0.8, moisture=0.4) vs Desert (temp=2.0, moisture=0.0)
  - Distance = sqrt((0.8-2.0)² + (0.4-0.0)²) = sqrt(1.44 + 0.16) = 1.26
- Plains vs Forest (temp=0.7, moisture=0.8)
  - Distance = sqrt((0.8-0.7)² + (0.4-0.8)²) = sqrt(0.01 + 0.16) = 0.41

### Climate Tolerance

When using climate grid mode, the tolerance determines which biomes can appear:

- **0.1** - Very strict (only exact matches)
- **0.2** - Moderate (recommended for climate grid)
- **0.5** - Loose (many biomes match each location)

## Integration with Dimension Generation

The biome pool is used differently depending on the dimension mode:

### Region Mode
The biome pool is passed to Terrablender regions. Each region filters the pool based on its own logic.

### Climate Grid Mode
For each chunk position:
1. Calculate the climate at that position (temperature + moisture)
2. Query BiomeMetadataDB for biomes matching that climate
3. Filter by the dimension's biome pool
4. Select a biome using weighted random selection

### Layered Mode
Each layer has its own biome list configured. The pool builder validates that those biomes exist.

## Performance Notes

### BiomeMetadataDB
- **Initialization**: O(n) where n = total biomes
- **Queries**: O(1) for ID lookup, O(n) for climate search
- **Memory**: ~200 bytes per biome (minimal)

### BiomePoolBuilder
- **Build Time**: O(n) where n = configured biomes
- **Memory**: Only stores Holder references (8 bytes each)

### Caching
Both systems cache their results:
- BiomeMetadataDB is populated once per server start
- BiomePoolBuilder results can be cached per dimension

## Error Handling

### Missing Mods
When a mod is not loaded, BiomePoolBuilder will:
1. Log a warning: `Mod 'biomesoplenty' is not loaded. Biomes skipped.`
2. Continue processing other biomes
3. The dimension will still generate (but without those biomes)

### Invalid Biome IDs
When a biome doesn't exist:
1. Log a warning: `Biome 'minecraft:invalid' not found.`
2. Skip that biome
3. Continue processing

### Empty Pools
If a biome pool ends up empty:
1. Log a warning: `Biome pool for dimension 'test' is empty!`
2. The dimension may not generate properly
3. This usually indicates a configuration error

## Debugging

### Enable Debug Logging

In `global.yml`:
```yaml
debug_mode: true
```

### What Gets Logged

With debug mode enabled:
- Each biome captured: `Registered biome: minecraft:plains from mod minecraft`
- Mod summary: `minecraft - 70 biomes`
- Pool building: `Added 85 biomes from mod 'biomesoplenty'`
- Climate matching: `Biome modified by event: plains -> forest`

### Common Issues

**Issue**: "BiomeMetadataDB must be initialized"
- **Cause**: Trying to build pool before server starts
- **Fix**: Wait for ServerAboutToStartEvent

**Issue**: "Biome pool is empty"
- **Cause**: No biomes match the configuration
- **Fix**: Check `include_vanilla` or `include_mods` settings

**Issue**: "Mod 'X' not loaded"
- **Cause**: Mod is in config but not installed
- **Fix**: Install the mod or remove from config
