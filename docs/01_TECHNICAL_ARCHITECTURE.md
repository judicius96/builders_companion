# Builders Companion - Technical Architecture

## Critical Technical Challenges & Solutions

### Challenge 1: The "is_overworld" Problem

**Problem:**
Minecraft/Forge biomes have hardcoded dimension tags:
- `is_overworld` - allows biome in overworld
- `is_nether` - allows biome in nether  
- `is_end` - allows biome in end

When you create a custom dimension, biomes with `is_overworld` won't work there because the dimension isn't tagged as overworld.

**Solution:**
Clone biomes and override dimension tags when placing in custom dimensions:
```java
Holder<Biome> cloneBiomeForDimension(Biome original, ResourceKey<Level> targetDim) {
    // Remove vanilla dimension tags
    Set<TagKey<Biome>> tags = new HashSet<>(original.getTags());
    tags.removeIf(tag -> 
        tag.location().equals(BiomeTags.IS_OVERWORLD.location()) ||
        tag.location().equals(BiomeTags.IS_NETHER.location()) ||
        tag.location().equals(BiomeTags.IS_END.location())
    );
    
    // Add custom dimension tag
    TagKey<Biome> customTag = TagKey.create(
        Registry.BIOME_REGISTRY,
        new ResourceLocation("builderscompanion", "is_" + targetDim.location().getPath())
    );
    tags.add(customTag);
    
    // Build cloned biome with new tags, preserving all other properties
    return Holder.direct(original.withTags(tags));
}
```

This allows `minecraft:desert` to work in `sand_world` dimension without creating a custom biome.

---

### Challenge 2: Region Mode vs Climate Grid Mode

**Key Architectural Decision:** These are **mutually exclusive** - a dimension uses one or the other, never both.

#### Region Mode (Terrablender Integration)

**How It Works:**
1. Intercept Terrablender region registrations from all mods
2. Store region data (weights, biomes, mod source)
3. Allow user to override region weights
4. Let Terrablender's competition system handle chunk assignment
5. Generate transparency reports showing all regions

**User Controls:**
- Which regions are active (`exclude_regions`)
- Region weights (`region_overrides`)
- Individual biome weights within regions (`weight_overrides`)

**User Does NOT Control:**
- WHERE biomes appear (Minecraft's noise determines this)
- Climate distribution (automatic)

**Use When:**
- Blending multiple biome mods traditionally
- Don't need geographic climate control
- Want Terrablender's sophisticated region system

#### Climate Grid Mode (Our Innovation)

**How It Works:**
1. Calculate climate (temp + moisture) for every chunk based on coordinates
2. Apply gradients: north→south = cold→hot, west→east = wet→dry
3. Select biome that best matches the climate at each position
4. Enforce minimum biome size using organic blob algorithm
5. Respect world boundaries with optional reversal

**User Controls:**
- Climate gradients (how steep temperature/moisture changes)
- World boundaries (size and reversal behavior)
- Which biomes are available for matching
- Minimum biome blob size and irregularity

**User Does NOT Control:**
- Terrablender regions (we bypass them entirely)

**Use When:**
- Want realistic climate zones
- Need predictable exploration (go north for snow)
- Want Vintage Story-style bounded worlds

**Critical Implementation:**
```java
public Climate getClimateAt(int chunkX, int chunkZ) {
    // Distance from spawn
    int distX = chunkX - config.spawnX;
    int distZ = chunkZ - config.spawnZ;
    
    // Apply boundary and reversal
    distX = applyBoundary(distX, config.boundaryChunks, config.reversal);
    distZ = applyBoundary(distZ, config.boundaryChunks, config.reversal);
    
    // Calculate gradients
    double temperature = calculateGradient(distZ, tempNorth, tempSouth, tempSpawn);
    double moisture = calculateGradient(distX, moistureWest, moistureEast, moistureSpawn);
    
    // Add noise for variation (prevents linear gradients)
    temperature += sampleNoise(chunkX, chunkZ, seed) * 0.1;
    moisture += sampleNoise(chunkX, chunkZ, seed + 1000) * 0.1;
    
    return new Climate(
        Math.clamp(temperature, -1.0, 1.0),
        Math.clamp(moisture, -1.0, 1.0)
    );
}
```

---

### Challenge 3: Organic Biome Blob Shapes

**Problem:**
Without minimum biome size enforcement, you get tiny 1-chunk biome patches that look unnatural.

**Wrong Solution (Grid-Based):**
```java
// DON'T DO THIS - creates visible grid pattern
int gridX = (chunkX / 8) * 8;  // Round to 8-chunk grid
return getBiomeForGrid(gridX, gridZ);

Result:
F F F F F F F F P P P P P P P P
F F F F F F F F P P P P P P P P
F F F F F F F F P P P P P P P P
F F F F F F F F P P P P P P P P
                ↑ visible grid seam
```

**Correct Solution (Organic Blobs):**

Use noise-based boundaries to create irregular shapes:
```java
public Biome enforceBiomeBlob(int chunkX, int chunkZ, Biome selectedBiome) {
    // 1. Find nearby existing biome blobs (spiral search, not grid)
    Biome nearbyBlob = findNearbyBiomeBlob(chunkX, chunkZ);
    
    if (nearbyBlob != null) {
        // 2. Use noise to decide if this chunk joins the blob
        double noise = boundaryNoise.sample(chunkX, chunkZ);
        double threshold = calculateJoinThreshold(chunkX, chunkZ, nearbyBlob);
        
        if (noise > threshold) {
            return nearbyBlob;  // Join existing blob (creates irregular edge)
        }
    }
    
    // 3. Check if selected biome would form valid blob (min size)
    if (!wouldFormValidBlob(chunkX, chunkZ, selectedBiome)) {
        // Extend nearby dominant biome instead
        return findDominantNearbyBiome(chunkX, chunkZ);
    }
    
    return selectedBiome;  // Start new blob
}

private double calculateJoinThreshold(int x, int z, Biome blobBiome) {
    // Find blob center
    ChunkPos center = findBlobCenter(blobBiome);
    double distance = Math.sqrt((x - center.x)² + (z - center.z)²);
    
    // Threshold increases with distance
    // Near center: easy to join (threshold 0.2)
    // At edge: moderate (threshold 0.5)
    // Far away: hard to join (threshold 0.8)
    return 0.2 + (normalizedDistance * 0.6);
}

Result:
    F F F F
  F F F F F F
F F F F F F F F
  F F F F F F
    F F F F F
      F F
        ↑ irregular, natural boundary
```

**Key Principles:**
- Minimum size = total chunks (64), NOT grid dimensions (8x8)
- Search pattern = spiral/circular, NOT grid
- Boundaries = noise-based, NOT straight lines
- Result = coastline-like edges, NOT squares

**Configuration:**
```yaml
min_biome_size_chunks: 64    # Total chunks in blob
blob_irregularity: 0.7       # 0.0=circle, 1.0=fractal
blob_noise_scale: 0.08       # Frequency of irregularities
blob_coherence: 0.6          # How well blob stays together
```

---

### Challenge 4: Terrablender Region Interception

**Problem:**
Multiple biome mods register regions with Terrablender. We need to:
1. Capture what regions exist
2. Store their data (biomes, weights)
3. Allow user to control them
4. Generate transparency reports

**Solution:**
```java
@SubscribeEvent
public static void onRegionRegister(TerrabenderRegisterEvent event) {
    Region region = event.getRegion();
    ResourceKey<Level> dimension = region.getDimension();
    
    // Capture region data
    RegionMetadata metadata = new RegionMetadata(
        region.getName(),
        region.getWeight(),
        region.getBiomes(),
        region.getModId(),
        dimension
    );
    
    // Store for later processing
    dimensionRegions.computeIfAbsent(dimension, k -> new ArrayList<>())
        .add(metadata);
    
    if (BCConfig.isDebugMode()) {
        LOGGER.debug("Captured region: {} (mod: {}, weight: {}, biomes: {})",
            region.getName(), region.getModId(), region.getWeight(), 
            region.getBiomes().size());
    }
}

@SubscribeEvent
public static void onServerStarting(ServerStartingEvent event) {
    // After all mods loaded, generate reports
    for (var entry : dimensionRegions.entrySet()) {
        generateRegionReport(entry.getKey(), entry.getValue());
    }
}
```

**Generated Report:**
```yaml
# config/builderscompanion/bc_dimensions/overworld_regions.yml

dimension: "minecraft:overworld"
total_regions: 7
total_weight: 135

biomesoplenty:
  "biomesoplenty:bop_overworld_1":
    region_number: 1
    weight: 15          # User boosted from default 10
    default_weight: 10
    override_applied: true
    biomes:
      total: 42
      # ... biome list

competition_analysis:
  mod_influence:
    "byg": "33.3%"          # BWG dominates
    "terralith": "18.5%"
    "biomesoplenty": "18.5%"
  
  recommendations:
    - "⚠ BWG has 33.3% total influence - most dominant"
    - "💡 To reduce, lower byg:overworld_1 weight to 10"
```

---

### Challenge 5: Layered Biome System (Mining Dimension)

**Problem:**
Minecraft's biome placement is horizontal (XZ plane). How do you create vertical layers (Y-axis control)?

**Solution:**
Custom BiomeSource that queries Y coordinate:
```java
public class LayeredBiomeProvider extends BiomeSource {
    private List<BiomeLayer> layers;
    
    @Override
    public Biome getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        int worldY = y * 4;  // Convert quart-pos to world Y
        
        // Find which layer this Y coordinate is in
        BiomeLayer layer = findLayer(worldY);
        
        if (layer.biomeMode.equals("single")) {
            // Entire layer is one biome (The Hollow)
            return layer.singleBiome;
        }
        
        // Check if this should be cave or solid
        if (shouldBeCave(x, z, layer)) {
            return layer.caveBiome;
        }
        
        // Select from layer's biome pool
        return selectBiomeFromLayer(x, z, layer, sampler);
    }
    
    private BiomeLayer findLayer(int worldY) {
        for (BiomeLayer layer : layers) {
            if (worldY >= layer.yMin && worldY <= layer.yMax) {
                return layer;
            }
        }
        return defaultLayer;  // Fallback
    }
}
```

**The Hollow (Hollow World Layer):**

Creating an entirely hollow layer requires custom chunk generation:
```java
public class HollowLayerChunkGenerator {
    void fillChunk(ChunkAccess chunk, BiomeLayer hollowLayer) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Ceiling (Y -29 to -32)
                for (int y = -32; y <= -29; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), 
                        Blocks.STONE.defaultBlockState(), false);
                }
                
                // Hollow space (Y -63 to -30) - AIR, do nothing
                
                // Floor (Y -64)
                chunk.setBlockState(new BlockPos(x, -64, z), 
                    Blocks.BEDROCK.defaultBlockState(), false);
                
                // Set biome for entire column
                for (int y = -64; y <= -29; y++) {
                    chunk.setBiome(x, y, z, hollowLayer.singleBiome);
                }
            }
        }
    }
}
```

---

## BC-Core API Design

### Primary APIs
```java
// Dimension Registry
public class BCDimensionsAPI {
    public static void registerDimension(ResourceKey<Level> dim, DimensionConfig config);
    public static DimensionConfig getDimensionConfig(ResourceKey<Level> dim);
    public static List<ResourceKey<Level>> getAllCustomDimensions();
}

// Portal Provider Interface (for BC:Portals to override)
public interface IPortalProvider {
    boolean shouldHandlePortal(BlockPos pos, Level level);
    void teleportEntity(Entity entity, ResourceKey<Level> targetDim);
}

// Biome Modifier Interface (for BC:Fluids)
public interface IBiomeModifier {
    boolean shouldModifyBiome(Biome biome, ResourceKey<Level> dimension);
    Biome getModifiedBiome(Biome original);
}

// Geode Provider Interface (for BC:Geodes)
public interface IGeodeProvider {
    boolean shouldGenerateGeode(ResourceKey<Level> dimension, BlockPos pos);
    GeodeConfiguration getGeodeConfig(Biome biome);
}
```

### Event System
```java
// Fired when a dimension is registered
public class DimensionRegisteredEvent extends Event {
    public final ResourceKey<Level> dimension;
    public final DimensionConfig config;
}

// Fired before biome placement (allow modifications)
public class BiomePlacementEvent extends Event {
    public final ChunkPos pos;
    public Biome biome;  // Mutable - listeners can change
    public final ResourceKey<Level> dimension;
}

// Fired when portal is activated
public class PortalActivatedEvent extends Event {
    public final BlockPos portalPos;
    public final Entity entity;
    public ResourceKey<Level> targetDimension;  // Mutable
}
```

---

## Configuration System Architecture

### Multi-File Structure
```
config/builderscompanion/
├─ dimensions.yml              # Main config - lists enabled dimensions
├─ global.yml                  # Global settings
├─ dimensions/                 # Per-dimension configs
│  ├─ avalon.yml
│  ├─ terra.yml
│  ├─ sand_world.yml
│  └─ mining_world.yml
├─ presets/
│  ├─ noise_presets.yml       # Built-in noise configs
│  └─ carver_presets.yml
└─ bc_dimensions/             # AUTO-GENERATED (transparency)
   ├─ overworld_regions.yml   # Region report
   ├─ avalon_regions.yml
   └─ biome_weights_avalon.yml  # Biome weight report
```

### Config Validation

All configs must be validated on load with helpful error messages:
```java
public class ConfigValidator {
    public ValidationResult validate(DimensionConfig config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check required fields
        if (config.displayName == null || config.displayName.isEmpty()) {
            errors.add("display_name is required");
        }
        
        // Validate portal block exists
        if (!Registry.BLOCK.containsKey(config.portal.frameBlock)) {
            errors.add("portal.frame_block '" + config.portal.frameBlock + 
                "' does not exist. Check spelling or required mods.");
        }
        
        // Validate biome mode
        if (!Arrays.asList("region", "climate_grid", "layered")
                .contains(config.biomes.mode)) {
            errors.add("biomes.mode must be 'region', 'climate_grid', or 'layered'");
        }
        
        // Check for conflicting settings
        if (config.biomes.mode.equals("climate_grid") && 
            !config.regionOverrides.isEmpty()) {
            warnings.add("region_overrides are ignored in climate_grid mode");
        }
        
        // Validate weight values
        for (var entry : config.weightOverrides.entrySet()) {
            if (entry.getValue() < 0) {
                errors.add("Biome weight for '" + entry.getKey() + 
                    "' cannot be negative");
            }
        }
        
        return new ValidationResult(errors, warnings);
    }
}
```

**Error Message Example:**
```
[ERROR] Failed to load dimension config: dimensions/avalon.yml

Errors (must fix):
  - Line 15: portal.frame_block 'minecraft:polished_diorit' does not exist
    Did you mean 'minecraft:polished_diorite'?
  - Line 42: biomes.mode 'climate' is invalid
    Valid options: 'region', 'climate_grid', 'layered'

Warnings (optional):
  - Line 67: region_overrides are set but biomes.mode='climate_grid'
    Region overrides are only used in 'region' mode and will be ignored.

Dimension 'avalon' was NOT loaded. Fix errors and run /bcreload
```

---

## Performance Considerations

### Biome Caching
```java
// Cache biome calculations (expensive operations)
private final LoadingCache<ChunkPos, Biome> biomeCache = 
    CacheBuilder.newBuilder()
        .maximumSize(10000)  // 10k chunks
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(new CacheLoader<ChunkPos, Biome>() {
            public Biome load(ChunkPos pos) {
                return calculateBiomeAt(pos.x, pos.z);
            }
        });
```

### Climate Grid Caching
```java
// Climate values are deterministic - cache aggressively
private final Map<ChunkPos, Climate> climateCache = new ConcurrentHashMap<>();

public Climate getClimateAt(int chunkX, int chunkZ) {
    ChunkPos pos = new ChunkPos(chunkX, chunkZ);
    return climateCache.computeIfAbsent(pos, p -> {
        return calculateClimate(p.x, p.z);
    });
}
```

### Blob Shape Optimization
```java
// Limit blob search radius to prevent expensive flood-fills
private static final int MAX_BLOB_SEARCH_RADIUS = 16;

// Early-exit when blob size confirmed
if (potentialSize >= minBiomeSizeChunks) {
    return true;  // Don't keep searching
}
```

---

## Logging Strategy

### Log Levels
```java
// ERROR - Critical failures (dimension won't load)
LOGGER.error("Failed to register dimension {}: {}", dimId, exception);

// WARN - Recoverable issues (fallback behavior)
LOGGER.warn("Mod '{}' not found. Biomes from this mod will be skipped.", modId);

// INFO - Important milestones (always logged)
LOGGER.info("Registered dimension '{}' with {} biomes", dimName, biomeCount);

// DEBUG - Detailed operations (only if config.debug_mode = true)
if (BCConfig.isDebugMode()) {
    LOGGER.debug("Biome {} placed at ({}, {})", biome, x, z);
}

// TRACE - Ultra-verbose (for troubleshooting)
if (LOGGER.isTraceEnabled()) {
    LOGGER.trace("Climate at ({}, {}): temp={}, moisture={}", 
        x, z, temp, moisture);
}
```

### Structured Logging
```java
// Use MDC for context in logs
try (MDC.MDCCloseable ctx = MDC.putCloseable("dimension", dimensionId)) {
    LOGGER.info("Loading dimension configuration");
    // All logs in this block will include dimension ID
}

Result in logs:
[INFO] [dimension=avalon] Loading dimension configuration
[DEBUG] [dimension=avalon] Found 254 available biomes
[INFO] [dimension=avalon] Successfully registered dimension
```

---

## Testing Strategy

### Unit Tests (JUnit)
```java
@Test
public void testClimateGradient() {
    ClimateConfig config = new ClimateConfig();
    config.tempNorth = -1.0;
    config.tempSouth = 1.0;
    config.boundaryChunks = 2500;
    
    ClimateGridGenerator generator = new ClimateGridGenerator(config, 12345);
    
    // At spawn (0, 0): should be moderate
    Climate spawn = generator.getClimateAt(0, 0);
    assertEquals(0.0, spawn.temperature, 0.15);  // Within noise tolerance
    
    // At north boundary (0, -2500): should be cold
    Climate north = generator.getClimateAt(0, -2500);
    assertEquals(-1.0, north.temperature, 0.15);
    
    // At south boundary (0, 2500): should be hot
    Climate south = generator.getClimateAt(0, 2500);
    assertEquals(1.0, south.temperature, 0.15);
}

@Test
public void testBoundaryReversal() {
    ClimateConfig config = new ClimateConfig();
    config.tempNorth = -1.0;
    config.tempSouth = 1.0;
    config.boundaryChunks = 2500;
    config.reversal = true;
    
    ClimateGridGenerator generator = new ClimateGridGenerator(config, 12345);
    
    // Beyond boundary (0, 2501): should start reversing
    Climate beyond = generator.getClimateAt(0, 2501);
    assertTrue(beyond.temperature < 1.0);  // No longer hottest
    
    // Full reversal (0, 5000): should be back to moderate
    Climate fullReverse = generator.getClimateAt(0, 5000);
    assertEquals(0.0, fullReverse.temperature, 0.15);
}
```

### Integration Tests (In-Game)
```java
// Test dimension actually loads
@Test
public void testDimensionRegistration() {
    // Load test config
    DimensionConfig config = loadTestConfig("test_dimension.yml");
    
    // Register dimension
    BCDimensionsAPI.registerDimension(TEST_DIM, config);
    
    // Verify it exists
    assertTrue(server.getAllLevels().stream()
        .anyMatch(level -> level.dimension().equals(TEST_DIM)));
}

// Test portal teleportation
@Test
public void testPortalTeleport() {
    // Create portal
    buildPortal(overworld, new BlockPos(0, 64, 0));
    
    // Teleport entity
    Entity testEntity = new Pig(overworld);
    testEntity.setPos(0, 64, 0);
    
    activatePortal(new BlockPos(0, 64, 0));
    
    // Verify entity in target dimension
    assertEquals(TEST_DIM, testEntity.level.dimension());
}
```

---

## Security Considerations

### Config Path Traversal Prevention
```java
public Path resolveConfigPath(String filename) {
    // Prevent ../../../etc/passwd attacks
    Path configDir = Paths.get("config/builderscompanion");
    Path requested = configDir.resolve(filename).normalize();
    
    if (!requested.startsWith(configDir)) {
        throw new SecurityException(
            "Config path traversal attempt blocked: " + filename);
    }
    
    return requested;
}
```

### Command Permissions
```java
// Only ops can reload configs (could grief server)
@Override
public boolean checkPermission(ServerCommandSource source) {
    return source.hasPermissionLevel(2);  // OP level
}
```

---

## Future Extensibility

### Plugin System (Future)
```java
// Allow other mods to register dimension generators
public interface IDimensionGenerator {
    String getGeneratorType();  // "custom_generator"
    void generateChunk(ChunkAccess chunk, DimensionConfig config);
}

// BC-Dimensions calls registered generators
public class DimensionGeneratorRegistry {
    private static Map<String, IDimensionGenerator> generators = new HashMap<>();
    
    public static void register(String type, IDimensionGenerator generator) {
        generators.put(type, generator);
    }
}
```

### Custom Biome Providers (Future)
```java
// Allow other mods to provide biomes programmatically
public interface IBiomeProvider {
    List<Biome> getBiomes(ResourceKey<Level> dimension);
    boolean shouldProvideBiomes(DimensionConfig config);
}
```

---

This architecture is designed to be:
- ✅ Extensible (interfaces for future features)
- ✅ Performant (caching, early exits)
- ✅ Maintainable (clear separation of concerns)
- ✅ Debuggable (comprehensive logging)
- ✅ Testable (dependency injection, clear APIs)
- ✅ Secure (input validation, path sanitization)