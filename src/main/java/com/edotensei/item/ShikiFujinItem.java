package com.edotensei.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Shiki Fujin (Selo do Ceifador da Morte): mata o alvo na sua mira, mas cobra um preco
 * altissimo do usuario (vida quase zerada + debuffs) e entra em recarga longa.
 */
public class ShikiFujinItem extends Item {
    private static final double RANGE = 12.0D;
    private static final int COOLDOWN_TICKS = 20 * 60 * 5;

    public ShikiFujinItem() {
        super(new Item.Properties().stacksTo(1).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, box,
                e -> e instanceof LivingEntity && !(e instanceof Player) && e.isAlive() && e.isPickable());

        if (hit == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.edotensei.no_target"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            LivingEntity target = (LivingEntity) hit.getEntity();
            ServerLevel server = (ServerLevel) level;
            boolean boss = target.getType().is(Tags.EntityTypes.BOSSES);

            target.invulnerableTime = 0;
            target.hurt(level.damageSources().indirectMagic(player, player), boss ? 300.0F : Float.MAX_VALUE);

            server.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.0D, target.getZ(), 60, 0.4D, 0.8D, 0.4D, 0.05D);
            server.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.05D);
            level.playSound(null, target.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 1.5F);

            if (!player.isCreative()) {
                player.setHealth(Math.min(player.getHealth(), 4.0F));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 180, 1));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 120, 1));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 60, 1));
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            }
        }
        return InteractionResultHolder.success(stack);
    }
}
