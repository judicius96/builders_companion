package com.builderscompanion.core.blockentity.tintedliquids;

import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class TintedCauldronBlockEntity extends BlockEntity {

    private static final String TAG_TYPE_ID = "TypeId";

    // -1 = unset / empty (your code already uses this convention)
    private int typeId = -1;

    public TintedCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(TintedLiquidsBlockEntity.TINTED_CAULDRON.get(), pos, state);
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        if (this.typeId == typeId) return;
        this.typeId = typeId;
        setChanged();

        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            // Force client to receive updated NBT so BlockColor can tint correctly
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_TYPE_ID, typeId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_TYPE_ID)) {
            this.typeId = tag.getInt(TAG_TYPE_ID);
        } else {
            this.typeId = -1;
        }
    }

    // ---- Client sync ----

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt(TAG_TYPE_ID, typeId);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains(TAG_TYPE_ID)) {
            this.typeId = tag.getInt(TAG_TYPE_ID);
        } else {
            this.typeId = -1;
        }
    }
}
