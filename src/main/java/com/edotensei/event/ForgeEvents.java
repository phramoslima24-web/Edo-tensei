package com.edotensei.event;

import com.edotensei.EdoTargets;
import com.edotensei.EdoTenseiAddon;
import com.edotensei.ModRegistry;
import com.edotensei.entity.CorpseEntity;
import com.edotensei.entity.EdoGoals;
import com.edotensei.entity.HokageEntity;
import com.edotensei.item.BloodSyringeItem;
import com.edotensei.jutsu.Jutsus;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = EdoTenseiAddon.MODID)
public final class ForgeEvents {
    private ForgeEvents() {}

    private static boolean isKunai(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && key.getNamespace().equals("narutoto") && key.getPath().contains("kunai");
    }

    private static boolean isPaper(ItemStack stack) {
        return stack.is(ModRegistry.EDO_PAPER.get());
    }

    private static void giveBlood(Player player, String entityTypeId, ItemStack syringe) {
        if (!player.getAbilities().instabuild) syringe.shrink(1);
        ItemStack blood = BloodSyringeItem.create(entityTypeId);
        if (!player.getInventory().add(blood)) player.drop(blood, false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.edotensei.blood_collected", EdoTargets.nameOf(entityTypeId)), true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        boolean client = player.level().isClientSide;

        // 1) Seringa vazia num corpo (Hokage ou qualquer mob do Narutoto) = coletar sangue
        boolean hokageCorpse = target instanceof HokageEntity h && h.isDeadOrDying();
        if (hokageCorpse || target instanceof CorpseEntity) {
            ItemStack held = event.getItemStack();
            if (held.is(ModRegistry.SYRINGE.get())) {
                if (!client) {
                    if (target instanceof HokageEntity hokage) {
                        if (hokage.isHarvested()) {
                            player.displayClientMessage(Component.translatable("message.edotensei.already_drained"), true);
                        } else {
                            hokage.setHarvested(true);
                            giveBlood(player, EdoTenseiAddon.MODID + ":" + hokage.getKind().id, held);
                        }
                    } else {
                        CorpseEntity corpse = (CorpseEntity) target;
                        if (corpse.isHarvested()) {
                            player.displayClientMessage(Component.translatable("message.edotensei.already_drained"), true);
                        } else if (EdoTargets.isValidType(corpse.getEntityTypeId())) {
                            corpse.setHarvested(true);
                            giveBlood(player, corpse.getEntityTypeId(), held);
                        }
                    }
                }
                event.setCancellationResult(InteractionResult.sidedSuccess(client));
                event.setCanceled(true);
            }
            return;
        }

        // 2) Kunai + papel do Edo Tensei num mob = imobiliza (sela) o mob
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(target instanceof Mob mob)) return;
        if (mob instanceof HokageEntity || mob.getType().is(Tags.EntityTypes.BOSSES)) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack paper;
        if (isPaper(main) && isKunai(off)) {
            paper = main;
        } else if (isPaper(off) && isKunai(main)) {
            paper = off;
        } else {
            return;
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(client));
        event.setCanceled(true);
        if (client) return;

        if (mob.getPersistentData().hasUUID(Jutsus.SEALER_TAG)) {
            player.displayClientMessage(Component.translatable("message.edotensei.already_sealed"), true);
            return;
        }

        if (!player.getAbilities().instabuild) paper.shrink(1);
        mob.getPersistentData().putUUID(Jutsus.SEALER_TAG, player.getUUID());
        mob.setTarget(null);
        mob.setNoAi(true);
        mob.getNavigation().stop();
        mob.setPersistenceRequired();
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 10, 0, false, false));
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.5D);
        player.level().playSound(null, mob.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.displayClientMessage(Component.translatable("message.edotensei.sealed"), true);
    }

    /** Mobs do Narutoto deixam o corpo no chao ao morrer. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !EdoTargets.isRevivable(entity)) return;
        if (entity.getPersistentData().getBoolean(EdoTargets.NO_CORPSE_TAG)) return;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null) return;

        CorpseEntity corpse = ModRegistry.CORPSE.get().create(entity.level());
        if (corpse == null) return;
        corpse.setup(key.toString(), entity.getYRot());
        corpse.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), 0.0F);
        entity.level().addFreshEntity(corpse);
    }

    /** Mobs invocados (qualquer um do Narutoto): seguem e protegem o invocador. */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof Mob mob && !(mob instanceof HokageEntity) && mob.getPersistentData().hasUUID(EdoTargets.OWNER_TAG)) {
            mob.targetSelector.addGoal(0, new EdoGoals.Protect(mob));
            mob.goalSelector.addGoal(1, new EdoGoals.Follow(mob));
        }
    }

    /** Invocados nunca miram no proprio invocador. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget == null) return;
        UUID owner = EdoTargets.ownerOf(event.getEntity());
        if (owner != null && owner.equals(newTarget.getUUID())) {
            event.setCanceled(true);
        }
    }

    /** Sem dano entre o invocador e suas criaturas. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;
        LivingEntity victim = event.getEntity();
        UUID attackerOwner = EdoTargets.ownerOf(attacker);
        UUID victimOwner = EdoTargets.ownerOf(victim);
        if ((attackerOwner != null && attackerOwner.equals(victim.getUUID()))
                || (victimOwner != null && victimOwner.equals(attacker.getUUID()))) {
            event.setCanceled(true);
        }
    }

    /** Mantem os jutsus aprendidos depois de morrer. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        if (event.getOriginal().getPersistentData().contains(Jutsus.KEY)) {
            event.getEntity().getPersistentData().put(Jutsus.KEY, event.getOriginal().getPersistentData().get(Jutsus.KEY).copy());
        }
        event.getOriginal().invalidateCaps();
    }
}
