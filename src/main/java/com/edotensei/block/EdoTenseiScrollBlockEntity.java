package com.edotensei.block;

import com.edotensei.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Guarda o sangue (id do Hokage) que foi colocado no pergaminho. Vazio = sem sangue. */
public class EdoTenseiScrollBlockEntity extends BlockEntity {
    private String hokage = "";

    public EdoTenseiScrollBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.SCROLL_BE.get(), pos, state);
    }

    public String getHokage() {
        return hokage;
    }

    public void setHokage(String hokage) {
        this.hokage = hokage;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Hokage", hokage);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.hokage = tag.getString("Hokage");
    }
}
