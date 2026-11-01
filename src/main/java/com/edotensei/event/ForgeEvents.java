package com.edotensei.event;

import com.edotensei.EdoTenseiAddon;
import com.edotensei.ModRegistry;
import com.edotensei.entity.HokageEntity;
import com.edotensei.item.BloodSyringeItem;
import com.edotensei.jutsu.Jutsus;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        boolean client = player.level().isClientSide;

        // 1) Seringa vazia no corpo de um Hokage = coletar sangue
        if (target instanceof HokageEntity hokage && hokage.isDeadOrDying()) {
            ItemStack held = event.getItemStack();
            if (held.is(ModRegistry.SYRINGE.get())) {
                if (!client) {
                    if (hokage.isHarvested()) {
                        player.displayClientMessage(Component.translatable("message.edotensei.already_drained"), true);
                    } else {
                        hokage.setHarvested(true);
                        if (!player.getAbilities().instabuild) held.shrink(1);
                        ItemStack blood = BloodSyringeItem.create(hokage.getKind().id);
                        if (!player.getInventory().add(blood)) player.drop(blood, false);
                        player.level().playSound(null, hokage.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 0.8F);
                        player.displayClientMessage(Component.translatable("message.edotensei.blood_collected",
                                Component.translatable("entity.edotensei." + hokage.getKind().id)), true);
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
