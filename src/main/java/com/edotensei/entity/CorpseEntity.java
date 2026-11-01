package com.edotensei.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Corpo de um mob do Narutoto. O cliente desenha o proprio mob deitado no chao
 * (veja CorpseRenderer). Clicar com a seringa coleta o sangue.
 */
public class CorpseEntity extends Entity {
    private static final EntityDataAccessor<String> TYPE_ID = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);
    public static final int LIFETIME = 20 * 60 * 10;

    private boolean harvested;
    private int age;

    public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
        super(type, level);
    }

    public void setup(String entityTypeId, float yRot) {
        this.entityData.set(TYPE_ID, entityTypeId);
        this.setYRot(yRot);
        this.yRotO = yRot;
    }

    public String getEntityTypeId() {
        return this.entityData.get(TYPE_ID);
    }

    public boolean isHarvested() {
        return harvested;
    }

    public void setHarvested(boolean harvested) {
        this.harvested = harvested;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TYPE_ID, "");
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        if (!level().isClientSide && ++age >= LIFETIME) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(TYPE_ID, tag.getString("EntityType"));
        this.harvested = tag.getBoolean("Harvested");
        this.age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("EntityType", getEntityTypeId());
        tag.putBoolean("Harvested", harvested);
        tag.putInt("Age", age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
