package com.edotensei.jutsu;

import com.edotensei.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Jutsus proibidos: aprendizado (guardado no jogador) e entrega do item. */
public final class Jutsus {
    private Jutsus() {}

    public static final String SHIKI_FUJIN = "shiki_fujin";
    public static final String EDO_TENSEI = "edo_tensei";

    /** Chave do NBT persistente do jogador. */
    public static final String KEY = "edotensei_jutsus";
    /** Chave (no mob) de quem selou o mob com kunai + papel. */
    public static final String SEALER_TAG = "edotensei_sealed_by";

    public static final int COST_SHIKI_FUJIN = 40;
    public static final int COST_EDO_TENSEI = 30;

    public static boolean isLearned(Player player, String id) {
        return player.getPersistentData().getCompound(KEY).getBoolean(id);
    }

    public static void learn(Player player, String id) {
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(KEY);
        tag.putBoolean(id, true);
        root.put(KEY, tag);
    }

    private static ItemStack stackFor(String id) {
        if (SHIKI_FUJIN.equals(id)) return new ItemStack(ModRegistry.SHIKI_FUJIN.get());
        if (EDO_TENSEI.equals(id)) return new ItemStack(ModRegistry.SCROLL_ITEM.get());
        return ItemStack.EMPTY;
    }

    private static int costOf(String id) {
        return SHIKI_FUJIN.equals(id) ? COST_SHIKI_FUJIN : COST_EDO_TENSEI;
    }

    /** Chamado no servidor quando o jogador clica no jutsu na tela de jutsus proibidos. */
    public static void requestLearn(ServerPlayer player, String id) {
        ItemStack stack = stackFor(id);
        if (stack.isEmpty()) return;

        if (!isLearned(player, id)) {
            int cost = costOf(id);
            if (!player.isCreative() && player.experienceLevel < cost) {
                player.sendSystemMessage(Component.translatable("message.edotensei.not_enough_levels", cost));
                return;
            }
            if (!player.isCreative()) player.giveExperienceLevels(-cost);
            learn(player, id);
            player.sendSystemMessage(Component.translatable("message.edotensei.learned", Component.translatable("jutsu.edotensei." + id)));
        } else if (player.getInventory().contains(stack)) {
            player.sendSystemMessage(Component.translatable("message.edotensei.already_have"));
            return;
        }

        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
