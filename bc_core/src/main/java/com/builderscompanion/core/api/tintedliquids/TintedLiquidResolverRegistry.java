package com.builderscompanion.core.api.tintedliquids;

import java.util.List;

public interface TintedLiquidResolverRegistry {
    void register(BiomeToTintTypeResolver resolver);

    List<BiomeToTintTypeResolver> resolvers();
}