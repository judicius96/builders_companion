package com.builderscompanion.dyedwaters.provider;

import com.builderscompanion.core.api.TintedLiquidProvider;
import com.builderscompanion.core.registry.tintedliquids.TintedIdRanges;
import com.builderscompanion.dyedwaters.registry.DyedColorRegistry;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

public class DyedWaterProvider implements TintedLiquidProvider {

    @Override
    public String getProviderId() {
        return "dyed_waters";
    }

    @Override
    public boolean handlesTypeId(int typeId) {
        return typeId >= TintedIdRanges.DYE_START && typeId < TintedIdRanges.DYE_END_EXCLUSIVE;
    }

    @Override
    public Component getDisplayName(int typeId) {
        DyedColorRegistry.ColorEntry entry = DyedColorRegistry.getEntryByTypeId(typeId);
        if (entry == null) return null;

        String dyeName = DyedColorRegistry.getPrimaryDyeForColor(entry.rgb);
        if (dyeName == null || dyeName.isEmpty()) return null;

        // Base name only; Core bucket item appends (Infused)/(Radiant)
        return Component.literal(dyeName + " Water Bucket");
    }

    @Override
    public List<Component> getTooltipLines(int typeId) {
        return Collections.emptyList();
    }
}
