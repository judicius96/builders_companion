# Builders Companion - Implementation Guide for Claude Code

This guide provides step-by-step instructions for implementing BC-Core and BC-Dimensions using Claude Code.

---

## Development Environment Setup

### Prerequisites
```
Minecraft: 1.20.1
Forge: 47.3.10 (or compatible)
Java: 17
IDE: IntelliJ IDEA (recommended) or Eclipse
```

### Project Structure
```
builders-companion/
├─ bc-core/
│  ├─ build.gradle
│  ├─ src/main/java/com/builderscompanion/core/
│  │  ├─ BCCore.java
│  │  ├─ api/
│  │  ├─ config/
│  │  └─ events/
│  └─ src/main/resources/
│     └─ META-INF/mods.toml
├─ bc-dimensions/
│  ├─ build.gradle
│  ├─ src/main/java/com/builderscompanion/dimensions/
│  │  ├─ BCDimensions.java
│  │  ├─ biome/
│  │  ├─ dimension/
│  │  ├─ portal/
│  │  └─ world/
│  └─ src/main/resources/
│     └─ META-INF/mods.toml
└─ settings.gradle
```

---

## Phase 1: BC-Core Implementation (Week 1)

### Objective
Create the foundation library with config system, APIs, and event bus.

### Components to Build

#### 1. Multi-File YAML Config Loader

**File:** `BCConfigLoader.java`

**Requirements:**
- Load main `dimensions.yml` to get enabled dimension list
- Load `global.yml` for defaults
- Load individual dimension configs from `dimensions/` directory
- Validate all configs and provide helpful error messages
- Support hot reload via command

**Key Classes:**
```java
public class BCConfigLoader {
    public static GlobalConfig loadGlobalConfig();
    public static List<String> getEnabledDimensions();
    public static DimensionConfig loadDimensionConfig(String dimensionId);
    public static void reloadAll();
}

public class ConfigValidator {
    public ValidationResult validate(DimensionConfig config);
}

public class ValidationResult {
    public List<String> errors;
    public List<String> warnings;
    public boolean isValid();
}
```

**YAML Library:** Use SnakeYAML (already in Forge)

**Error Handling Example:**
```java
try {
    DimensionConfig config = loadDimensionConfig("avalon");
    ValidationResult result = validator.validate(config);
    
    if (!result.isValid()) {
        for (String error : result.errors) {
            LOGGER.error("Config error in avalon: {}", error);
        }
        return null;
    }
    
    if (!result.warnings.isEmpty()) {
        for (String warning : result.warnings) {
            LOGGER.warn("Config warning in avalon: {}", warning);
        }
    }
    
    return config;
} catch (IOException e) {
    LOGGER.error("Failed to load config: {}", e.getMessage());
    return null;
}
```

---

#### 2. Data Classes for Config

**File:** `DimensionConfig.java`

**Requirements:**
- Mirror YAML structure exactly
- Use Java records or POJOs
- Support all three modes (region, climate_grid, layered)
- Include sensible defaults

**Example Structure:**
```java
public class DimensionConfig {
    public String displayName;
    public PortalConfig portal;
    public WorldBorderConfig worldBorder;
    public BiomeConfig biomes;
    public WorldGenerationConfig worldGeneration;
    public StructureConfig structures;
    public PlacedFeatureConfig placedFeatures;
    public MobSpawningConfig mobSpawning;
    
    // Nested classes
    public static class PortalConfig {
        public String frameBlock;
        public String ignitionItem = "minecraft:flint_and_steel";
        public int portalOffset = 1;
        public String linkMode = "coordinate";
        public String ambientSound = "minecraft:block.portal.ambient";
        public String travelSound = "minecraft:block.portal.travel";
        public String particleColor = "#8B00FF";
    }
    
    public static class BiomeConfig {
        public String mode = "region";
        
        // Region mode fields
        public boolean includeVanilla = false;
        public String vanillaDimension = "overworld";
        public List<String> includeMods = new ArrayList<>();
        public List<String> excludeBiomes = new ArrayList<>();
        public Map<String, Integer> regionOverrides = new HashMap<>();
        public Map<String, Integer> weightOverrides = new HashMap<>();
        
        // Climate grid fields
        public ClimateGridConfig climateGrid;
        
        // Layered mode fields
        public List<BiomeLayer> layers = new ArrayList<>();
    }
    
    // ... more nested classes
}
```

---

#### 3. API Interfaces

**File:** `api/BCDimensionsAPI.java`

**Requirements:**
- Define all public APIs for other mods
- Include comprehensive Javadoc
- Make thread-safe where needed

**Example:**
```java
/**
 * Main API for Builders Companion: Dimensions.
 * 
 * <p>This API allows other mods to:
 * <ul>
 *   <li>Query registered custom dimensions</li>
 *   <li>Access dimension configurations</li>
 *   <li>Register portal providers (for BC:Portals)</li>
 *   <li>Register biome/geode/fluid modifiers</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public class BCDimensionsAPI {
    
    /**
     * Registers a custom dimension with BC:Dimensions.
     * 
     * <p>This should be called during mod initialization, typically in the
     * FMLCommonSetupEvent handler.
     * 
     * @param dimension The dimension resource key
     * @param config The dimension configuration
     * @throws IllegalArgumentException if dimension already registered
     * @throws IllegalStateException if called after initialization phase
     */
    public static void registerDimension(ResourceKey<Level> dimension, DimensionConfig config) {
        // Implementation
    }
    
    /**
     * Gets the configuration for a registered dimension.
     * 
     * @param dimension The dimension resource key
     * @return The dimension config, or null if not registered
     */
    @Nullable
    public static DimensionConfig getDimensionConfig(ResourceKey<Level> dimension) {
        // Implementation
    }
    
    /**
     * Gets all custom dimensions registered by BC:Dimensions.
     * 
     * @return Unmodifiable list of dimension keys
     */
    public static List<ResourceKey<Level>> getAllCustomDimensions() {
        // Implementation
    }
    
    // More API methods...
}
```

**File:** `api/IPortalProvider.java`
```java
/**
 * Interface for mods that want to override BC:Dimensions portal system.
 * 
 * <p>Example: BC:Portals implements this to provide advanced portal features.
 * 
 * <p>To register your portal provider:
 * <pre>{@code
 * BCDimensionsAPI.registerPortalProvider(new MyPortalProvider());
 * }</pre>
 * 
 * @since 1.0.0
 */
public interface IPortalProvider {
    
    /**
     * Checks if this provider should handle the portal at the given position.
     * 
     * <p>BC:Dimensions will call this before attempting to handle a portal.
     * If this returns true, BC:Dimensions will defer to this provider.
     * 
     * @param pos The portal frame position
     * @param level The level containing the portal
     * @return true if this provider handles this portal
     */
    boolean shouldHandlePortal(BlockPos pos, Level level);
    
    /**
     * Teleports an entity through the portal.
     * 
     * <p>This is only called if {@link #shouldHandlePortal} returned true.
     * 
     * @param entity The entity to teleport
     * @param targetDim The target dimension
     */
    void teleportEntity(Entity entity, ResourceKey<Level> targetDim);
}
```

**File:** `api/IBiomeModifier.java`, `api/IGeodeProvider.java`, etc.

Similar structure for other integration interfaces.

---

#### 4. Event System

**File:** `events/BCEvents.java`

**Requirements:**
- Use Forge event bus
- Define custom events for BC mod suite
- Allow other mods to listen and modify behavior

**Example:**
```java
/**
 * Fired when a dimension is registered with BC:Dimensions.
 * 
 * <p>Other mods can listen to this to know when a custom dimension is available.
 */
public class DimensionRegisteredEvent extends Event {
    private final ResourceKey<Level> dimension;
    private final DimensionConfig config;
    
    public DimensionRegisteredEvent(ResourceKey<Level> dimension, DimensionConfig config) {
        this.dimension = dimension;
        this.config = config;
    }
    
    public ResourceKey<Level> getDimension() {
        return dimension;
    }
    
    public DimensionConfig getConfig() {
        return config;
    }
}

/**
 * Fired before a biome is placed in a chunk.
 * 
 * <p>Listeners can modify which biome is placed by changing the biome field.
 * This allows mods like BC:Fluids to swap biomes based on water color.
 */
@Cancelable
public class BiomePlacementEvent extends Event {
    private final ChunkPos pos;
    private Biome biome;  // Mutable
    private final ResourceKey<Level> dimension;
    
    public BiomePlacementEvent(ChunkPos pos, Biome biome, ResourceKey<Level> dimension) {
        this.pos = pos;
        this.biome = biome;
        this.dimension = dimension;
    }
    
    public ChunkPos getPos() { return pos; }
    public Biome getBiome() { return biome; }
    public void setBiome(Biome biome) { this.biome = biome; }
    public ResourceKey<Level> getDimension() { return dimension; }
}
```

---

#### 5. Utilities

**File:** `util/BCLogger.java`
```java
/**
 * Centralized logging for BC mod suite.
 * 
 * <p>Supports configurable log levels via config.
 */
public class BCLogger {
    private static final Logger LOGGER = LogManager.getLogger("BuildersCompanion");
    
    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }
    
    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }
    
    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }
    
    public static void debug(String message, Object... args) {
        if (BCConfig.isDebugMode()) {
            LOGGER.debug(message, args);
        }
    }
    
    public static void trace(String message, Object... args) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(message, args);
        }
    }
}
```

---

### Testing BC-Core

Create test configs in `src/test/resources/`:

**test_dimension.yml:**
```yaml
display_name: "Test Dimension"
portal:
  frame_block: "minecraft:stone"
biomes:
  mode: "region"
  include_vanilla: true
```

**Test loading:**
```java
@Test
public void testConfigLoading() {
    DimensionConfig config = BCConfigLoader.loadDimensionConfig("test_dimension");
    assertNotNull(config);
    assertEquals("Test Dimension", config.displayName);
    assertEquals("minecraft:stone", config.portal.frameBlock);
}

@Test
public void testConfigValidation() {
    DimensionConfig config = new DimensionConfig();
    config.portal = new PortalConfig();
    config.portal.frameBlock = "minecraft:invalid_block";
    
    ValidationResult result = new ConfigValidator().validate(config);
    assertFalse(result.isValid());
    assertTrue(result.errors.stream()
        .anyMatch(e -> e.contains("invalid_block")));
}
```

---

### Claude Code Prompt Template for BC-Core
```
I'm implementing BC-Core, the foundation library for Builders Companion mod suite.

Target Environment:
- Minecraft 1.20.1
- Forge 47.3.10
- Java 17

Requirements:
1. Multi-file YAML config loader
   - Load dimensions.yml, global.yml, and dimensions/*.yml
   - Use SnakeYAML library
   - Validate all configs with helpful error messages
   - Support hot reload

2. Config data classes
   - DimensionConfig with all nested classes
   - Mirror the YAML schema from project knowledge
   - Use sensible defaults

3. API interfaces
   - BCDimensionsAPI (main API)
   - IPortalProvider (for BC:Portals)
   - IBiomeModifier (for BC:Fluids)
   - IGeodeProvider (for BC:Geodes)

4. Event system
   - DimensionRegisteredEvent
   - BiomePlacementEvent
   - PortalActivatedEvent

5. Utilities
   - BCLogger with configurable debug mode
   - Config path validation (prevent traversal)

Code Quality Requirements:
- Comprehensive Javadoc on all public APIs
- Inline comments explaining WHY, not just WHAT
- Debug logging at key points
- MIT license headers on all files
- Educational code (others will learn from this)

Testing:
- Create test configs
- Unit tests for config loading and validation

Please implement BC-Core with full documentation and error handling.
Work through the implementation systematically, testing as you go.
```

---

## Phase 2: BC-Dimensions Foundation (Weeks 2-3)

### Objective
Implement basic dimension creation, biome composition, and portal system.

### Components to Build

#### 1. Biome Metadata System

**File:** `biome/BiomeMetadataDB.java`

**Purpose:** Intercept and store biome registrations from all mods

**Requirements:**
- Hook into Forge biome registry
- Capture: mod source, biome ID, climate tuple, tags
- Provide query API

**Implementation:**
```java
/**
 * Database of all registered biomes with metadata.
 * 
 * <p>This captures biome registrations from all mods during initialization
 * and provides query methods for dimension generation.
 */
public class BiomeMetadataDB {
    
    private static final Map<ResourceLocation, BiomeMetadata> biomes = new ConcurrentHashMap<>();
    
    /**
     * Captures a biome registration.
     * 
     * <p>Called automatically when biomes are registered.
     */
    @SubscribeEvent
    public static void onBiomeRegister(RegistryEvent.Register<Biome> event) {
        event.getRegistry().getEntries().forEach((key, biome) -> {
            String modId = key.location().getNamespace();
            
            BiomeMetadata metadata = new BiomeMetadata(
                key.location(),
                modId,
                extractClimate(biome),
                extractTags(biome)
            );
            
            biomes.put(key.location(), metadata);
            
            BCLogger.debug("Registered biome: {} from mod {}", 
                key.location(), modId);
        });
        
        BCLogger.info("Captured {} biomes from {} mods", 
            biomes.size(), 
            biomes.values().stream()
                .map(m -> m.modId)
                .distinct()
                .count());
    }
    
    /**
     * Gets all biomes from a specific mod.
     */
    public static List<BiomeMetadata> getBiomesByMod(String modId) {
        return biomes.values().stream()
            .filter(m -> m.modId.equals(modId))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets biome metadata by ID.
     */
    @Nullable
    public static BiomeMetadata getBiome(ResourceLocation id) {
        return biomes.get(id);
    }
    
    // More query methods...
}

/**
 * Metadata about a biome.
 */
public class BiomeMetadata {
    public final ResourceLocation id;
    public final String modId;
    public final Climate climate;
    public final Set<TagKey<Biome>> tags;
    
    // Constructor, getters...
}
```

---

#### 2. Biome Pool Builder

**File:** `biome/BiomePoolBuilder.java`

**Purpose:** Build a pool of biomes for a dimension based on config

**Requirements:**
- Handle wildcard patterns (`biomesoplenty:*`)
- Apply include/exclude rules
- Validate all biomes exist

**Implementation:**
```java
/**
 * Builds a pool of biomes for a dimension from config.
 */
public class BiomePoolBuilder {
    
    /**
     * Builds biome pool from dimension config.
     * 
     * @param config The dimension configuration
     * @return Set of biomes available in this dimension
     * @throws IllegalArgumentException if required mods missing
     */
    public static Set<Holder<Biome>> build(DimensionConfig config) {
        Set<Holder<Biome>> pool = new HashSet<>();
        
        // Add vanilla biomes if requested
        if (config.biomes.includeVanilla) {
            String vanillaDim = config.biomes.vanillaDimension;
            pool.addAll(getVanillaBiomes(vanillaDim));
            
            BCLogger.debug("Added {} vanilla {} biomes", 
                pool.size(), vanillaDim);
        }
        
        // Add mod biomes
        for (String pattern : config.biomes.includeMods) {
            if (pattern.endsWith(":*")) {
                // Wildcard: add all biomes from mod
                String modId = pattern.substring(0, pattern.length() - 2);
                
                if (!isModLoaded(modId)) {
                    BCLogger.warn("Mod '{}' not loaded. Biomes skipped.", modId);
                    continue;
                }
                
                List<BiomeMetadata> modBiomes = BiomeMetadataDB.getBiomesByMod(modId);
                pool.addAll(biomesToHolders(modBiomes));
                
                BCLogger.debug("Added {} biomes from mod {}", 
                    modBiomes.size(), modId);
            } else {
                // Single biome
                BiomeMetadata biome = BiomeMetadataDB.getBiome(
                    new ResourceLocation(pattern));
                
                if (biome == null) {
                    BCLogger.warn("Biome '{}' not found. Check spelling or mods.", 
                        pattern);
                    continue;
                }
                
                pool.add(biomeToHolder(biome));
            }
        }
        
        // Remove excluded biomes
        for (String excluded : config.biomes.excludeBiomes) {
            pool.removeIf(h -> h.unwrapKey().get().location()
                .toString().equals(excluded));
        }
        
        BCLogger.info("Built biome pool for dimension: {} biomes", pool.size());
        return pool;
    }
}
```

---

#### 3. Dimension Registration

**File:** `dimension/DimensionRegistry.java`

**Purpose:** Register custom dimensions with Forge

**Requirements:**
- Create dimension from config
- Register with Minecraft's dimension system
- Handle dimension type (overworld/nether/end)

**Implementation Pattern:**
```java
/**
 * Registers custom dimensions with Forge.
 */
public class DimensionRegistry {
    
    @SubscribeEvent
    public static void registerDimensions(RegisterEvent event) {
        // Load enabled dimensions from config
        List<String> enabled = BCConfigLoader.getEnabledDimensions();
        
        for (String dimId : enabled) {
            DimensionConfig config = BCConfigLoader.loadDimensionConfig(dimId);
            
            if (config == null) {
                BCLogger.error("Failed to load config for dimension: {}", dimId);
                continue;
            }
            
            // Validate config
            ValidationResult result = new ConfigValidator().validate(config);
            if (!result.isValid()) {
                BCLogger.error("Invalid config for {}: {}", 
                    dimId, String.join(", ", result.errors));
                continue;
            }
            
            // Register dimension
            ResourceKey<Level> dimensionKey = createDimensionKey(dimId);
            registerDimension(dimensionKey, config);
            
            // Fire event for other mods
            MinecraftForge.EVENT_BUS.post(
                new DimensionRegisteredEvent(dimensionKey, config));
            
            BCLogger.info("Registered dimension: {} ({})", 
                config.displayName, dimId);
        }
    }
    
    private static void registerDimension(ResourceKey<Level> key, 
                                         DimensionConfig config) {
        // Forge dimension registration logic
        // This involves creating LevelStem, NoiseGeneratorSettings, etc.
        // Complex Forge-specific code here
    }
}
```java

/**
 * Portal block for custom dimensions.
 * 
 * <p>This is dynamically created for each dimension based on config.
 */
public class CustomPortalBlock extends NetherPortalBlock {
    
    private final ResourceKey<Level> targetDimension;
    private final DimensionConfig config;
    
    public CustomPortalBlock(ResourceKey<Level> targetDim, DimensionConfig config) {
        super(Properties.of(Material.PORTAL)
            .noCollission()
            .lightLevel(state -> 11)
            .strength(-1.0F)
            .sound(SoundType.GLASS));
        
        this.targetDimension = targetDim;
        this.config = config;
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !entity.canChangeDimensions()) {
            return;
        }
        
        // Check if portal provider should handle this
        IPortalProvider provider = BCDimensionsAPI.getPortalProvider();
        if (provider != null && provider.shouldHandlePortal(pos, level)) {
            provider.teleportEntity(entity, targetDimension);
            return;
        }
        
        // Default teleportation logic
        teleportEntity(entity, level, pos);
    }
    
    /**
     * Teleports entity through portal with coordinate offset.
     */
    private void teleportEntity(Entity entity, Level level, BlockPos portalPos) {
        ServerLevel targetLevel = level.getServer().getLevel(targetDimension);
        
        if (targetLevel == null) {
            BCLogger.error("Target dimension not found: {}", targetDimension);
            return;
        }
        
        // Calculate destination with portal offset
        BlockPos destination = calculateDestination(
            entity.blockPosition(), 
            config.portal.portalOffset);
        
        // Find or create portal at destination
        BlockPos targetPortal = findOrCreatePortal(targetLevel, destination);
        
        // Teleport
        entity.changeDimension(targetLevel, new CustomTeleporter(targetPortal));
        
        // Play sound
        entity.playSound(SoundEvent.createVariableRangeEvent(
            new ResourceLocation(config.portal.travelSound)), 1.0F, 1.0F);
        
        BCLogger.debug("Teleported {} to {} at {}", 
            entity.getName().getString(), targetDimension, targetPortal);
    }
    
    /**
     * Calculates destination coordinates with offset scaling.
     * 
     * Examples:
     * - offset=1: (100, 64, 200) → (100, 64, 200) [1:1]
     * - offset=8: (100, 64, 200) → (12, 64, 25) [nether-style]
     * - offset=-8: (100, 64, 200) → (800, 64, 1600) [reverse nether]
     */
    private BlockPos calculateDestination(BlockPos origin, int offset) {
        if (offset == 1) {
            return origin;  // 1:1 mapping
        }
        
        int destX, destZ;
        if (offset > 0) {
            // Positive offset: divide (nether-style)
            destX = origin.getX() / offset;
            destZ = origin.getZ() / offset;
        } else {
            // Negative offset: multiply (reverse nether)
            destX = origin.getX() * Math.abs(offset);
            destZ = origin.getZ() * Math.abs(offset);
        }
        
        return new BlockPos(destX, origin.getY(), destZ);
    }
}
(continuing from where Phase 3 ended...)

---

## Phase 4: Climate Grid Mode Implementation (Week 5)

### Objective
Implement geographic climate-based biome distribution with organic blob shapes.

### Components to Build

#### 1. Climate Grid Generator

**File:** `biome/ClimateGridGenerator.java`

**Purpose:** Calculate climate (temperature + moisture) for any world coordinate

**Key Algorithm:**
```java
// Pseudocode for climate calculation
Climate getClimateAt(chunkX, chunkZ) {
    // 1. Calculate distance from spawn
    distX = chunkX - spawnX;
    distZ = chunkZ - spawnZ;
    
    // 2. Apply boundary with optional reversal
    if (reversal && distX > boundary) {
        distX = (2 * boundary) - distX;  // Sawtooth pattern
    }
    
    // 3. Calculate gradients
    temperature = interpolate(distZ, tempNorth, tempSouth);
    moisture = interpolate(distX, moistureWest, moistureEast);
    
    // 4. Add noise for variation (~10%)
    temperature += noise(chunkX, chunkZ) * 0.1;
    
    return Climate(temperature, moisture);
}
```

**Requirements:**
- Linear gradients: north→south (temp), west→east (moisture)
- Boundary behavior with reversal (Vintage Story style)
- Noise for natural variation (prevent perfectly linear)
- Efficient - cache climate calculations
- Debug logging for verification

**Testing:**
```java
// Test at spawn: should be moderate (0.0, 0.0)
Climate spawn = generator.getClimateAt(0, 0);
assert spawn.temperature ≈ 0.0;

// Test at north boundary: should be cold (-1.0)
Climate north = generator.getClimateAt(0, -2500);
assert north.temperature ≈ -1.0;

// Test reversal: beyond boundary should reverse
Climate beyond = generator.getClimateAt(0, 2501);
assert beyond.temperature < 1.0;  // Starting to reverse
```

---

#### 2. Climate-Based Biome Selector

**File:** `biome/ClimateBiomeSelector.java`

**Purpose:** Select best matching biome for a climate

**Key Algorithm:**
```java
// Pseudocode for biome selection
Biome selectBiome(climate, chunkX, chunkZ) {
    candidates = [];
    
    // 1. Find biomes within climate tolerance
    for (biome in availableBiomes) {
        distance = sqrt(
            (climate.temp - biome.temp)² + 
            (climate.moisture - biome.moisture)²
        );
        
        if (distance < tolerance) {
            candidates.add(biome, distance);
        }
    }
    
    // 2. Weight by inverse distance (closer = better)
    for (candidate in candidates) {
        candidate.weight = 1.0 / (distance + 0.1);
    }
    
    // 3. Use noise to select from weighted candidates
    noise = getNoise(chunkX, chunkZ);
    return weightedSelect(candidates, noise);
}
```

**Requirements:**
- 2D climate distance (temperature + moisture)
- Climate tolerance configurable (default 0.2)
- Weighted selection using noise (prevents grid patterns)
- Fallback to plains if no matches
- Normalize Minecraft biome climate values

**Climate Distance Formula:**
```
distance = sqrt((tempA - tempB)² + (moistureA - moistureB)²)

Examples:
- Plains (temp=0.8, moisture=0.4) vs Desert (temp=2.0, moisture=0.0)
  = sqrt((0.8-2.0)² + (0.4-0.0)²) = sqrt(1.44 + 0.16) = 1.26

- Tolerance 0.2 = very strict (few matches)
- Tolerance 0.5 = loose (many matches)
```

---

#### 3. Organic Blob Shape Enforcer

**File:** `biome/OrganicBlobEnforcer.java`

**Purpose:** Enforce minimum biome size with natural, irregular shapes

**CRITICAL REQUIREMENTS:**
- ❌ NO grid-based enforcement (creates visible squares)
- ✅ Spiral search pattern (creates organic shapes)
- ✅ Noise-based boundaries (irregular edges)
- ✅ Distance-based thresholds (fuzzy blob edges)

**Key Algorithm:**
```java
// Pseudocode for organic blob enforcement
Biome enforceBlob(chunkX, chunkZ, selectedBiome) {
    // 1. Find nearby blob using SPIRAL search (not grid)
    nearbyBlob = spiralSearch(chunkX, chunkZ);
    
    if (nearbyBlob != null) {
        // 2. Use noise to decide if joining blob
        noise = boundaryNoise(chunkX, chunkZ);
        threshold = calculateThreshold(distanceFromBlobCenter);
        
        // Near center: low threshold (easy to join)
        // Far from center: high threshold (hard to join)
        
        if (noise > threshold) {
            return nearbyBlob;  // Join existing blob
        }
    }
    
    // 3. Check if selected would form valid blob
    if (estimatedBlobSize(selectedBiome) < minSize) {
        // Too small - extend nearby dominant biome
        return findDominantNearby(chunkX, chunkZ);
    }
    
    return selectedBiome;  // Start new blob
}
```

**Spiral Search Pattern:**
```java
// Search in expanding circles, NOT grid squares
for (radius = 1 to searchRadius) {
    for (angle = 0° to 360° step 15°) {
        dx = radius * cos(angle);
        dz = radius * sin(angle);
        
        checkNeighbor(chunkX + dx, chunkZ + dz);
    }
}

// This creates circular tendency but noise makes irregular
```

**Join Threshold Calculation:**
```java
// Threshold varies by distance from blob center
double calculateJoinThreshold(x, z, blobBiome) {
    center = findBlobCenter(blobBiome);
    distance = sqrt((x - center.x)² + (z - center.z)²);
    expectedRadius = sqrt(minSize / π);
    normalizedDist = distance / expectedRadius;
    
    // Base threshold increases with distance
    baseThreshold = 0.2 + (normalizedDist * 0.6);
    
    // Apply irregularity (more variation = more complex shapes)
    threshold = baseThreshold * (1.0 + irregularity * 0.5);
    
    // Apply coherence (lower = breaks apart easier)
    threshold *= (2.0 - coherence);
    
    return clamp(threshold, 0.0, 1.0);
}

// Examples:
// At center (dist=0): threshold ≈ 0.2 (easy to join, blob grows)
// At edge (dist=1.0): threshold ≈ 0.8 (hard to join, blob stops)
// High irregularity: more variation in threshold = complex shapes
```

**Configuration Parameters:**
```yaml
min_biome_size_chunks: 64    # Total chunks (not grid dimensions)
blob_irregularity: 0.7       # 0.0=circle, 1.0=fractal
blob_noise_scale: 0.08       # Frequency of boundary features
blob_coherence: 0.6          # How well blob stays together

# Result with these settings:
# - Blobs ~64 chunks total
# - Highly irregular boundaries (0.7)
# - Medium-sized boundary features (0.08)
# - Moderate cohesion (0.6)
# - Looks like reference image provided by user
```

---

#### 4. Climate Grid Biome Source

**File:** `world/ClimateGridBiomeSource.java`

**Purpose:** BiomeSource implementation that uses climate grid

**Integration:**
```java
public class ClimateGridBiomeSource extends BiomeSource {
    
    private final ClimateGridGenerator climateGen;
    private final ClimateBiomeSelector biomeSelector;
    private final OrganicBlobEnforcer blobEnforcer;
    
    @Override
    public Biome getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // 1. Get climate at this position
        Climate climate = climateGen.getClimateAt(x, z);
        
        // 2. Select best matching biome
        Biome selected = biomeSelector.selectBiome(climate, x, z);
        
        // 3. Enforce minimum blob size with organic shapes
        Biome final = blobEnforcer.enforceBlob(x, z, selected);
        
        return final;
    }
}
```

**Requirements:**
- Implements Minecraft's BiomeSource interface
- Integrates all three climate grid components
- Y coordinate ignored (horizontal biome distribution only)
- Cache aggressively for performance

---

### Testing Climate Grid Mode

**Test Dimension Config:**
```yaml
# config/builderscompanion/dimensions/climate_test.yml
display_name: "Climate Test"

portal:
  frame_block: "minecraft:stone"

biomes:
  mode: "climate_grid"
  
  climate_grid:
    available_biomes:
      include_vanilla: true
    
    spawn_location: [0, 0]
    boundary_chunks: 1000  # Smaller for testing
    
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
    blob_irregularity: 0.7
    blob_noise_scale: 0.08
    blob_coherence: 0.6
```

**In-Game Testing:**
1. Create dimension and enter via portal
2. Fly north → should get progressively colder biomes (tundra, ice)
3. Fly south → should get progressively hotter biomes (desert, jungle)
4. Fly west → should get wetter biomes (swamp, rainforest)
5. Fly east → should get drier biomes (savanna, desert)
6. Check biome shapes from map view → should be organic, not grid-like
7. Find biome boundaries → should be irregular with peninsulas/bays

**Debug Commands:**
```
/bc climate test <x> <z>
> Climate at (100, -500): temp=-0.42 (cool), moisture=0.15 (moderate)
> Expected biomes: taiga, birch_forest, forest
> Actual biome: taiga

/bc climate verify <dimension>
> Verifying climate grid for climate_test...
> North boundary (-1000): temp=-0.98 ✓
> South boundary (1000): temp=0.97 ✓
> Reversal at 1001: temp=0.96 ✓
> Climate grid working correctly
```

---

### Claude Code Prompt Template for Climate Grid
```
I'm implementing Climate Grid Mode for BC-Dimensions (Phase 4).

Context:
- BC-Core and BC-Dimensions foundation are complete
- Region mode is working
- Now adding climate-based biome distribution

Components to implement:

1. ClimateGridGenerator
   - Calculate climate (temp + moisture) from world coordinates
   - North-south temperature gradient
   - East-west moisture gradient  
   - Boundary with reversal behavior
   - Noise for natural variation

2. ClimateBiomeSelector
   - Find biomes matching climate within tolerance
   - Weight by climate distance
   - Use noise for selection (prevent grid patterns)
   - Fallback to plains if no matches

3. OrganicBlobEnforcer
   - CRITICAL: Enforce minimum biome size with organic shapes
   - Use spiral search (NOT grid)
   - Noise-based boundaries (irregular edges)
   - Distance-based join thresholds
   - Result must look like natural coastlines, NOT squares

4. ClimateGridBiomeSource
   - BiomeSource implementation
   - Integrate all three components
   - Cache for performance

Reference the architecture in 03_IMPLEMENTATION_GUIDE.md for:
- Algorithm pseudocode
- Configuration parameters
- Testing requirements

Target: Minecraft 1.20.1, Forge 47.3.10

CRITICAL: The organic blob enforcer must create shapes like the reference
image (irregular, natural, with peninsulas and bays). NO grid patterns.

Code Quality:
- Full Javadoc with algorithm explanations
- Debug logging for climate values and biome selection
- Performance optimization (caching)
- MIT license headers

Please implement these components with full documentation.
```

---

## Phase 5: Layered Mode Implementation (Week 6)

### Objective
Implement vertical biome layers for mining dimensions with hollow world support.

### Components to Build

#### 1. Layered Biome Source

**File:** `biome/LayeredBiomeSource.java`

**Purpose:** BiomeSource that provides different biomes at different Y levels

**Key Algorithm:**
```java
// Pseudocode for layered biome selection
Biome getNoiseBiome(x, y, z, sampler) {
    worldY = y * 4;  // Convert quart-pos to world Y
    
    // 1. Find which layer this Y is in
    layer = findLayer(worldY);
    
    if (layer == null) {
        return defaultBiome;
    }
    
    // 2. Check for single-biome layer (The Hollow)
    if (layer.biomeMode == "single") {
        return layer.singleBiome;
    }
    
    // 3. Determine cave vs solid
    if (shouldBeCave(x, z, layer)) {
        return layer.caveBiome;
    }
    
    // 4. Select from layer's biome pool
    return selectFromLayer(x, z, layer);
}
```

**Layer Finding:**
```java
BiomeLayer findLayer(worldY) {
    for (layer in layers) {
        if (worldY >= layer.yMin && worldY <= layer.yMax) {
            return layer;
        }
    }
    return null;  // Outside all layers
}
```

**Cave Determination:**
```java
boolean shouldBeCave(x, z, layer) {
    if (layer.cavePercentage == 0) {
        return false;  // No caves in this layer
    }
    
    // Use noise to determine cave vs solid
    noise = caveNoise.getValue(x * 0.05, z * 0.05);
    
    // If noise < percentage, it's a cave
    return noise < (layer.cavePercentage / 100.0);
}

// Examples:
// cavePercentage = 30, noise = 0.25 → cave (0.25 < 0.30)
// cavePercentage = 30, noise = 0.45 → solid (0.45 > 0.30)
```

**Requirements:**
- Query Y coordinate to determine layer
- Support single-biome layers (The Hollow)
- Support cave percentage per layer
- Different biome pools per layer
- Efficient Y-based lookups

---

#### 2. Hollow Layer Chunk Generator

**File:** `world/HollowLayerChunkGenerator.java`

**Purpose:** Generate hollow world layers (entirely open caverns)

**Key Algorithm:**
```java
// Pseudocode for hollow layer generation
void generateHollowLayer(chunk, hollowLayer) {
    for (x = 0 to 15) {
        for (z = 0 to 15) {
            // Build ceiling
            for (y = layer.yMax - ceilingThickness to layer.yMax) {
                setBlock(x, y, z, STONE);
            }
            
            // Hollow space (air) - do nothing
            // Y from (yMin + floorThickness) to (yMax - ceilingThickness)
            
            // Build floor
            for (y = layer.yMin to layer.yMin + floorThickness) {
                setBlock(x, y, z, BEDROCK);
            }
            
            // Set biome for entire column
            for (y = layer.yMin to layer.yMax) {
                setBiome(x, y, z, hollowLayer.singleBiome);
            }
        }
    }
}
```

**Ceiling & Floor Structure:**
```
Y -29: ████ STONE (ceiling)
Y -30: ████ STONE
Y -31: ████ STONE
Y -32: ████ STONE (3 blocks thick)
-------------------------
Y -33: ░░░░ AIR (hollow space)
Y -34: ░░░░ AIR
...
Y -62: ░░░░ AIR
Y -63: ░░░░ AIR (31 blocks of open air)
-------------------------
Y -64: ████ BEDROCK (floor, 1 block thick)

Total layer: Y -64 to -29 (36 blocks)
Hollow space: 31 blocks of air
```

**Requirements:**
- Custom chunk generation overriding normal terrain
- Configurable ceiling/floor thickness
- Entire layer uses single biome
- Features can generate in hollow space (trees, glowstone, etc.)
- Structure generation compatible

---

#### 3. Layered Ore Generator

**File:** `world/LayeredOreGenerator.java`

**Purpose:** Generate ores with different rates per layer

**Key Algorithm:**
```java
// Pseudocode for per-layer ore generation
void generateOres(chunk, dimension) {
    for (layer in dimension.layers) {
        oreConfig = layer.oreGeneration;
        
        for (oreType in oreConfig) {
            multiplier = oreConfig.get(oreType);
            baseRate = getVanillaOreRate(oreType);
            
            actualRate = baseRate * multiplier;
            
            generateOreInLayer(
                chunk,
                oreType,
                actualRate,
                layer.yMin,
                layer.yMax
            );
        }
    }
}
```

**Example Configuration:**
```yaml
layers:
  - name: "Upper Mines"
    y_min: 0
    y_max: 64
    ore_generation:
      iron: 5.0    # 5x vanilla iron in this layer
      copper: 3.0
      coal: 10.0
  
  - name: "Deep Mines"
    y_min: -32
    y_max: 0
    ore_generation:
      gold: 4.0    # 4x vanilla gold
      diamond: 2.0 # 2x vanilla diamond
```

**Requirements:**
- Override vanilla ore generation
- Per-layer ore multipliers
- Respect Y-level ranges
- Compatible with other ore gen mods

---

#### 4. Layer Configuration Validator

**File:** `config/LayerConfigValidator.java`

**Purpose:** Validate layered mode configurations

**Validation Rules:**
```java
// Pseudocode for layer validation
ValidationResult validateLayers(List<BiomeLayer> layers) {
    errors = [];
    warnings = [];
    
    // 1. Check for gaps in Y coverage
    layers.sortByYMin();
    for (i = 0 to layers.size - 2) {
        if (layers[i].yMax + 1 < layers[i+1].yMin) {
            warnings.add("Gap between layers " + i + " and " + (i+1));
        }
    }
    
    // 2. Check for overlaps
    for (i = 0 to layers.size - 2) {
        if (layers[i].yMax >= layers[i+1].yMin) {
            errors.add("Layers " + i + " and " + (i+1) + " overlap");
        }
    }
    
    // 3. Validate hollow layers
    for (layer in layers) {
        if (layer.percentage == 0 && layer.biomeMode != "single") {
            errors.add("Layer " + layer.name + ": percentage=0 requires biomeMode='single'");
        }
        
        if (layer.biomeMode == "single" && layer.singleBiome == null) {
            errors.add("Layer " + layer.name + ": single mode requires 'biome' field");
        }
    }
    
    // 4. Check ceiling/floor thickness
    for (layer in layers) {
        totalThickness = layer.ceilingThickness + layer.floorThickness;
        layerHeight = layer.yMax - layer.yMin;
        
        if (totalThickness >= layerHeight) {
            errors.add("Layer " + layer.name + ": ceiling+floor thicker than layer");
        }
    }
    
    return new ValidationResult(errors, warnings);
}
```

**Example Error Messages:**
```
[ERROR] Layer configuration errors:

Line 15 (Upper Mines) and Line 23 (Deep Mines): Layers overlap
  Upper Mines: Y 0 to 64
  Deep Mines: Y -32 to 0
  Overlap at Y 0
  → Adjust y_max of Upper Mines to -1 or y_min of Deep Mines to 1

Line 45 (The Hollow): percentage=0 requires biomeMode='single'
  → Add: biome_mode: "single"
  → Add: biome: "builderscompanion:hollow_cavern"

Line 50 (The Hollow): ceiling_thickness (5) + floor_thickness (3) = 8
  Layer height: Y -64 to -32 = 32 blocks
  Leaves only 24 blocks for hollow space
  → Consider reducing ceiling/floor thickness
```

---

### Testing Layered Mode

**Test Dimension Config:**
```yaml
# config/builderscompanion/dimensions/layered_test.yml
display_name: "Layered Test"

portal:
  frame_block: "minecraft:stone"

biomes:
  mode: "layered"
  
  layers:
    # Surface
    - name: "Surface"
      y_min: 64
      y_max: 320
      percentage: 100
      biomes:
        - "minecraft:plains"
    
    # Mixed layer (70% solid, 30% caves)
    - name: "Mixed"
      y_min: 0
      y_max: 64
      percentage: 70
      biomes:
        - "minecraft:stone"
      cave_percentage: 30
      cave_biome: "minecraft:dripstone_caves"
    
    # Hollow layer
    - name: "The Hollow"
      y_min: -64
      y_max: 0
      percentage: 0
      biome_mode: "single"
      biome: "minecraft:lush_caves"
      ceiling_thickness: 3
      floor_thickness: 1

world_generation:
  ore_generation:
    per_layer:
      "Mixed":
        iron: 5.0
      "The Hollow":
        diamond: 2.0
```

**In-Game Testing:**
1. Enter dimension, dig down from surface
2. Y 64-320: Should be plains biome
3. Y 0-64: Should be mix of stone and dripstone caves
4. Y -1 to -62: Should be entirely open (hollow)
5. Y -62 to -59: Should be stone ceiling
6. Y -64: Should be bedrock floor
7. Check F3 screen: biome should change at layer boundaries

**Debug Commands:**
```
/bc layer info <dimension>
> Dimension: layered_test
> Layers: 3
>
> Layer 1: Surface (Y 64 to 320)
>   Biomes: 1 (plains)
>   Solid: 100%, Caves: 0%
>
> Layer 2: Mixed (Y 0 to 64)
>   Biomes: 1 (stone)
>   Cave biome: dripstone_caves
>   Solid: 70%, Caves: 30%
>
> Layer 3: The Hollow (Y -64 to 0)
>   Mode: single
>   Biome: lush_caves
>   Ceiling: 3 blocks, Floor: 1 block
>   Hollow space: 61 blocks

/bc layer test <x> <y> <z>
> Position: (100, -50, 200)
> Layer: The Hollow
> Expected: lush_caves (hollow space)
> Actual: lush_caves ✓
```

---

### Claude Code Prompt Template for Layered Mode
```
I'm implementing Layered Mode for BC-Dimensions (Phase 5).

Context:
- BC-Core, foundation, region mode, and climate grid mode are complete
- Now adding vertical biome layers for mining dimensions

Components to implement:

1. LayeredBiomeSource
   - BiomeSource that varies biomes by Y level
   - Find layer based on world Y coordinate
   - Support single-biome layers
   - Support cave percentage per layer
   - Select from layer's biome pool

2. HollowLayerChunkGenerator
   - Generate entirely hollow layers (The Hollow)
   - Create ceiling (configurable thickness)
   - Create floor (configurable thickness)
   - Hollow space in between (air)
   - Set biome for entire hollow layer

3. LayeredOreGenerator
   - Per-layer ore generation multipliers
   - Override vanilla ore gen
   - Respect layer Y boundaries
   - Apply configured multipliers

4. LayerConfigValidator
   - Validate layer configurations
   - Check for gaps and overlaps
   - Validate hollow layer requirements
   - Check ceiling/floor thickness
   - Provide helpful error messages

Reference the architecture in 03_IMPLEMENTATION_GUIDE.md for:
- Algorithm pseudocode
- Layer structure examples
- Validation rules
- Testing requirements

Key Use Case (from user):
Mining dimension with:
- Surface layer (Y 64-320)
- Upper mines (Y 0-64, 70% solid, 30% caves)
- Deep mines (Y -32-0, 60% solid, 40% caves)
- The Hollow (Y -64 to -32, entirely open cavern)

Target: Minecraft 1.20.1, Forge 47.3.10

Code Quality:
- Full Javadoc with examples
- Debug logging for layer selection
- Comprehensive error messages
- MIT license headers

Please implement these components with full documentation.
```

---

## Phase 6: Integration & Polish (Week 7)

### Objective
Connect all pieces, add commands, hot reload, and final documentation.

### Components to Build

#### 1. Reload Command

**File:** `command/BCReloadCommand.java`

**Purpose:** Reload configs without restarting server

**Implementation:**
```java
// Pseudocode for reload command
@Command("bcreload")
@Permission(level = 2)  // OP only
void executeReload(CommandContext ctx) {
    ctx.sendMessage("Reloading Builders Companion configs...");
    
    try {
        // 1. Reload global config
        GlobalConfig global = BCConfigLoader.reloadGlobalConfig();
        
        // 2. Reload dimension configs
        List<String> enabled = BCConfigLoader.getEnabledDimensions();
        int loaded = 0;
        int failed = 0;
        
        for (String dimId : enabled) {
            try {
                DimensionConfig config = BCConfigLoader.loadDimensionConfig(dimId);
                
                // Validate
                ValidationResult result = new ConfigValidator().validate(config);
                if (!result.isValid()) {
                    ctx.sendError("Dimension " + dimId + " has errors: " + 
                        result.errors.get(0));
                    failed++;
                    continue;
                }
                
                // Re-register dimension (if already loaded)
                if (isDimensionLoaded(dimId)) {
                    updateDimension(dimId, config);
                }
                
                loaded++;
            } catch (Exception e) {
                ctx.sendError("Failed to load " + dimId + ": " + e.getMessage());
                failed++;
            }
        }
        
        // 3. Clear caches
        clearBiomeCaches();
        clearClimateCaches();
        
        // 4. Regenerate reports
        regenerateAllReports();
        
        ctx.sendSuccess(String.format(
            "Reload complete! Loaded: %d, Failed: %d",
            loaded, failed
        ));
        
    } catch (Exception e) {
        ctx.sendError("Reload failed: " + e.getMessage());
    }
}
```

**Requirements:**
- Only operators can execute
- Reload all configs
- Re-validate everything
- Update live dimensions (if possible)
- Clear all caches
- Helpful success/error messages

---

#### 2. Debug Commands

**File:** `command/BCDebugCommands.java`

**Commands:**
```java
/bc dimension list
> Lists all custom dimensions

/bc dimension info <id>
> Shows dimension details (biomes, mode, portal block, etc.)

/bc dimension report <id>
> Generates distribution report

/bc biome list <dimension>
> Lists all biomes in dimension with spawn probabilities

/bc biome info <biome_id>
> Shows biome metadata (mod, climate, tags)

/bc region list <dimension>
> Lists Terrablender regions in dimension

/bc climate test <x> <z>
> Tests climate at coordinates (climate grid mode only)

/bc layer info <dimension>
> Shows layer structure (layered mode only)

/bc debug <on|off>
> Toggle debug logging

/bc version
> Show BC version and loaded modules
```

---

#### 3. Documentation Generator

**File:** `util/DocumentationGenerator.java`

**Purpose:** Generate user-friendly documentation from configs

**Generated Docs:**
```
docs/
├─ GETTING_STARTED.md      # How to use BC:Dimensions
├─ CONFIG_GUIDE.md         # Config file reference
├─ EXAMPLES.md             # Example dimension configs
├─ TROUBLESHOOTING.md      # Common issues and solutions
└─ API.md                  # For mod developers
```

**Auto-generate from code:**
- Config field descriptions → CONFIG_GUIDE.md
- Example configs → EXAMPLES.md
- API javadoc → API.md

---

### Final Testing Checklist

**Region Mode:**
- [ ] Multiple biome mods blend correctly
- [ ] Region weight overrides work
- [ ] Biome weight overrides work
- [ ] Region reports generate
- [ ] Biome weight reports generate
- [ ] Excluded regions don't generate
- [ ] Excluded biomes don't appear

**Climate Grid Mode:**
- [ ] Temperature gradient works (north=cold, south=hot)
- [ ] Moisture gradient works (west=wet, east=dry)
- [ ] Boundary reversal works
- [ ] Biomes match climate correctly
- [ ] Blob shapes are organic (not grid-like)
- [ ] Minimum biome size enforced
- [ ] No performance issues

**Layered Mode:**
- [ ] Biomes change at layer boundaries
- [ ] Cave percentage works per layer
- [ ] Hollow layers generate correctly
- [ ] Ceiling/floor thickness correct
- [ ] Per-layer ore generation works
- [ ] Single-biome layers work

**Portal System:**
- [ ] Portals activate with configured block
- [ ] Teleportation works
- [ ] Coordinate offset works (1:1, 8:1, -8:1)
- [ ] Portal linking modes work
- [ ] IPortalProvider override works

**World Borders:**
- [ ] Borders apply at configured radius
- [ ] Damage works
- [ ] Warnings work
- [ ] Persists across restarts

**Commands:**
- [ ] All commands work
- [ ] Permissions enforced
- [ ] Error messages helpful
- [ ] Reload command works

**Performance:**
- [ ] No lag with multiple dimensions
- [ ] Chunk generation fast enough
- [ ] Biome caching works
- [ ] Climate caching works

---

This completes the implementation guide. All phases are now documented in the same markdown format for AI reference.