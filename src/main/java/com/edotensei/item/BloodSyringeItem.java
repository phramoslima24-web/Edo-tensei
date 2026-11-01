package com.edotensei.item;

import com.edotensei.ModRegistry;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Seringa com sangue de um Hokage (guarda o id do Hokage na tag "Hokage"). */
public class BloodSyringeItem extends Item {
    public static final String TAG = "Hokage";

    public BloodSyringeItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static ItemStack create(String hokageId) {
        ItemStack stack = new ItemStack(ModRegistry.BLOOD_SYRINGE.get());
        stack.getOrCreateTag().putString(TAG, hokageId);
        return stack;
    }

    public static String getHokage(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString(TAG) : "";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String id = getHokage(stack);
        if (!id.isEmpty()) {
            tooltip.add(Component.translatable("entity.edotensei." + id).withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
