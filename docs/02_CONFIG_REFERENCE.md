# Builders Companion - Complete Configuration Reference

## File Structure
```
config/builderscompanion/
├─ dimensions.yml              # Main config - which dimensions are enabled
├─ global.yml                  # Global defaults and limits
├─ dimensions/                 # Individual dimension configs
│  ├─ avalon.yml
│  ├─ terra.yml
│  ├─ sand_world.yml
│  ├─ mining_world.yml
│  ├─ nether_mirror.yml
│  └─ end_mirror.yml
├─ presets/
│  ├─ noise_presets.yml       # Built-in terrain presets
│  └─ carver_presets.yml      # Cave system presets
└─ bc_dimensions/             # AUTO-GENERATED (read-only)
   ├─ overworld_regions.yml   # Terrablender region analysis
   ├─ avalon_regions.yml
   └─ biome_weights_avalon.yml # Biome spawn probabilities
```

---

## Main Config: `dimensions.yml`
```yaml
# ============================================
# BUILDERS COMPANION: DIMENSIONS
# Main Configuration File
# ============================================

# List of enabled dimensions
# Comment out dimensions you don't want to load
enabled_dimensions:
  - avalon
  - terra
  - sand_world
  - mining_world
  # - nether_mirror  # Disabled example

# Include global settings
include_global_config: true

# Where to find individual dimension configs
dimension_config_directory: "config/builderscompanion/dimensions"
```

---

## Global Config: `global.yml`
```yaml
# ============================================
# GLOBAL SETTINGS
# ============================================

# Performance & Safety
max_dimensions: 10              # Hard limit on custom dimensions
chunk_load_radius: 8            # Chunks kept loaded around portals

# Default World Border (applied unless dimension overrides)
default_world_border:
  center_x: 0
  center_z: 0
  radius: 10000                 # Chunks (160,000 blocks)
  damage_per_block: 0.2
  warning_distance: 100         # Blocks from border
  warning_time: 15              # Seconds

# Portal Defaults
default_portal_offset: 1        # 1:1 coords, 8 = nether-style

# Cave System Defaults
default_carver: "minecraft:default"

# Debugging
debug_mode: false
log_dimension_creation: true
log_biome_placement: false
log_region_competition: false

# Auto-Generation Settings
generate_region_reports: true
generate_biome_weight_reports: true
generate_distribution_reports: true
```

---

## Dimension Config Template

Every file in `dimensions/` follows this structure:
```yaml
# ============================================
# DIMENSION: <NAME>
# ============================================

# Display name (used in UI, commands, messages)
display_name: "Example Dimension"

# ============================================
# PORTAL CONFIGURATION
# ============================================
portal:
  # Frame block (must be a valid block ID)
  frame_block: "minecraft:obsidian"
  
  # Ignition item (what lights the portal)
  ignition_item: "minecraft:flint_and_steel"
  
  # Portal coordinate scaling
  portal_offset: 1
  # 1   = 1:1 mapping with overworld
  # 8   = nether-style (1 block here = 8 in overworld)
  # -8  = reverse nether (8 blocks here = 1 in overworld)
  
  # Portal linking mode
  link_mode: "coordinate"  # "coordinate" or "spawn"
  
  # Optional: Custom portal effects
  ambient_sound: "minecraft:block.portal.ambient"
  travel_sound: "minecraft:block.portal.travel"
  particle_color: "#8B00FF"  # Hex color code

# ============================================
# WORLD BORDER
# ============================================
world_border:
  center_x: 0
  center_z: 0
  radius: 5000              # Chunks
  damage_per_block: 0.2
  warning_distance: 100
  warning_time: 15

# ============================================
# BIOME CONFIGURATION
# ============================================
biomes:
  
  # ==========================================
  # GENERATION MODE (Choose ONE)
  # ==========================================
  mode: "region"  # Options: "region", "climate_grid", "layered"
  
  # ==========================================
  # MODE: REGION (Terrablender Integration)
  # ==========================================
  # Only used if mode = "region"
  
  # Copy biomes from existing dimension
  source_dimension: null  # e.g., "minecraft:the_nether"
  
  # Include vanilla biomes
  include_vanilla: true
  vanilla_dimension: "overworld"  # "overworld", "nether", or "end"
  
  # Include mod biomes
  include_mods:
    - "biomesoplenty:*"         # All BOP biomes
    - "terralith:*"             # All Terralith biomes
    - "hexerei:willow_forest"   # Single biome
  
  # Exclude specific biomes
  exclude_biomes:
    - "biomesoplenty:origin_valley"
  
  # Override Terrablender region weights
  region_overrides:
    "byg:overworld_1": 15       # Increase BWG presence
    "terralith:overworld": 25   # Increase Terralith
  
  # Exclude entire regions
  exclude_regions:
    - "regions_unexplored:overworld_2"
  
  # Override individual biome weights
  weight_overrides:
    "biomesoplenty:cherry_blossom_grove": 2  # Make rare
    "minecraft:plains": 50                    # Make very common
  
  # Generate transparency reports
  generate_region_report: true
  generate_biome_weights: true
  
  # ==========================================
  # MODE: CLIMATE_GRID (Geographic Control)
  # ==========================================
  # Only used if mode = "climate_grid"
  
  climate_grid:
    # Available biomes for climate matching
    available_biomes:
      include_vanilla: true
      include_mods:
        - "biomesoplenty:*"
        - "terralith:*"
      exclude_biomes:
        - "biomesoplenty:origin_valley"
    
    # World center point
    spawn_location: [0, 0]  # [X, Z]
    
    # World boundary (chunks from spawn)
    boundary_chunks: 2500
    
    # Temperature gradient (north-south)
    temperature:
      north: -1.0   # Coldest (ice, tundra)
      south: 1.0    # Hottest (desert, jungle)
      spawn: 0.0    # Moderate (plains, forest)
    
    # Moisture gradient (east-west)
    moisture:
      west: 1.0     # Wettest (swamp, jungle)
      east: -1.0    # Driest (desert, savanna)
      spawn: 0.0    # Moderate (plains)
    
    # Boundary behavior
    reversal: true  # Gradient reverses beyond boundary
    
    # ======================================
    # BIOME BLOB CONTROL
    # ======================================
    
    # Minimum biome size (total chunks, not grid dimensions)
    min_biome_size_chunks: 64
    
    # Blob irregularity (0.0 = circle, 1.0 = fractal)
    blob_irregularity: 0.7
    # 0.0-0.3 = Nearly circular (boring)
    # 0.4-0.7 = Natural variation (recommended)
    # 0.8-1.0 = Very irregular, fractal-like
    
    # Noise scale (frequency of boundary features)
    blob_noise_scale: 0.08
    # 0.02-0.05 = Large smooth features
    # 0.06-0.10 = Medium features (recommended)
    # 0.11-0.20 = Small frequent features
    
    # Blob coherence (0.0 = fragments easily, 1.0 = very cohesive)
    blob_coherence: 0.6
    # 0.0-0.4 = Archipelago-like, many separate patches
    # 0.5-0.7 = Moderate (recommended)
    # 0.8-1.0 = Very unified, few peninsulas
    
    # Transition smoothness between biomes
    transition_width_chunks: 4
    # 1 = Sharp boundaries
    # 4 = Smooth transitions (recommended)
    # 8 = Very gradual blending
    
    # Climate matching strictness
    climate_tolerance: 0.2
    # 0.1 = Very strict (few biomes match each climate)
    # 0.2 = Moderate (recommended)
    # 0.5 = Loose (many biomes can appear anywhere)
  
  # ==========================================
  # MODE: LAYERED (Vertical Biome Control)
  # ==========================================
  # Only used if mode = "layered"
  
  layers:
    # Surface layer
    - name: "Surface"
      y_min: 64
      y_max: 320
      percentage: 100       # % of layer that's solid (vs caves)
      
      biomes:
        - "minecraft:plains"
        - "minecraft:forest"
      
      cave_percentage: 0    # No caves in surface
      cave_biome: null
    
    # Upper mines
    - name: "Upper Mines"
      y_min: 0
      y_max: 64
      percentage: 70        # 70% solid, 30% caves
      
      biomes:
        - "builderscompanion:stone_mine"
        - "builderscompanion:iron_vein"
      
      cave_percentage: 30
      cave_biome: "minecraft:dripstone_caves"
    
    # Deep mines
    - name: "Deep Mines"
      y_min: -32
      y_max: 0
      percentage: 60        # 60% solid, 40% caves
      
      biomes:
        - "builderscompanion:deepslate_mine"
        - "builderscompanion:diamond_vein"
      
      cave_percentage: 40
      cave_biome: "minecraft:lush_caves"
    
    # The Hollow (entirely open layer)
    - name: "The Hollow"
      y_min: -64
      y_max: -32
      percentage: 0         # 0% = entirely hollow
      
      biome_mode: "single"  # Entire layer is one biome
      biome: "builderscompanion:hollow_cavern"
      
      # Hollow layer structure
      ceiling_thickness: 3  # Blocks of stone above
      floor_thickness: 1    # Blocks of bedrock below

# ============================================
# WORLD GENERATION
# ============================================
world_generation:
  
  # Dimension type (affects generation mechanics)
  dimension_type: "overworld"  # "overworld", "nether", "end", "custom"
  
  # ======================================
  # SEED CONFIGURATION
  # ======================================
  seed:
    mirror_overworld: false           # Use overworld's seed
    mirror_dimension: null            # e.g., "minecraft:the_nether"
    custom_seed: null                 # Specific seed or null for random
  
  # ======================================
  # NOISE CONFIGURATION
  # ======================================
  noise:
    mirror_overworld: false           # Use overworld's noise
    mirror_dimension: null            # e.g., "minecraft:the_nether"
    preset: "default"                 # See noise_presets.yml
    # Options: "default", "amplified", "large_biomes", "caves", 
    #          "flat_world", "floating_islands", "custom"
    
    # If preset = "custom", define these:
    custom_settings:
      terrain:
        sea_level: 63
        min_y: -64
        max_y: 320
        vertical_scale: 1.0     # <1 = flatter, >1 = more extreme
        horizontal_scale: 1.0   # <1 = smaller, >1 = larger features
      
      noise_router:
        temperature:
          amplitude: 1.0
          frequency: 0.5
        vegetation:
          amplitude: 1.0
          frequency: 0.5
        continentalness:
          amplitude: 1.0
          frequency: 0.5
        erosion:
          amplitude: 1.0
          frequency: 0.5
        depth:
          amplitude: 1.0
          frequency: 0.5
        weirdness:
          amplitude: 1.0
          frequency: 0.5
  
  # ======================================
  # CAVE CARVER
  # ======================================
  carver:
    type: "minecraft:default"  # Or "yungsapi:better_caves"
    
    # If yungsapi:better_caves:
    config:
      frequency: "normal"           # "low", "normal", "high"
      size_variation: "moderate"    # "small", "moderate", "large"
  
  # ======================================
  # DIMENSION-SPECIFIC SETTINGS
  # ======================================
  
  # Nether-type dimensions
  ceiling_bedrock: false        # Bedrock ceiling (like nether)
  lava_level: 32                # Y level for lava oceans
  ambient_light: 1.0            # 0.0 = dark, 1.0 = normal
  
  # End-type dimensions
  void_level: 0                 # Y level where void starts
  island_generation: false      # Generate floating islands
  central_island: false         # Generate main island with portal
  
  # ======================================
  # AQUIFER SETTINGS
  # ======================================
  aquifers:
    enabled: true
    lava_level: -54
    water_level: 0
  
  # ======================================
  # ORE GENERATION
  # ======================================
  ore_generation:
    mirror_overworld: true      # Use overworld's ore distribution
    
    # OR custom multipliers
    multipliers:
      iron: 1.0
      gold: 1.5                 # 50% more gold
      diamond: 0.8              # 20% less diamond
      emerald: 2.0              # Double emeralds
      copper: 1.0
      coal: 1.0
      lapis: 1.0
      redstone: 1.0
    
    # For layered mode: per-layer ore generation
    per_layer:
      "Upper Mines":
        iron: 5.0
        copper: 3.0
        coal: 10.0
      
      "Deep Mines":
        gold: 4.0
        diamond: 2.0
        redstone: 5.0
        lapis: 3.0
      
      "The Hollow":
        diamond: 0.5
        emerald: 1.0

# ============================================
# STRUCTURES
# ============================================
structures:
  # Copy structures from existing dimension
  source_dimension: null        # e.g., "minecraft:the_nether"
  
  # Only allow structures from mods with biomes in this dimension
  limit_to_parent_mods: true
  
  # Whitelist (overrides parent mod rule)
  include_structures:
    - "minecraft:village"
    - "minecraft:ruined_portal"
    - "biomesoplenty:*"
  
  # Blacklist
  exclude_structures:
    - "minecraft:stronghold"    # No End Portal
    - "minecraft:ancient_city"  # No Warden

# ============================================
# PLACED FEATURES (trees, flowers, ores, etc.)
# ============================================
placed_features:
  limit_to_parent_mods: true
  
  # Density multipliers
  density:
    trees: 1.5          # 50% more trees
    flowers: 2.0        # Double flowers
    grass: 1.2
    ores: 1.0
    mushrooms: 1.0
    structures: 0.5     # Half as many structures

# ============================================
# MOB SPAWNING (Optional)
# ============================================
mob_spawning:
  inherit_from_biomes: true     # Use biome's natural spawns
  
  # OR custom spawn rates
  # ambient: 1.0        # Bats
  # creature: 1.0       # Passive mobs
  # monster: 0.5        # Half as many monsters
  # water_creature: 1.0 # Fish, squid
```

---

## Noise Presets: `presets/noise_presets.yml`
```yaml
# ============================================
# NOISE PRESETS
# ============================================
# Built-in terrain generation presets

presets:
  
  default:
    description: "Vanilla Minecraft terrain"
    # Uses Minecraft's default noise settings
  
  amplified:
    description: "Extreme mountains and deep valleys"
    terrain:
      vertical_scale: 2.0
      horizontal_scale: 1.0
    noise_router:
      continentalness:
        amplitude: 1.5
      erosion:
        amplitude: 1.5
  
  large_biomes:
    description: "Biomes 4x larger than normal"
    terrain:
      horizontal_scale: 4.0
  
  caves:
    description: "Massive cave systems, less solid terrain"
    terrain:
      vertical_scale: 1.2
    noise_router:
      depth:
        amplitude: 1.5
      erosion:
        amplitude: 2.0
  
  flat_world:
    description: "Mostly flat with gentle rolling hills"
    terrain:
      vertical_scale: 0.3
    noise_router:
      continentalness:
        amplitude: 0.5
      erosion:
        amplitude: 0.3
  
  floating_islands:
    description: "Sky islands with void below"
    terrain:
      vertical_scale: 1.5
    noise_router:
      continentalness:
        amplitude: 2.0
        frequency: 0.2
      depth:
        amplitude: 2.0
  
  archipelago:
    description: "Many small islands"
    terrain:
      horizontal_scale: 0.5
    noise_router:
      continentalness:
        amplitude: 1.2
        frequency: 0.8
  
  mesa:
    description: "Flat-topped plateaus and canyons"
    terrain:
      vertical_scale: 1.3
    noise_router:
      erosion:
        amplitude: 2.5
      weirdness:
        amplitude: 0.3
```

---

## Carver Presets: `presets/carver_presets.yml`
```yaml
# ============================================
# CARVER PRESETS
# ============================================
# Cave generation configurations

carvers:
  
  minecraft:default:
    description: "Vanilla Minecraft cave generation"
    type: "minecraft:default"
  
  yungsapi:better_caves:
    description: "Yung's Better Caves - massive interconnected cave systems"
    type: "yungsapi:better_caves"
    requires_mod: "yungsapi"
    
    configs:
      # Frequency presets
      low:
        frequency: "low"
        size_variation: "moderate"
        description: "Fewer caves, normal size"
      
      normal:
        frequency: "normal"
        size_variation: "moderate"
        description: "Vanilla-like cave density with better shapes"
      
      high:
        frequency: "high"
        size_variation: "large"
        description: "Many large caves, good for mining dimensions"
      
      extreme:
        frequency: "high"
        size_variation: "large"
        additional_settings:
          ravine_chance: 0.5
          cavern_chance: 0.3
        description: "Maximum cave generation, swiss cheese world"
```

---

## Auto-Generated Files (Examples)

### Region Report: `bc_dimensions/overworld_regions.yml`
```yaml
# ============================================
# AUTO-GENERATED: TERRABLENDER REGION REPORT
# Dimension: overworld
# Generated: 2024-01-15 16:45:30
# ============================================
# DO NOT EDIT - This file is regenerated on server start
# To modify, edit your dimension config and run /bcreload

dimension: "minecraft:overworld"
total_regions: 7
total_weight: 135

# ============================================
# VANILLA (1 region)
# ============================================
vanilla:
  "minecraft:overworld":
    region_number: 1
    weight: 10
    default_weight: 10
    override_applied: false
    biomes:
      total: 70
      sample:  # First 5 biomes
        - "minecraft:plains" (weight: 10)
        - "minecraft:forest" (weight: 10)
        - "minecraft:desert" (weight: 8)
        - "minecraft:taiga" (weight: 10)
        - "minecraft:swamp" (weight: 7)

# ============================================
# BIOMES O' PLENTY (2 regions)
# ============================================
biomesoplenty:
  "biomesoplenty:bop_overworld_1":
    region_number: 1
    weight: 15
    default_weight: 10
    override_applied: true  # User boosted to 15
    biomes:
      total: 42
      sample:
        - "biomesoplenty:cherry_blossom_grove" (weight: 2, overridden)
        - "biomesoplenty:maple_forest" (weight: 25, overridden)
        - "biomesoplenty:wetland" (weight: 8)

# ... more regions ...

# ============================================
# COMPETITION ANALYSIS
# ============================================
competition_analysis:
  win_percentages:
    "minecraft:overworld": "7.4%"
    "biomesoplenty:bop_overworld_1": "11.1%"
    "byg:overworld_1": "14.8%"
    "terralith:overworld": "18.5%"  # Most dominant
  
  mod_influence:
    "minecraft": "7.4%"
    "biomesoplenty": "18.5%"
    "byg": "33.3%"  # BWG most influential
    "terralith": "18.5%"
  
  recommendations:
    - "⚠ BWG has 33.3% total influence - most dominant mod"
    - "⚠ Vanilla only 7.4% - might feel too modded"
    - "💡 To reduce BWG, lower byg:overworld_1 weight to 10"
    - "💡 To see more vanilla, increase minecraft:overworld to 20"
```

### Biome Weights: `bc_dimensions/biome_weights_avalon.yml`
```yaml
# ============================================
# AUTO-GENERATED: BIOME WEIGHTS REPORT
# Dimension: avalon
# Generated: 2024-01-15 16:45:31
# ============================================
# DO NOT EDIT - Copy desired changes to dimensions/avalon.yml

dimension: "avalon"
total_biomes: 254
total_weight: 2847

# ============================================
# WEIGHT EXPLANATION
# ============================================
# - Higher weight = more common
# - Weight 10 = normal frequency
# - Weight 1 = very rare (mushroom island rarity)
# - Weight 50+ = very common (plains-level)
# - Weight 0 = excluded (won't generate)

# ============================================
# VANILLA BIOMES (70 biomes)
# ============================================
vanilla:
  "minecraft:plains":
    weight: 50
    default_weight: 10
    source: "minecraft"
    temperature: 0.8
    moisture: 0.4
    rarity: "common"
    spawn_probability: "1.76%"  # 50/2847
    override: true
  
  "minecraft:forest":
    weight: 40
    default_weight: 10
    source: "minecraft"
    temperature: 0.7
    moisture: 0.8
    rarity: "common"
    spawn_probability: "1.41%"
    override: true

# ... more biomes ...

# ============================================
# SUMMARY STATISTICS
# ============================================
summary:
  breakdown_by_rarity:
    common: 45        # Weight 20+
    uncommon: 128     # Weight 5-19
    rare: 67          # Weight 2-4
    ultra_rare: 13    # Weight 1
    excluded: 1       # Weight 0
  
  breakdown_by_mod:
    minecraft: 70
    biomesoplenty: 85
    terralith: 95
    hexerei: 4
  
  overridden_biomes: 6
  
  top_10_common:
    - "minecraft:plains" (50, 1.76%)
    - "minecraft:forest" (40, 1.41%)
    - "biomesoplenty:maple_forest" (25, 0.88%)
  
  top_10_rare:
    - "minecraft:mushroom_fields" (1, 0.04%)
    - "terralith:alpha_islands" (1, 0.04%)
    - "biomesoplenty:cherry_blossom_grove" (2, 0.07%)
```

---

## Commands

### `/bc` - Main Command
```
/bc reload              Reload all configs
/bc dimension list      List all custom dimensions
/bc dimension info <id> Show dimension details
/bc dimension report <id> Generate distribution report
/bc biome list <dim>    List all biomes in dimension
/bc biome info <biome>  Show biome details
/bc region list <dim>   List all regions in dimension
/bc debug <on|off>      Toggle debug logging
/bc version             Show mod version
```

### Examples
```
/bc reload
> Reloading Builders Companion configs...
> Loaded 4 dimensions: avalon, terra, sand_world, mining_world
> Generated 4 region reports, 4 biome weight reports
> Reload complete!

/bc dimension info avalon
> Dimension: Avalon
> Mode: region
> Biomes: 254 (vanilla + biomesoplenty + terralith + hexerei)
> Portal: minecraft:polished_diorite
> World Border: 5000 chunks
> Status: Loaded

/bc dimension report avalon
> Generating distribution report for avalon...
> Report saved to: config/builderscompanion/reports/avalon_report.txt
> Top biome: minecraft:plains (1.76% spawn chance)
> Rarest biome: terralith:alpha_islands (0.04% spawn chance)
```

---

## Validation & Error Messages

### Example: Invalid Config

**Config:**
```yaml
# dimensions/test.yml
portal:
  frame_block: "minecraft:polished_diorit"  # Typo
biomes:
  mode: "climate"  # Invalid mode
  include_mods:
    - "fake_mod:*"  # Mod not installed
```

**Error Output:**
```
[ERROR] Failed to load dimension config: dimensions/test.yml

Configuration Errors (must fix to load dimension):

Line 2: portal.frame_block
  ✗ Block 'minecraft:polished_diorit' does not exist
  → Did you mean 'minecraft:polished_diorite'?

Line 5: biomes.mode
  ✗ Invalid mode 'climate'
  → Valid options: 'region', 'climate_grid', 'layered'

Line 7: biomes.include_mods
  ✗ Mod 'fake_mod' is not installed
  → This dimension requires mod 'fake_mod' to function
  → Install the mod or remove it from include_mods

Dimension 'test' was NOT loaded.
Fix the errors above and run /bcreload to try again.
```

### Example: Warnings

**Config:**
```yaml
biomes:
  mode: "climate_grid"
  region_overrides:
    "byg:overworld_1": 20  # Ignored in climate_grid mode
```

**Warning Output:**
```
[WARN] Configuration warnings for dimension 'avalon':

Line 8: region_overrides
  ⚠ Region overrides are set but mode='climate_grid'
  → Region overrides only apply in 'region' mode
  → These settings will be ignored

Dimension 'avalon' loaded successfully, but review warnings above.
```

---

## Tips & Best Practices

### Starting Simple
```yaml
# Minimal valid config - great starting point
display_name: "My Test Dimension"

portal:
  frame_block: "minecraft:stone"

biomes:
  mode: "region"
  include_vanilla: true
  include_mods:
    - "biomesoplenty:*"
```

### Climate Grid Recommended Settings
```yaml
climate_grid:
  boundary_chunks: 2500
  temperature:
    north: -1.0
    south: 1.0
    spawn: 0.0
  moisture:
    west: 1.0
    east: -1.0
    spawn: 0.0
  reversal: true
  min_biome_size_chunks: 64
  blob_irregularity: 0.7      # Natural-looking
  blob_noise_scale: 0.08      # Medium features
  blob_coherence: 0.6         # Stays together moderately
```

### Performance Tips

1. **Limit custom dimensions:** Don't create more than you need
2. **Use world borders:** Prevents infinite chunk generation
3. **Cache-friendly config:** Avoid changing configs frequently
4. **Region mode is faster:** Climate grid requires more calculation

### Common Patterns

**Mirror Dimension (Same Terrain, Different Biomes):**
```yaml
world_generation:
  seed:
    mirror_overworld: true
  noise:
    mirror_overworld: true
```

**Resource Farming Dimension:**
```yaml
world_border:
  radius: 2000  # Smaller = less server load

biomes:
  include_biomes:
    - "minecraft:desert"  # Only specific biomes

ore_generation:
  multipliers:
    iron: 3.0  # Triple iron
```

**Nether Mirror:**
```yaml
biomes:
  source_dimension: "minecraft:the_nether"

world_generation:
  dimension_type: "nether"
  ceiling_bedrock: true
  lava_level: 32
```