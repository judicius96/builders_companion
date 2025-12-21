# Builders Companion - Example Dimension Configurations

This document provides real-world example configurations for common use cases.

Each example includes:
- Use case description
- Complete dimension config
- Explanation of key settings
- Expected behavior
- Testing tips

---

## Table of Contents

1. [Example 1: Avalon (Magic Mirror Dimension)](#example-1-avalon)
2. [Example 2: Terra (Pure RU Mirror)](#example-2-terra)
3. [Example 3: Sand World (Resource Farming)](#example-3-sand-world)
4. [Example 4: Mining Dimension (Layered + Hollow)](#example-4-mining-dimension)
5. [Example 5: Climate World (Realistic Zones)](#example-5-climate-world)
6. [Example 6: Nether Mirror](#example-6-nether-mirror)
7. [Example 7: End Mirror](#example-7-end-mirror)
8. [Example 8: Skylands (Floating Islands)](#example-8-skylands)
9. [Example 9: Amplified Mountains](#example-9-amplified-mountains)
10. [Example 10: Minimal Test Dimension](#example-10-minimal-test)

---

## Example 1: Avalon (Magic Mirror Dimension) {#example-1-avalon}

### Use Case
Combine all magic mod biomes in one dimension for a modpack where magic is confined to a separate realm.

### Configuration

**File:** `config/builderscompanion/dimensions/avalon.yml`
```yaml
# ============================================
# AVALON - REALM OF MAGIC
# ============================================
# Combines magic mod biomes (Hexerei, Ars Nouveau, etc.)
# with natural biomes from BOP and Terralith

display_name: "Avalon - Realm of Magic"

# ============================================
# PORTAL
# ============================================
portal:
  frame_block: "minecraft:polished_diorite"
  ignition_item: "minecraft:flint_and_steel"
  portal_offset: 1  # 1:1 coordinate mapping
  link_mode: "coordinate"
  
  # Purple particles for magical theme
  particle_color: "#9B59B6"
  ambient_sound: "minecraft:block.portal.ambient"
  travel_sound: "minecraft:block.portal.travel"

# ============================================
# WORLD BORDER
# ============================================
world_border:
  center_x: 0
  center_z: 0
  radius: 5000  # 80,000 blocks diameter
  damage_per_block: 0.2
  warning_distance: 100
  warning_time: 15

# ============================================
# BIOMES - Region Mode
# ============================================
biomes:
  mode: "region"  # Use Terrablender regions
  
  # Include vanilla biomes as base
  include_vanilla: true
  vanilla_dimension: "overworld"
  
  # Include magic and natural biome mods
  include_mods:
    - "biomesoplenty:*"      # All BOP biomes
    - "terralith:*"          # All Terralith biomes
    - "hexerei:*"            # Witch/magic biomes
    - "ars_nouveau:*"        # Ars Nouveau magic forest
    - "ars_elemental:*"      # Elemental biomes
  
  # Exclude overpowered biomes
  exclude_biomes:
    - "biomesoplenty:origin_valley"  # Too many diamonds
  
  # Boost magical atmosphere
  region_overrides:
    "terralith:overworld": 30        # More Terralith (pretty)
    "byg:overworld_1": 10             # Less BWG (if installed)
  
  # Make specific biomes more/less common
  weight_overrides:
    # Common base biomes
    "minecraft:plains": 50
    "minecraft:forest": 40
    
    # More magic biomes
    "hexerei:willow_swamp": 15
    "hexerei:witch_hazel_forest": 8
    
    # Rare beautiful biomes
    "biomesoplenty:cherry_blossom_grove": 2
    "terralith:alpha_islands": 1

# ============================================
# WORLD GENERATION
# ============================================
world_generation:
  dimension_type: "overworld"
  
  seed:
    custom_seed: 42424242  # Consistent world
  
  noise:
    preset: "default"  # Normal terrain
  
  carver:
    type: "yungsapi:better_caves"  # If installed
    config:
      frequency: "normal"
      size_variation: "moderate"
  
  aquifers:
    enabled: true
  
  ore_generation:
    mirror_overworld: true  # Same ores as overworld

# ============================================
# STRUCTURES
# ============================================
structures:
  limit_to_parent_mods: true  # Only structures from included mods
  
  include_structures:
    - "minecraft:village"
    - "minecraft:ruined_portal"
  
  exclude_structures:
    - "minecraft:stronghold"  # No End Portal in Avalon
    - "minecraft:ancient_city"  # No Warden

# ============================================
# PLACED FEATURES
# ============================================
placed_features:
  limit_to_parent_mods: true
  
  density:
    trees: 1.5      # 50% more trees (magical forest vibe)
    flowers: 2.0    # Double flowers
    grass: 1.2
    mushrooms: 1.5  # More mushrooms (witchy)

# ============================================
# MOB SPAWNING
# ============================================
mob_spawning:
  inherit_from_biomes: true  # Use biome defaults
```

### Key Features Explained

**Portal:**
- Polished diorite frame (distinct from nether's obsidian)
- Purple particles for magical aesthetic
- 1:1 coordinates (same position as overworld)

**Biomes:**
- Combines 5 mods + vanilla
- Boosts Terralith for prettier terrain
- Makes witch swamps more common (magic theme)
- Cherry blossoms rare (exploration reward)

**Region Overrides:**
- `terralith:overworld: 30` means Terralith wins ~18% of chunks (increased from default)
- Creates Terralith-heavy world while keeping variety

**Weight Overrides:**
- Plains/forest common (50/40) ensures findable base spots
- Witch biomes increased (15/8) for magic theme
- Rare biomes very rare (1-2) for exploration

### Expected Behavior

**World Feel:**
- Magical but natural
- Plenty of plains/forests for building
- Witch swamps common enough to find
- Rare biomes feel special when discovered

**Biome Distribution:**
- ~50% Terralith
- ~20% BOP
- ~15% Vanilla
- ~15% Magic mods

### Testing Tips
```bash
# 1. Build portal with polished diorite, light it
# 2. Enter Avalon
# 3. Check biome distribution:
/bc dimension report avalon

# 4. Verify witch biomes are common:
/bc biome list avalon | grep hexerei

# 5. Check no strongholds:
/locate structure minecraft:stronghold
# Should say "Could not find that structure nearby"
```

---

## Example 2: Terra (Pure RU Mirror) {#example-2-terra}

### Use Case
Dedicated Regions Unexplored dimension - experience RU as the mod author intended, without interference from other biome mods.

### Configuration

**File:** `config/builderscompanion/dimensions/terra.yml`
```yaml
# ============================================
# TERRA - PURE REGIONS UNEXPLORED
# ============================================
# Showcases Regions Unexplored without any other biome mods

display_name: "Terra - Unexplored Lands"

portal:
  frame_block: "minecraft:mossy_cobblestone"
  portal_offset: 1

world_border:
  radius: 5000

# ============================================
# BIOMES - Pure RU Only
# ============================================
biomes:
  mode: "region"
  
  include_vanilla: false  # NO vanilla biomes
  
  include_mods:
    - "regions_unexplored:*"  # ONLY RU
  
  # Use RU's default weights (no overrides)

# ============================================
# WORLD GENERATION
# ============================================
world_generation:
  dimension_type: "overworld"
  
  seed:
    mirror_overworld: true  # Same terrain shape as overworld
  
  noise:
    mirror_overworld: true  # Same mountains/valleys
  
  carver:
    type: "minecraft:default"

structures:
  limit_to_parent_mods: true  # Only RU structures

placed_features:
  limit_to_parent_mods: true  # Only RU features
```

### Key Features Explained

**Purity:**
- NO vanilla biomes
- NO other mods
- Pure RU experience

**Mirrored Terrain:**
- `mirror_overworld: true` on seed AND noise
- Exact same terrain shape as overworld
- Different biomes painting same landscape

**Why Mirror?**
- Overworld has proven good terrain
- Just want different biomes
- Can compare directly to overworld

### Expected Behavior

**World Feel:**
- 100% Regions Unexplored
- RU's vision fully realized
- No biome soup

**Use Case:**
- Players want "pure" RU experience
- Modpack has other biome mods in overworld
- Terra becomes the "RU showcase"

### Testing Tips
```bash
# Verify only RU biomes:
/bc biome list terra
# Should show ONLY regions_unexplored:* biomes

# Compare coordinates to overworld:
# Go to X=1000, Z=2000 in overworld (note terrain)
# Go to X=1000, Z=2000 in Terra (same mountains, different biomes)
```

---

## Example 3: Sand World (Resource Farming) {#example-3-sand-world}

### Use Case
Dedicated dimension for farming sand, terracotta, and gold without exploring 20+ regions in the overworld.

### Configuration

**File:** `config/builderscompanion/dimensions/sand_world.yml`
```yaml
# ============================================
# SAND WORLD - RESOURCE FARMING
# ============================================
# Only desert and badlands biomes for easy resource gathering

display_name: "Endless Dunes"

portal:
  frame_block: "minecraft:sandstone"
  portal_offset: 1

# ============================================
# SMALLER WORLD BORDER
# ============================================
world_border:
  radius: 2000  # Smaller = less server load for resource dim

# ============================================
# BIOMES - Desert & Badlands Only
# ============================================
biomes:
  mode: "region"
  
  include_vanilla: true
  
  # Explicit biome list (NOT wildcard)
  include_biomes:
    - "minecraft:desert"
    - "minecraft:badlands"
    - "minecraft:eroded_badlands"
    - "minecraft:wooded_badlands"
  
  # No mods, no other vanilla biomes

# ============================================
# WORLD GENERATION
# ============================================
world_generation:
  dimension_type: "overworld"
  
  seed:
    custom_seed: 88888  # Consistent sand world
  
  noise:
    preset: "large_biomes"  # Bigger desert patches
  
  carver:
    type: "minecraft:default"
  
  # More gold (badlands), less iron
  ore_generation:
    multipliers:
      gold: 3.0   # Triple gold
      iron: 0.5   # Half iron
      coal: 0.8
      emerald: 0.0  # No emeralds (not in deserts)

# ============================================
# STRUCTURES - Desert themed
# ============================================
structures:
  include_structures:
    - "minecraft:desert_pyramid"
    - "minecraft:mineshaft"  # Badlands mineshafts
  
  exclude_structures:
    - "minecraft:village"  # No villages (would need water)
    - "minecraft:pillager_outpost"

# ============================================
# PLACED FEATURES
# ============================================
placed_features:
  density:
    cacti: 3.0  # Triple cactus density
    dead_bushes: 2.0
```

### Key Features Explained

**Resource Focus:**
- Only 4 biomes (all have sand/terracotta)
- Smaller world (2000 chunks) saves performance
- Tripled gold (badlands have lots naturally)

**Large Biomes:**
- `preset: "large_biomes"` makes each biome 4x bigger
- Less biome searching
- Larger mining areas

**No Villages:**
- Deserts need water for villages
- This dimension is pure resource farming
- No distractions

### Expected Behavior

**World Feel:**
- Endless desert and badlands
- Easy to find resources
- Harsh, barren (thematic)

**Resource Rates:**
- Sand: Abundant everywhere
- Terracotta: Abundant in badlands
- Gold: 3x normal (mesa biomes)
- Cactus: 3x normal

### Testing Tips
```bash
# 1. Enter sand world
# 2. Fly around - should see ONLY desert/badlands
# 3. Mine in badlands - should find lots of gold
# 4. Check no villages spawn:
/locate structure minecraft:village
# Should fail
```

---

## Example 4: Mining Dimension (Layered + Hollow) {#example-4-mining-dimension}

### Use Case
Dedicated mining dimension with vertical ore layers and a massive hollow cavern for exploration.

### Configuration

**File:** `config/builderscompanion/dimensions/mining_world.yml`
```yaml
# ============================================
# THE MINES - LAYERED MINING DIMENSION
# ============================================
# Vertical ore layers with The Hollow (massive cavern)

display_name: "The Mines"

portal:
  frame_block: "minecraft:crying_obsidian"
  portal_offset: 1

world_border:
  radius: 3000

# ============================================
# BIOMES - Layered Mode
# ============================================
biomes:
  mode: "layered"  # Vertical layers
  
  layers:
    # ========================================
    # LAYER 1: Surface (Y 64 to 320)
    # ========================================
    - name: "Surface"
      y_min: 64
      y_max: 320
      percentage: 100  # Fully solid
      
      biomes:
        - "minecraft:plains"
        - "minecraft:forest"
      
      cave_percentage: 0  # No caves in surface
    
    # ========================================
    # LAYER 2: Upper Mines (Y 0 to 64)
    # ========================================
    - name: "Upper Mines"
      y_min: 0
      y_max: 64
      percentage: 70  # 70% solid, 30% caves
      
      biomes:
        - "builderscompanion:stone_mine"
        - "builderscompanion:iron_vein"
      
      cave_percentage: 30
      cave_biome: "minecraft:dripstone_caves"
    
    # ========================================
    # LAYER 3: Deep Mines (Y -32 to 0)
    # ========================================
    - name: "Deep Mines"
      y_min: -32
      y_max: 0
      percentage: 60  # 60% solid, 40% caves
      
      biomes:
        - "builderscompanion:deepslate_mine"
        - "builderscompanion:diamond_vein"
      
      cave_percentage: 40
      cave_biome: "minecraft:lush_caves"
    
    # ========================================
    # LAYER 4: The Hollow (Y -64 to -32)
    # ========================================
    - name: "The Hollow"
      y_min: -64
      y_max: -32
      percentage: 0  # 0% = entirely hollow
      
      # Single biome for entire layer
      biome_mode: "single"
      biome: "builderscompanion:hollow_cavern"
      
      # Structure of hollow layer
      ceiling_thickness: 3  # Stone ceiling
      floor_thickness: 1    # Bedrock floor

# ============================================
# WORLD GENERATION
# ============================================
world_generation:
  dimension_type: "overworld"
  
  seed:
    custom_seed: 12345
  
  noise:
    preset: "caves"  # More cave-focused terrain
  
  carver:
    type: "yungsapi:better_caves"
    config:
      frequency: "high"
      size_variation: "large"
  
  # Per-layer ore generation
  ore_generation:
    per_layer:
      "Upper Mines":
        iron: 5.0      # 5x iron
        copper: 3.0    # 3x copper
        coal: 10.0     # 10x coal
      
      "Deep Mines":
        gold: 4.0      # 4x gold
        diamond: 2.0   # 2x diamond
        redstone: 5.0  # 5x redstone
        lapis: 3.0
      
      "The Hollow":
        diamond: 0.5   # Some diamonds in hollow
        emerald: 1.0

# ============================================
# STRUCTURES - Mining themed
# ============================================
structures:
  exclude_structures:
    - "*"  # No natural structures

placed_features:
  limit_to_parent_mods: false
  
  density:
    trees: 0.5  # Sparse underground trees in hollow
    mushrooms: 5.0  # Lots of mushrooms
    glowstone: 3.0  # Glowing ceiling

# ============================================
# MOB SPAWNING
# ============================================
mob_spawning:
  inherit_from_biomes: false
  
  # Custom spawn rates
  ambient: 0.0      # No bats
  creature: 0.0     # No passive mobs
  monster: 1.5      # 50% more monsters
  water_creature: 0.0
```

### Key Features Explained

**Layer Structure:**
```
Y 320 ─────────── Surface (plains/forest)
     │
Y 64  ─────────── Upper Mines (70% stone, 30% caves)
     │             Lots of iron, copper, coal
Y 0   ─────────── Deep Mines (60% deepslate, 40% caves)
     │             Lots of gold, diamond, redstone
Y -32 ─────────── The Hollow (100% open)
     │             Massive cavern, sparse features
     │             ███ 3-block stone ceiling
     │             ░░░ 29 blocks of air
     │             ███ 1-block bedrock floor
Y -64 ───────────
```

**The Hollow:**
- Entirely open from Y -63 to Y -30
- Single biome (hollow_cavern)
- 3-block ceiling prevents falling through
- 1-block floor (bedrock)
- 29 blocks of flying space

**Ore Distribution:**
- Upper Mines: Common ores (iron, coal)
- Deep Mines: Rare ores (diamond, gold)
- The Hollow: Minimal ores, exploration focus

### Expected Behavior

**Mining Experience:**
1. Surface: Normal overworld
2. Dig to Y 50: Stone caves with tons of iron
3. Dig to Y -10: Deepslate caves with diamond/gold
4. Dig to Y -33: Break through ceiling into The Hollow
5. The Hollow: Massive open cavern for building/exploring

**Mob Behavior:**
- 50% more monsters (dangerous mining)
- No passive mobs (pure mining)
- Bats disabled (less annoying)

### Testing Tips
```bash
# 1. Enter mining dimension
# 2. Dig straight down from spawn
# 3. At Y 50: Should see lots of iron ore
# 4. At Y -10: Should see lots of diamond
# 5. At Y -33: Should break into massive cavern
# 6. Look up in hollow: Should see stone ceiling
# 7. Look down: Should see bedrock floor

# Verify layers:
/bc layer info mining_world
```

---

## Example 5: Climate World (Realistic Zones) {#example-5-climate-world}

### Use Case
Realistic climate zones where geography matters - go north for snow biomes, south for jungles.

### Configuration

**File:** `config/builderscompanion/dimensions/climate_world.yml`
```yaml
# ============================================
# CLIMATE WORLD - GEOGRAPHIC REALISM
# ============================================
# Realistic climate zones based on coordinates

display_name: "Climate World"

portal:
  frame_block: "minecraft:prismarine"
  portal_offset: 1

world_border:
  radius: 2500

# ============================================
# BIOMES - Climate Grid Mode
# ============================================
biomes:
  mode: "climate_grid"  # Geographic climate control
  
  climate_grid:
    # Available biomes for matching
    available_biomes:
      include_vanilla: true
      include_mods:
        - "biomesoplenty:*"
        - "terralith:*"
    
    # World center
    spawn_location: [0, 0]
    boundary_chunks: 2500  # 40,000 blocks to edge
    
    # ========================================
    # Temperature Gradient (North-South)
    # ========================================
    temperature:
      north: -1.0   # Coldest (ice spikes, tundra)
      south: 1.0    # Hottest (desert, jungle)
      spawn: 0.0    # Temperate (plains, forest)
    
    # ========================================
    # Moisture Gradient (East-West)
    # ========================================
    moisture:
      west: 1.0     # Wettest (rainforest, swamp)
      east: -1.0    # Driest (desert, savanna)
      spawn: 0.0    # Moderate (plains)
    
    # Boundary behavior
    reversal: true  # Gradient reverses beyond boundary
    
    # ========================================
    # Biome Blob Configuration
    # ========================================
    min_biome_size_chunks: 64  # Minimum blob size
    
    blob_irregularity: 0.7
    # 0.7 = highly irregular (natural coastlines)
    
    blob_noise_scale: 0.08
    # 0.08 = medium-sized boundary features
    
    blob_coherence: 0.6
    # 0.6 = moderate cohesion (stays together)
    
    transition_width_chunks: 4
    # 4 chunks of gradual biome transition
    
    climate_tolerance: 0.2
    # 0.2 = moderate strictness for biome matching

# ============================================
# WORLD GENERATION
# ============================================
world_generation:
  dimension_type: "overworld"
  
  seed:
    custom_seed: 99999
  
  noise:
    preset: "default"
  
  carver:
    type: "minecraft:default"

structures:
  limit_to_parent_mods: false  # All structures allowed

placed_features:
  limit_to_parent_mods: false
```

### Key Features Explained

**Climate Zones:**
```
           NORTH (Cold)
              ↑
    Tundra, Ice Plains, Taiga
              
    WET ←   SPAWN    → DRY
   Swamp  Plains/Forest  Desert
   
    Jungle, Rainforest, Savanna
              ↓
           SOUTH (Hot)
```

**Coordinate Examples:**
- Spawn (0, 0): Temperate plains/forest
- North (0, -2500): Ice spikes, tundra
- South (0, 2500): Desert, jungle
- West (-2500, 0): Swamp, rainforest
- East (2500, 0): Desert, savanna
- Northwest (-2500, -2500): Cold + wet = taiga
- Southeast (2500, 2500): Hot + dry = desert

**Boundary Reversal:**
- At 2501 chunks north: Gradient starts reversing
- At 5000 chunks north: Back to temperate
- Creates Vintage Story-style bounded world

**Blob Settings:**
- Irregularity 0.7 = very natural looking
- Coherence 0.6 = stays together but allows peninsulas
- Min size 64 = no tiny patches

### Expected Behavior

**Exploration:**
- Go north → progressively colder biomes
- Go south → progressively hotter biomes
- Go west → progressively wetter biomes
- Go east → progressively drier biomes

**World Feel:**
- Realistic climate zones
- Predictable (can plan expeditions)
- Natural biome shapes
- No sudden changes

### Testing Tips
```bash
# Test climate values:
/bc climate test 0 0
> temp=0.0 (temperate), moisture=0.0 (moderate)

/bc climate test 0 -2500
> temp=-1.0 (cold), moisture=0.0 (moderate)

/bc climate test 2500 2500
> temp=1.0 (hot), moisture=-1.0 (dry)

# Fly to these coordinates and verify biomes match
```

---

## Example 6: Nether Mirror {#example-6-nether-mirror}

### Use Case
Additional nether dimension with modded nether biomes, without crowding the vanilla nether.

### Configuration

**File:** `config/builderscompanion/dimensions/nether_mirror.yml`
```yaml
# ============================================
# NETHER MIRROR - MODDED NETHER
# ============================================
# Vanilla + modded nether biomes

display_name: "The Crimson Wastes"

# ============================================
# PORTAL - Nether-style scaling
# ============================================
portal:
  frame_block: "minecraft:blackstone"
  ignition_item: "minecraft:flint_and_steel"
  portal_offset: 8  # 1:8 scaling (nether-style)
  link_mode: "coordinate"
  particle_color: "#FF0000"  # Red

# ============================================
# SMALLER BORDER (8x scaling)
# ============================================
world_border:
  radius: 2000  # 2000 chunks = 16,000 chunks in overworld

# ============================================
# BIOMES
# ============================================
biomes:
  mode: "region"
  
  # Copy from vanilla nether
  source_dimension: "minecraft:the_nether"
  
  include_vanilla: true
  vanilla_dimension: "nether"
  
  # Add modded nether biomes
  include_mods:
    - "byg:*"          # BYG nether biomes
    - "incendium:*"    # Incendium
  
  exclude_biomes:
    - "minecraft:basalt_deltas"  # Annoying to traverse

# ============================================
# WORLD GENERATION - Nether Type
# ============================================
world_generation:
  dimension_type: "nether"  # Nether-style generation
  
  seed:
    mirror_dimension: "minecraft:the_nether"
  
  noise:
    mirror_dimension: "minecraft:the_nether"
  
  # Nether-specific settings
  ceiling_bedrock: true    # Bedrock ceiling
  lava_level: 32           # Lava ocean at Y 32
  ambient_light: 0.1       # Dim like nether

# ============================================
# STRUCTURES
# ============================================
structures:
  source_dimension: "minecraft:the_nether"
  
  include_structures:
    - "minecraft:nether_fortress"
    - "minecraft:bastion_remnant"
  
  exclude_structures:
    - "minecraft:ruined_portal"  # Already in nether-style dim
```

### Key Features Explained

**Nether-Style:**
- `dimension_type: "nether"` enables nether mechanics
- Ceiling bedrock at Y 127
- Lava oceans at Y 32
- Dim ambient light (0.1)

**Portal Offset:**
- `portal_offset: 8` means 1:8 scaling
- 1 block of travel in nether = 8 in overworld
- Smaller world border (2000 vs 5000)

**Mirrored Terrain:**
- Same seed/noise as vanilla nether
- Different biomes (vanilla + mods)
- Same fortress/bastion locations

### Expected Behavior

**Travel:**
- Build portal at overworld X=8000, Z=0
- Appears at nether X=1000, Z=0
- Return portal appears at overworld X=8000

**World Feel:**
- Exactly like nether (ceiling, lava, darkness)
- Modded biomes mixed with vanilla
- Familiar but expanded

### Testing Tips
```bash
# Test portal scaling:
# 1. Overworld: Build portal at X=1600, Z=0
# 2. Enter nether_mirror
# 3. Check F3: Should be at X=200, Z=0 (1600/8)
# 4. Build return portal
# 5. Exit: Should return to X=1600, Z=0
```

---

## Example 7: End Mirror {#example-7-end-mirror}

### Use Case
Additional end dimension with modded end content, without affecting the vanilla end (dragon fight).

### Configuration

**File:** `config/builderscompanion/dimensions/end_mirror.yml`
```yaml
# ============================================
# END MIRROR - MODDED END
# ============================================
# Vanilla + modded end biomes without dragon

display_name: "The Outer Void"

portal:
  frame_block: "minecraft:purpur_block"
  portal_offset: 1

world_border:
  radius: 5000

# ============================================
# BIOMES
# ============================================
biomes:
  mode: "region"
  
  source_dimension: "minecraft:the_end"
  
  include_vanilla: true
  vanilla_dimension: "end"
  
  include_mods:
    - "betterendforge:*"
    - "nullscape:*"

# ============================================
# WORLD GENERATION - End Type
# ============================================
world_generation:
  dimension_type: "end"  # Floating islands
  
  seed:
    mirror_dimension: "minecraft:the_end"
  
  noise:
    mirror_dimension: "minecraft:the_end"
  
  # End-specific settings
  void_level: 0           # Void below Y 0
  island_generation: true # Generate floating islands
  central_island: false   # NO main island (no dragon)

# ============================================
# STRUCTURES
# ============================================
structures:
  source_dimension: "minecraft:the_end"
  
  exclude_structures:
    - "minecraft:end_city"  # No elytras/shulkers
```

### Key Features Explained

**End-Style:**
- `dimension_type: "end"` enables end mechanics
- Floating islands
- Void below Y 0
- End sky/fog

**No Central Island:**
- `central_island: false` removes main island
- No dragon fight
- Spawn on outer islands instead

**No End Cities:**
- Prevents easy elytra farming
- Keeps vanilla end special
- Pure exploration dimension

### Expected Behavior

**World Feel:**
- Looks like end (void, islands, sky)
- Modded end biomes
- No dragon, no cities
- Peaceful exploration

---

## Example 8: Skylands (Floating Islands) {#example-8-skylands}

### Use Case
Floating island dimension for sky-themed builds.

### Configuration

**File:** `config/builderscompanion/dimensions/skylands.yml`
```yaml
display_name: "Skylands"

portal:
  frame_block: "minecraft:quartz_block"

biomes:
  mode: "region"
  include_vanilla: true

world_generation:
  dimension_type: "overworld"
  
  noise:
    preset: "floating_islands"

---

## Example 9: Amplified Mountains {#example-9-amplified-mountains}

### Use Case
Extreme terrain for mountain builds.

### Configuration

**File:** `config/builderscompanion/dimensions/amplified.yml`
```yaml
display_name: "Mountain Realm"

portal:
  frame_block: "minecraft:granite"

biomes:
  mode: "region"
  
  include_vanilla: true
  
  # Only mountain biomes
  include_biomes:
    - "minecraft:jagged_peaks"
    - "minecraft:frozen_peaks"
    - "minecraft:stony_peaks"
    - "minecraft:meadow"

world_generation:
  noise:
    preset: "amplified"  # Extreme mountains

structures:
  include_structures:
    - "minecraft:pillager_outpost"  # On mountains
```

---

## Example 10: Minimal Test Dimension {#example-10-minimal-test}

### Use Case
Simplest possible config for testing BC:Dimensions installation.

### Configuration

**File:** `config/builderscompanion/dimensions/test.yml`
```yaml
# Minimal valid config
display_name: "Test Dimension"

portal:
  frame_block: "minecraft:stone"

biomes:
  mode: "region"
  include_vanilla: true
```

**That's it!** Everything else uses defaults.

### Testing
```bash
# 1. Start server
# 2. Build stone portal (4x5)
# 3. Light with flint and steel
# 4. Enter
# 5. Check /bc dimension list
```

If this works, BC:Dimensions is installed correctly.

---

## Quick Reference: Config Decision Tree

What do you want?
├─ Combine multiple biome mods
│  └─ Use: Region Mode (Example 1: Avalon)
├─ One mod only, pure experience
│  └─ Use: Region Mode (Example 2: Terra)
├─ Resource farming (specific biomes)
│  └─ Use: Region Mode + explicit biome list (Example 3: Sand World)
├─ Realistic climate zones
│  └─ Use: Climate Grid Mode (Example 5: Climate World)
├─ Vertical ore layers
│  └─ Use: Layered Mode (Example 4: Mining Dimension)
├─ Hollow world cavern
│  └─ Use: Layered Mode + hollow layer (Example 4)
├─ Nether-style dimension
│  └─ Use: dimension_type: "nether" (Example 6)
├─ End-style dimension
│  └─ Use: dimension_type: "end" (Example 7)
└─ Just testing installation
└─ Use: Minimal config (Example 10)