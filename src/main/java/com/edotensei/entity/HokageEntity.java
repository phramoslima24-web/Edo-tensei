package com.edotensei.entity;

import java.util.EnumSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Hokage (1o, 2o ou 3o). Pode nascer vivo no mundo (neutro) ou ser invocado
 * pelo Edo Tensei (obedece e protege o invocador). Ao morrer deixa o corpo no chao.
 */
public class HokageEntity extends PathfinderMob {

    public enum Kind {
        HASHIRAMA("hashirama", 220.0F, 14.0F, 0x5B3A29, 0x2E6B3A),
        TOBIRAMA("tobirama", 160.0F, 12.0F, 0xD8D8E0, 0xA02020),
        HIRUZEN("hiruzen", 140.0F, 10.0F, 0xF2F2F2, 0xB02A2A);

        public final String id;
        public final float health;
        public final float damage;
        public final int eggPrimary;
        public final int eggSecondary;

        Kind(String id, float health, float damage, int eggPrimary, int eggSecondary) {
            this.id = id;
            this.health = health;
            this.damage = damage;
            this.eggPrimary = eggPrimary;
            this.eggSecondary = eggSecondary;
        }

        @Nullable
        public static Kind byId(String id) {
            for (Kind k : values()) {
                if (k.id.equals(id)) return k;
            }
            return null;
        }
    }

    /** Tempo que o corpo fica no chao (10 minutos). */
    public static final int CORPSE_LIFETIME = 20 * 60 * 10;

    private final Kind kind;
    @Nullable
    private UUID owner;
    private boolean harvested;
    private int corpseAge;

    public HokageEntity(EntityType<? extends HokageEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.health)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, kind.damage)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.ARMOR, 6.0D);
    }

    public static boolean checkSpawn(EntityType<HokageEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Mob.checkMobSpawnRules(type, level, reason, pos, random) && level.getRawBrightness(pos, 0) > 8;
    }

    public Kind getKind() {
        return kind;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return owner;
    }

    public void setOwner(@Nullable UUID uuid) {
        this.owner = uuid;
        if (uuid != null) this.setPersistenceRequired();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (owner == null || level().isClientSide) return null;
        return level().getPlayerByUUID(owner);
    }

    public boolean isHarvested() {
        return harvested;
    }

    public void setHarvested(boolean harvested) {
        this.harvested = harvested;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerProtectGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (owner != null && source.getEntity() != null && owner.equals(source.getEntity().getUUID())) {
            return false; // nao machuca quem o invocou
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && owner == null && source.getEntity() instanceof Player) {
            ExperienceOrb.award((ServerLevel) level(), this.position(), this.xpReward);
        }
    }

    /** Em vez de sumir apos 1 segundo, o corpo fica no chao por um bom tempo. */
    @Override
    protected void tickDeath() {
        if (this.deathTime < 20) {
            this.deathTime++;
        }
        if (!level().isClientSide && ++corpseAge >= CORPSE_LIFETIME) {
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && this.isAlive()) {
            if (!level().isClientSide) {
                player.displayClientMessage(
                        Component.translatable("entity.edotensei." + kind.id).append(": ")
                                .append(Component.translatable("dialog.edotensei." + kind.id)),
                        false);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("EdoOwner", owner);
        tag.putBoolean("Harvested", harvested);
        tag.putInt("CorpseAge", corpseAge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.owner = tag.hasUUID("EdoOwner") ? tag.getUUID("EdoOwner") : null;
        this.harvested = tag.getBoolean("Harvested");
        this.corpseAge = tag.getInt("CorpseAge");
    }

    // ------------------------------------------------------------------ goals

    /** Segue o invocador de perto (e teleporta se ficar muito longe). */
    static class FollowOwnerGoal extends Goal {
        private final HokageEntity hokage;
        private LivingEntity owner;
        private int recalc;

        FollowOwnerGoal(HokageEntity hokage) {
            this.hokage = hokage;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = hokage.getOwner();
            if (o == null || !o.isAlive() || hokage.getTarget() != null) return false;
            if (o.level() != hokage.level()) return false;
            if (hokage.distanceToSqr(o) < 100.0D) return false;
            this.owner = o;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && hokage.getTarget() == null && hokage.distanceToSqr(owner) > 9.0D;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void stop() {
            owner = null;
            hokage.getNavigation().stop();
        }

        @Override
        public void tick() {
            hokage.getLookControl().setLookAt(owner, 10.0F, (float) hokage.getMaxHeadXRot());
            if (--recalc <= 0) {
                recalc = 10;
                if (hokage.distanceToSqr(owner) >= 900.0D) {
                    for (int i = 0; i < 10; i++) {
                        double x = owner.getX() + hokage.getRandom().nextInt(7) - 3;
                        double z = owner.getZ() + hokage.getRandom().nextInt(7) - 3;
                        if (hokage.randomTeleport(x, owner.getY(), z, true)) break;
                    }
                } else {
                    hokage.getNavigation().moveTo(owner, 1.25D);
                }
            }
        }
    }

    /** Ataca quem machucou o invocador, ou quem o invocador atacou. */
    static class OwnerProtectGoal extends TargetGoal {
        private final HokageEntity hokage;
        private LivingEntity wanted;

        OwnerProtectGoal(HokageEntity hokage) {
            super(hokage, false);
            this.hokage = hokage;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = hokage.getOwner();
            if (owner == null) return false;

            LivingEntity candidate = owner.getLastHurtByMob();
            if (candidate == null || owner.tickCount - owner.getLastHurtByMobTimestamp() > 200) {
                candidate = owner.getLastHurtMob();
                if (candidate == null || owner.tickCount - owner.getLastHurtMobTimestamp() > 200) return false;
            }
            if (candidate == owner || candidate == hokage) return false;
            if (candidate instanceof HokageEntity other && owner.getUUID().equals(other.getOwnerUUID())) return false;
            if (!candidate.isAlive()) return false;
            if (!this.canAttack(candidate, TargetingConditions.DEFAULT)) return false;
            this.wanted = candidate;
            return true;
        }

        @Override
        public void start() {
            this.mob.setTarget(wanted);
            super.start();
        }
    }
}
