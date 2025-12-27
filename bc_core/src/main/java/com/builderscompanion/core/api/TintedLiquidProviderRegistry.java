    package com.builderscompanion.core.api;

    import com.builderscompanion.core.util.BCLogger;
    import net.minecraft.network.chat.Component;
    import java.util.*;

    /**
     * Registry for TintedLiquidProvider implementations
     */
    public class TintedLiquidProviderRegistry {
        private static final List<TintedLiquidProvider> providers = new ArrayList<>();

        /**
         * Register a provider
         */
        public static void register(TintedLiquidProvider provider) {
            providers.add(provider);
            BCLogger.info("Registered TintedLiquidProvider: {}", provider.getProviderId());
        }

        /**
         * Get display name for a typeId from the appropriate provider
         */
        public static Component getDisplayName(int typeId) {
            for (TintedLiquidProvider provider : providers) {
                if (provider.handlesTypeId(typeId)) {
                    Component name = provider.getDisplayName(typeId);
                    if (name != null) {
                        return name;
                    }
                }
            }
            return null;
        }

        public static int getRgbColor(int typeId) {
            for (TintedLiquidProvider provider : providers) {
                if (provider.handlesTypeId(typeId)) {
                    return provider.getRgbColor(typeId);
                }
            }
            return 0xFFFFFFFF; // default white
        }

        /**
         * Get tooltip lines for a typeId from the appropriate provider
         */
        public static List<Component> getTooltipLines(int typeId) {
            for (TintedLiquidProvider provider : providers) {
                if (provider.handlesTypeId(typeId)) {
                    return provider.getTooltipLines(typeId);
                }
            }
            return List.of();
        }
    }