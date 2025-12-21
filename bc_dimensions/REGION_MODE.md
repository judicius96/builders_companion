# Region Mode - Terrablender Integration

This document explains how Region Mode works in BC:Dimensions and how it integrates with Terrablender for biome distribution.

---

## What is Region Mode?

Region Mode uses Terrablender's sophisticated region competition system to distribute biomes in custom dimensions. This provides traditional Minecraft-style biome placement with support for multiple biome mods.

**Use Region Mode when you want:**
- Traditional Minecraft biome distribution
- To blend multiple biome mods together
- Terrablender's proven region system
- Fine control over biome mod influence via weights

**Don't use Region Mode if you want:**
- Geographic climate zones (use Climate Grid Mode instead)
- Vertical biome layers (use Layered Mode instead)

---

## How Terrablender Regions Work

Terrablender uses a **region competition system**:

1. **Multiple Regions** - Mods register regions containing their biomes
2. **Weighted Competition** - Each region has a weight determining influence
3. **Climate Matching** - Biomes are placed based on climate parameters (temperature, moisture, etc.)
4. **Random Selection** - At each location, Terrablender picks a region based on weights, then selects an appropriate biome from that region

**Example:**
```
Location at (100, 64, 200):
- Climate: temp=0.5 (warm), moisture=-0.3 (dry)
- Competing regions:
  * Biomes O' Plenty (weight 10) → 33% chance
  * Terralith (weight 15) → 50% chance
  * BC Avalon (weight 5) → 17% chance
- Winner: Terralith (won competition)
- Selected biome: Terralith's "Arid Highlands" (matches warm/dry climate)
```

---

## BC:Dimensions Region Mode Implementation

### Architecture

BC:Dimensions creates **one Terrablender region per dimension** that uses Region Mode.

**Flow:**
1. User configures dimension with `mode: region` in YAML
2. BiomePoolBuilder creates a pool of available biomes from config
3. RegionRegistry creates a BCRegion and registers it with Terrablender
4. BCRegion adds all biomes to the region with climate parameters
5. Terrablender handles in-game biome placement

### Files

#### 1. RegionMetadata.java
Data class storing information about a BC region:
- Region name
- Mod ID (always "bc_dimensions" for BC regions)
- Weight (default and current)
- Biomes in the region
- Dimension the region applies to

#### 2. RegionMetadataDB.java
Database tracking all BC regions:
- Stores region metadata
- Applies weight overrides from config
- Provides query methods
- Generates statistics

#### 3. BCRegion.java
Extends Terrablender's `Region` class:
- Implements `addBiomes()` to register biomes with climate parameters
- Converts BC's normalized climate values to Terrablender's multi-dimensional parameters
- Uses BiomeMetadata to determine appropriate climate for each biome

#### 4. RegionRegistry.java
Manages region registration with Terrablender:
- Creates BCRegion instances
- Determines region type (OVERWORLD, NETHER, or THE_END)
- Registers regions with Terrablender during common setup
- Logs transparency reports

#### 5. RegionReportGenerator.java
Generates transparency reports:
- Creates YAML files in `config/builderscompanion/bc_dimensions/`
- Shows region weights, biome counts, influence percentages
- Provides recommendations for tuning

#### 6. DimensionRegistry.java (updated)
Added Region Mode setup:
- Checks if dimension uses `mode: region`
- Builds biome pool
- Calls RegionRegistry to register the region

---

## Configuration

### Basic Region Mode Config

```yaml
biomes:
  mode: "region"

  # Include vanilla biomes
  include_vanilla: true
  vanilla_dimension: "overworld"

  # Add biomes from mods
  include_mods:
    - "biomesoplenty:*"      # All BOP biomes
    - "terralith:*"          # All Terralith biomes

  # Exclude specific biomes
  exclude_biomes:
    - "biomesoplenty:origin_valley"
```

### Weight Overrides

Control how much influence your BC dimension's region has:

```yaml
biomes:
  mode: "region"
  # ... biome selection ...

  # Override region weights
  region_overrides:
    "bc_dimensions:avalon_region": 20  # Boost Avalon's influence
```

**Weight Guidelines:**
- **10** - Standard weight (Terrablender default)
- **5-15** - Typical range for balanced blending
- **20+** - Dominant presence (use sparingly)
- **1-4** - Rare presence (occasional biomes)

**Example Impact:**
```
If total weights = 50:
- Region with weight 10 → 20% influence
- Region with weight 20 → 40% influence
- Region with weight 5 → 10% influence
```

---

## Climate Parameters

Terrablender uses 6 climate parameters for biome placement:

1. **Temperature** (-1.0 to 1.0)
   - -1.0 = Frozen
   - 0.0 = Moderate
   - 1.0 = Hot

2. **Humidity** (-1.0 to 1.0)
   - -1.0 = Dry
   - 0.0 = Normal
   - 1.0 = Wet

3. **Continentalness** (-1.0 to 1.0)
   - -1.0 = Ocean
   - 0.0 = Coast
   - 1.0 = Inland

4. **Erosion** (-1.0 to 1.0)
   - -1.0 = Peaks/Mountains
   - 0.0 = Normal
   - 1.0 = Valleys/Lowlands

5. **Depth** (-1.0 to 1.0)
   - -1.0 = Surface
   - 0.0 = Shallow underground
   - 1.0 = Deep underground

6. **Weirdness** (-1.0 to 1.0)
   - -1.0 = Normal terrain
   - 1.0 = Unusual/chaotic terrain

**BC Implementation:**
- Uses Temperature and Humidity from BiomeMetadata
- Sets wide ranges for Continentalness, Erosion, and Weirdness to allow flexible placement
- Sets Depth to 0.0 (surface only)

---

## Transparency Reports

BC:Dimensions automatically generates transparency reports showing region configuration.

**Location:** `config/builderscompanion/bc_dimensions/<dimension>_regions.yml`

**Example Report:**
```yaml
# BC:Dimensions Region Report
# Generated: 2025-01-15 14:30:00

dimension: "bc_dimensions:avalon"
total_regions: 1
total_weight: 15

regions:
  "bc_dimensions:avalon_region":
    mod: "bc_dimensions"
    weight: 15
    default_weight: 10
    override_applied: true
    influence: "100.0%"
    biome_count: 67
    biomes:
      - "biomesoplenty:cherry_blossom_grove"
      - "biomesoplenty:lavender_field"
      # ... and 65 more

analysis:
  influence_breakdown:
    "bc_dimensions:avalon_region": "100.0%"

  recommendations:
    - "ℹ Only one BC region registered for this dimension"
```

**When Reports Are Generated:**
- Automatically when server starts
- Via `/bc reload` command (future)
- Manually via `/bc region report <dimension>` command (future)

---

## Usage Examples

### Example 1: Avalon (Multiple Mods)

```yaml
# avalon.yml
display_name: "Avalon"

biomes:
  mode: "region"
  include_vanilla: true
  vanilla_dimension: "overworld"
  include_mods:
    - "biomesoplenty:*"
    - "terralith:*"
    - "regions_unexplored:*"
  exclude_biomes:
    - "minecraft:ocean"
    - "minecraft:deep_ocean"
```

**Result:** Blends vanilla overworld biomes with all biomes from BOP, Terralith, and Regions Unexplored.

### Example 2: Terra (Single Mod, Pure Experience)

```yaml
# terra.yml
display_name: "Terra"

biomes:
  mode: "region"
  include_vanilla: false
  include_mods:
    - "terralith:*"
```

**Result:** Only Terralith biomes. Pure Terralith experience in a custom dimension.

### Example 3: Custom Weight

```yaml
# custom.yml
display_name: "Custom Blend"

biomes:
  mode: "region"
  include_mods:
    - "biomesoplenty:*"
    - "byg:*"

  # Make BC region more dominant
  region_overrides:
    "bc_dimensions:custom_region": 25
```

**Result:** BC dimension has higher influence than other regions (if any).

---

## Integration with Other Mods

### Biomes O' Plenty
✅ Works out of the box. BOP registers its own Terrablender regions.

### Terralith
✅ Works out of the box. Terralith registers its own regions.

### Oh The Biomes You'll Go
✅ Works out of the box. BYG registers its own regions.

### Regions Unexplored
✅ Works out of the box. Uses Terrablender.

### BC:Portals
✅ Will use custom portal system if registered via IPortalProvider.

### BC:Fluids
✅ Can modify biomes via IBiomeModifier events.

### BC:Geodes
✅ Will spawn custom geodes in BC dimensions.

---

## Performance

Region Mode is **very efficient**:
- Terrablender handles all biome placement (proven, optimized)
- BC:Dimensions only creates one region per dimension
- No custom chunk generation overhead
- Same performance as vanilla Terrablender usage

---

## Troubleshooting

### No biomes generating
**Check:**
1. Is `mode: "region"` set correctly?
2. Do the biome mods exist and are loaded?
3. Check logs for "biome pool is empty" error
4. Verify biome IDs in `exclude_biomes` are correct

### Only vanilla biomes appearing
**Check:**
1. Are biome mods installed?
2. Check region report - is weight too low?
3. Try increasing weight in `region_overrides`

### Biomes too rare/common
**Solution:** Adjust region weight via `region_overrides`.

Example:
```yaml
region_overrides:
  "bc_dimensions:mydim_region": 20  # Increase from default 10
```

---

## Technical Details

### Region Registration Flow

```
Server Startup
  ↓
FMLCommonSetupEvent
  ↓
DimensionRegistry.onServerStarting()
  ↓
For each dimension with mode=region:
  ├─ BiomePoolBuilder.build() → Set<Holder<Biome>>
  ├─ RegionRegistry.registerRegion()
  │   ├─ Create BCRegion instance
  │   ├─ Regions.register(region) → Terrablender
  │   └─ RegionMetadataDB.registerRegion()
  └─ RegionReportGenerator.generateReport()
  ↓
Server Ready
```

### Climate Mapping

BC BiomeMetadata → Terrablender Climate Parameters:

```java
BiomeMetadata metadata; // temp=-0.5, moisture=0.3

Climate.ParameterPoint point = new Climate.ParameterPoint(
    Climate.Parameter.point(-0.5f),  // Temperature
    Climate.Parameter.point(0.3f),   // Humidity
    Climate.Parameter.span(-0.5f, 1.0f),  // Continentalness (wide)
    Climate.Parameter.span(-1.0f, 1.0f),  // Erosion (any)
    Climate.Parameter.point(0.0f),   // Depth (surface)
    Climate.Parameter.span(-0.5f, 0.5f),  // Weirdness (some variation)
    0L  // offset (unused)
);
```

### Why Wide Parameter Ranges?

BC sets wide ranges for Continentalness, Erosion, and Weirdness to give biomes **flexibility** in placement:
- Allows biomes to appear in varied locations
- Prevents overly strict placement rules
- Mimics vanilla biome behavior
- Better blending with other mods

---

## Future Enhancements

Planned features for Region Mode:

1. **Per-Biome Weight Overrides**
   ```yaml
   weight_overrides:
     "biomesoplenty:cherry_blossom_grove": 5
   ```

2. **Region Exclusion**
   ```yaml
   exclude_regions:
     - "biomesoplenty:bop_overworld_1"
   ```

3. **Commands**
   - `/bc region list` - Show all regions
   - `/bc region info <name>` - Show region details
   - `/bc region report <dimension>` - Generate report

4. **Multi-Region Support**
   - Allow multiple BC regions per dimension
   - Different region weights for different biome groups

---

## Summary

Region Mode provides:
✅ Traditional Terrablender biome distribution
✅ Easy mod blending via YAML config
✅ Weight control for influence tuning
✅ Transparency reports showing exact configuration
✅ Zero performance overhead
✅ Compatible with all Terrablender-based mods

Perfect for users who want **proven biome distribution** with **configuration flexibility** and **mod compatibility**.
