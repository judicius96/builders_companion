package com.builderscompanion.core.registry.tintedliquids;

public final class TintedIdRanges {
    private TintedIdRanges() {}

    // Global capacity for array-backed lookups
    public static final int MAX_TYPE_ID = 320;

    // Biome tint budget (96 + 12 headroom)
    public static final int BIOME_TINT_CAP = 108;

    // Dyes start immediately after biome tint budget
    public static final int DYE_START = BIOME_TINT_CAP;

    // 4 variants: dyed, infused, radiant, infused+radiant
    public static final int DYE_VARIANTS = 4;

    // Max dyes you want to allow within this cap
    public static final int MAX_DYES = (MAX_TYPE_ID - DYE_START) / DYE_VARIANTS;

    public static final int DYE_END_EXCLUSIVE = DYE_START + (MAX_DYES * DYE_VARIANTS);
}
