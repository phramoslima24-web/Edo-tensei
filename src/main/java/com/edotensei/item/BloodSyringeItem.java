package com.edotensei.item;

import com.edotensei.EdoTargets;
import com.edotensei.ModRegistry;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Seringa com sangue: guarda o id completo do tipo de mob (ex.: "narutoto:itachi_npc") na tag "Entity". */
public class BloodSyringeItem extends Item {
    public static final String TAG = "Entity";

    public BloodSyringeItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static ItemStack create(String entityTypeId) {
        ItemStack stack = new ItemStack(ModRegistry.BLOOD_SYRINGE.get());
        stack.getOrCreateTag().putString(TAG, entityTypeId);
        return stack;
    }

    public static String getEntity(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString(TAG) : "";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String id = getEntity(stack);
        if (!id.isEmpty()) {
            tooltip.add(EdoTargets.nameOf(id).copy().withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
