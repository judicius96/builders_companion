package com.builderscompanion.core.tintedliquids;

import com.builderscompanion.core.api.tintedliquids.BiomeToTintTypeResolver;
import com.builderscompanion.core.api.tintedliquids.TintedLiquidResolverRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TintedLiquidResolverRegistryImpl implements TintedLiquidResolverRegistry {

    private final List<BiomeToTintTypeResolver> resolvers = new ArrayList<>();

    @Override
    public void register(BiomeToTintTypeResolver resolver) {
        resolvers.add(resolver);
    }

    @Override
    public List<BiomeToTintTypeResolver> resolvers() {
        return Collections.unmodifiableList(resolvers);
    }
}
