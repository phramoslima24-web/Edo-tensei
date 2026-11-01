package com.edotensei.block;

import com.edotensei.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Guarda o sangue (id do tipo de mob) que foi colocado no pergaminho. Vazio = sem sangue. */
public class EdoTenseiScrollBlockEntity extends BlockEntity {
    private String entityId = "";

    public EdoTenseiScrollBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.SCROLL_BE.get(), pos, state);
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Entity", entityId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.entityId = tag.getString("Entity");
    }
}
