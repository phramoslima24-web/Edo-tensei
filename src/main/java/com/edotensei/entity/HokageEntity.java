package com.edotensei.entity;

import com.edotensei.EdoTargets;
import java.util.EnumSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Hokage (1o, 2o ou 3o). Pode nascer vivo no mundo (neutro) ou ser invocado
 * pelo Edo Tensei (obedece e protege o invocador). Ao morrer deixa o corpo no chao.
 */
public class HokageEntity extends PathfinderMob {

    public enum Kind {
        //          id          vida    dano   veloc.  armadura ovo1      ovo2      hostil  drop (item de outro mod, opcional)
        HASHIRAMA("hashirama", 220.0F, 14.0F, 0.33D, 6.0D,  0x5B3A29, 0x2E6B3A, false, null),
        TOBIRAMA("tobirama",   160.0F, 12.0F, 0.33D, 6.0D,  0xD8D8E0, 0xA02020, false, null),
        HIRUZEN("hiruzen",     140.0F, 10.0F, 0.33D, 6.0D,  0xF2F2F2, 0xB02A2A, false, null),
        /** Madara: ataca qualquer jogador por conta propria. Larga o Mangekyou dele. */
        MADARA("madara",       400.0F, 24.0F, 0.38D, 12.0D, 0x1A1A1A, 0xA02020, true,  "narutoto_mangekyou:mangekyou_madara"),
        /** Shisui: so ataca se for atacado. Larga o Mangekyou dele. */
        SHISUI("shisui",       300.0F, 20.0F, 0.42D, 8.0D,  0x2B2B3A, 0xC02020, false, "narutoto_mangekyou:mangekyou_shisui");

        public final String id;
        public final float health;
        public final float damage;
        public final double speed;
        public final double armor;
        public final int eggPrimary;
        public final int eggSecondary;
        /** true = ataca jogadores sem ser provocado (so quando nao foi invocado por Edo Tensei). */
        public final boolean hostile;
        /** Id do item largado ao morrer (null = nada). So larga se nao foi invocado por Edo Tensei. */
        @Nullable
        public final String dropItem;

        Kind(String id, float health, float damage, double speed, double armor, int eggPrimary, int eggSecondary,
             boolean hostile, @Nullable String dropItem) {
            this.id = id;
            this.health = health;
            this.damage = damage;
            this.speed = speed;
            this.armor = armor;
            this.eggPrimary = eggPrimary;
            this.eggSecondary = eggSecondary;
            this.hostile = hostile;
            this.dropItem = dropItem;
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

    // tempos de recarga das habilidades (em ticks)
    private int katonCd;
    private int shockCd;
    private int susanooCd;
    private int shunshinCd;
    private int genjutsuCd;

    public HokageEntity(EntityType<? extends HokageEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.health)
                .add(Attributes.MOVEMENT_SPEED, kind.speed)
                .add(Attributes.ATTACK_DAMAGE, kind.damage)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind == Kind.MADARA ? 0.9D : 0.6D)
                .add(Attributes.ARMOR, kind.armor);
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
        // Madara: ataca jogadores sem ser provocado (so se nao foi invocado). "kind" ainda e null aqui,
        // entao a condicao e avaliada so depois, durante o jogo.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> this.kind != null && this.kind.hostile && this.owner == null));
    }

    // ------------------------------------------------------------- habilidades

    /** Pode ser atingido pelas habilidades? (nunca o invocador, nem os aliados dele, nem outros Hokages). */
    private boolean canHarm(LivingEntity e) {
        if (e == this || e instanceof HokageEntity || !e.isAlive()) return false;
        if (owner != null && EdoTargets.isRevived(e)) return false; // sem fogo amigo entre revividos
        if (owner != null) {
            if (owner.equals(e.getUUID())) return false;
            if (owner.equals(EdoTargets.ownerOf(e))) return false;
        }
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (katonCd > 0) katonCd--;
        if (shockCd > 0) shockCd--;
        if (susanooCd > 0) susanooCd--;
        if (shunshinCd > 0) shunshinCd--;
        if (genjutsuCd > 0) genjutsuCd--;

        hokagePassiveTick();

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !canHarm(target)) return;
        if (kind == Kind.MADARA) madaraTick(target);
        else if (kind == Kind.SHISUI) shisuiTick(target);
        else if (kind == Kind.HASHIRAMA) hashiramaTick(target);
        else if (kind == Kind.TOBIRAMA) tobiramaTick(target);
        else if (kind == Kind.HIRUZEN) hiruzenTick(target);
    }

    /** Habilidades passivas dos Hokages (nao dependem de ter alvo). */
    private void hokagePassiveTick() {
        if (kind != Kind.HASHIRAMA || !(level() instanceof ServerLevel server)) return;
        // Hashirama: cura lentamente o invocador que estiver por perto
        if (genjutsuCd <= 0 && owner != null) {
            LivingEntity o = getOwner();
            if (o != null && o.isAlive() && o.level() == level() && distanceToSqr(o) < 100.0D && o.getHealth() < o.getMaxHealth()) {
                o.heal(2.0F);
                genjutsuCd = 100;
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, o.getX(), o.getY() + 1.0D, o.getZ(), 8, 0.4D, 0.6D, 0.4D, 0.0D);
            }
        }
    }

    /** Hashirama - Mokuton: raizes prendem o alvo; regeneracao Senju com pouca vida. */
    private void hashiramaTick(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        double d2 = distanceToSqr(target);

        if (susanooCd <= 0 && getHealth() < getMaxHealth() * 0.5F) {
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
            susanooCd = 700;
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0D, getZ(), 40, 0.6D, 1.0D, 0.6D, 0.0D);
            playSound(SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.0F);
        }

        // Mokuton: Shibari - raizes prendem e ferem o alvo
        if (shockCd <= 0 && d2 < 196.0D && hasLineOfSight(target)) {
            shockCd = 140;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
            target.hurt(damageSources().mobAttack(this), 6.0F);
            server.sendParticles(ParticleTypes.COMPOSTER, target.getX(), target.getY() + 0.5D, target.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.0D);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.0D, target.getZ(), 15, 0.5D, 0.8D, 0.5D, 0.0D);
            playSound(SoundEvents.GRASS_BREAK, 1.2F, 0.6F);
        }

        // Mokuton: Shichuuka no Jutsu - area ao redor dele empurra e fere quem chega perto
        if (katonCd <= 0 && d2 < 16.0D) {
            katonCd = 120;
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0D), this::canHarm)) {
                Vec3 push = e.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
                e.hurt(damageSources().mobAttack(this), 7.0F);
                e.setDeltaMovement(e.getDeltaMovement().add(push.x * 1.0D, 0.4D, push.z * 1.0D));
                e.hurtMarked = true;
            }
            server.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + 1.0D, getZ(), 50, 2.0D, 0.8D, 2.0D, 0.0D);
            playSound(SoundEvents.WOOD_BREAK, 1.2F, 0.7F);
        }
    }

    /** Tobirama - Suiton e Hiraishin. */
    private void tobiramaTick(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        double d2 = distanceToSqr(target);

        // Hiraishin no Jutsu: aparece atras do alvo ja atacando
        if (shunshinCd <= 0 && d2 > 25.0D && d2 < 900.0D) {
            Vec3 back = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize().scale(-1.6D);
            double nx = target.getX() + back.x;
            double ny = target.getY();
            double nz = target.getZ() + back.z;
            AABB box = getBoundingBox().move(nx - getX(), ny - getY(), nz - getZ());
            if (level().noCollision(this, box)) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.0D, getZ(), 15, 0.3D, 0.6D, 0.3D, 0.05D);
                moveTo(nx, ny, nz, getYRot(), getXRot());
                getNavigation().stop();
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.0D, getZ(), 15, 0.3D, 0.6D, 0.3D, 0.05D);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.7F);
                doHurtTarget(target);
                shunshinCd = 100;
            } else {
                shunshinCd = 20;
            }
        }

        // Suiton: Suiryuudan - onda de agua que arremessa quem estiver perto
        if (shockCd <= 0 && d2 < 64.0D) {
            shockCd = 130;
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5.0D), this::canHarm)) {
                Vec3 push = e.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
                e.hurt(damageSources().mobAttack(this), 6.0F);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                e.setDeltaMovement(e.getDeltaMovement().add(push.x * 1.3D, 0.5D, push.z * 1.3D));
                e.hurtMarked = true;
            }
            server.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1.0D, getZ(), 80, 2.0D, 0.6D, 2.0D, 0.2D);
            server.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 1.0D, getZ(), 30, 1.5D, 0.6D, 1.5D, 0.05D);
            playSound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.2F, 0.8F);
        }
    }

    /** Hiruzen - Katon e Kawarimi (a troca esta em hurt()). */
    private void hiruzenTick(LivingEntity target) {
        double d2 = distanceToSqr(target);
        if (katonCd <= 0 && d2 >= 9.0D && d2 <= 400.0D && hasLineOfSight(target)) {
            katonCd = 90;
            Vec3 look = target.getEyePosition().subtract(getEyePosition()).normalize();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = look.yRot(i * 0.12F);
                SmallFireball ball = new SmallFireball(level(), this, dir.x, dir.y, dir.z);
                ball.setPos(getX() + dir.x * 1.5D, getEyeY() - 0.2D, getZ() + dir.z * 1.5D);
                level().addFreshEntity(ball);
            }
            playSound(SoundEvents.BLAZE_SHOOT, 1.2F, 0.9F);
        }
    }

    private void madaraTick(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        double d2 = distanceToSqr(target);

        // Susanoo: abaixo de 50% de vida ganha resistencia forte por 10 segundos
        if (susanooCd <= 0 && getHealth() < getMaxHealth() * 0.5F) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
            susanooCd = 600;
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0D, getZ(), 60, 0.6D, 1.0D, 0.6D, 0.05D);
            playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 0.6F);
        }

        // Shinra Tensei: onda de choque que arremessa todos que estiverem perto
        if (shockCd <= 0 && d2 < 36.0D) {
            shockCd = 160;
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0D), this::canHarm)) {
                Vec3 push = e.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
                e.hurt(damageSources().mobAttack(this), 8.0F);
                e.setDeltaMovement(e.getDeltaMovement().add(push.x * 1.6D, 0.6D, push.z * 1.6D));
                e.hurtMarked = true;
            }
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.0D, getZ(), 6, 1.5D, 0.5D, 1.5D, 0.0D);
            playSound(SoundEvents.GENERIC_EXPLODE, 1.0F, 1.2F);
            return;
        }

        // Katon: Goukakyuu no Jutsu - leque de bolas de fogo a distancia
        if (katonCd <= 0 && d2 >= 16.0D && d2 <= 576.0D && hasLineOfSight(target)) {
            katonCd = 70;
            Vec3 look = target.getEyePosition().subtract(getEyePosition()).normalize();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = look.yRot(i * 0.15F);
                SmallFireball ball = new SmallFireball(level(), this, dir.x, dir.y, dir.z);
                ball.setPos(getX() + dir.x * 1.5D, getEyeY() - 0.2D, getZ() + dir.z * 1.5D);
                level().addFreshEntity(ball);
            }
            playSound(SoundEvents.BLAZE_SHOOT, 1.2F, 0.7F);
        }
    }

    private void shisuiTick(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        double d2 = distanceToSqr(target);

        // Shunshin: some e reaparece atras do alvo, ja atacando
        if (shunshinCd <= 0 && d2 > 25.0D && d2 < 900.0D) {
            Vec3 back = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize().scale(-1.6D);
            double nx = target.getX() + back.x;
            double ny = target.getY();
            double nz = target.getZ() + back.z;
            AABB box = getBoundingBox().move(nx - getX(), ny - getY(), nz - getZ());
            if (level().noCollision(this, box)) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0D, getZ(), 20, 0.3D, 0.6D, 0.3D, 0.02D);
                moveTo(nx, ny, nz, getYRot(), getXRot());
                getNavigation().stop();
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0D, getZ(), 20, 0.3D, 0.6D, 0.3D, 0.02D);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.5F);
                doHurtTarget(target);
                shunshinCd = 80;
            } else {
                shunshinCd = 20; // sem espaco atras do alvo, tenta de novo logo
            }
        }

        // Genjutsu (Kotoamatsukami): cega, atordoa e deixa o alvo lento
        if (genjutsuCd <= 0 && d2 < 256.0D && hasLineOfSight(target)) {
            genjutsuCd = 200;
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            server.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getEyeY(), target.getZ(), 40, 0.4D, 0.4D, 0.4D, 0.8D);
            playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.8F);
        }
    }

    /** Larga o item do Kind (ex.: Mangekyou) ao morrer, mas so se NAO foi invocado pelo Edo Tensei. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (owner != null || kind.dropItem == null) return;
        ItemStack stack = EdoTargets.optionalItem(kind.dropItem);
        if (!stack.isEmpty()) this.spawnAtLocation(stack);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (owner != null && source.getEntity() != null && owner.equals(source.getEntity().getUUID())) {
            return false; // nao machuca quem o invocou
        }
        // Hiruzen - Kawarimi no Jutsu: com pouca vida, troca de lugar e evita o golpe
        if (kind == Kind.HIRUZEN && susanooCd <= 0 && !level().isClientSide && isAlive()
                && getHealth() < getMaxHealth() * 0.6F && source.getEntity() != null) {
            for (int i = 0; i < 10; i++) {
                double x = getX() + random.nextInt(11) - 5;
                double z = getZ() + random.nextInt(11) - 5;
                if (randomTeleport(x, getY(), z, true)) {
                    susanooCd = 400;
                    ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0D, getZ(), 25, 0.4D, 0.8D, 0.4D, 0.02D);
                    playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.4F);
                    return false;
                }
            }
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
            if (EdoTargets.isRevived(candidate)) return false; // nunca mira em outro revivido
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
