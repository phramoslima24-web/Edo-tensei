package com.edotensei.entity;

import com.edotensei.EdoTargets;
import java.util.EnumSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

/** Metas genericas para qualquer mob invocado pelo Edo Tensei: seguir e proteger o invocador. */
public final class EdoGoals {
    private EdoGoals() {}

    @Nullable
    static LivingEntity owner(Mob mob) {
        if (mob.level().isClientSide) return null;
        UUID uuid = EdoTargets.ownerOf(mob);
        return uuid == null ? null : mob.level().getPlayerByUUID(uuid);
    }

    public static class Follow extends Goal {
        private final Mob mob;
        private LivingEntity owner;
        private int recalc;

        public Follow(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = owner(mob);
            if (o == null || !o.isAlive() || mob.getTarget() != null) return false;
            if (o.level() != mob.level() || mob.distanceToSqr(o) < 100.0D) return false;
            this.owner = o;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && mob.getTarget() == null && mob.distanceToSqr(owner) > 9.0D;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void stop() {
            owner = null;
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(owner, 10.0F, (float) mob.getMaxHeadXRot());
            if (--recalc <= 0) {
                recalc = 10;
                if (mob.distanceToSqr(owner) >= 900.0D) {
                    for (int i = 0; i < 10; i++) {
                        double x = owner.getX() + mob.getRandom().nextInt(7) - 3;
                        double z = owner.getZ() + mob.getRandom().nextInt(7) - 3;
                        if (mob.randomTeleport(x, owner.getY(), z, true)) break;
                    }
                } else {
                    mob.getNavigation().moveTo(owner, 1.25D);
                }
            }
        }
    }

    public static class Protect extends TargetGoal {
        private LivingEntity wanted;

        public Protect(Mob mob) {
            super(mob, false);
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = owner(mob);
            if (owner == null) return false;

            LivingEntity candidate = owner.getLastHurtByMob();
            if (candidate == null || owner.tickCount - owner.getLastHurtByMobTimestamp() > 200) {
                candidate = owner.getLastHurtMob();
                if (candidate == null || owner.tickCount - owner.getLastHurtMobTimestamp() > 200) return false;
            }
            if (candidate == owner || candidate == mob || !candidate.isAlive()) return false;
            if (owner.getUUID().equals(EdoTargets.ownerOf(candidate))) return false; // aliados do mesmo invocador
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
