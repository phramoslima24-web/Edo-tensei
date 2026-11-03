package com.edotensei;

import com.edotensei.block.EdoTenseiScrollBlock;
import com.edotensei.block.EdoTenseiScrollBlockEntity;
import com.edotensei.entity.CorpseEntity;
import com.edotensei.entity.HokageEntity;
import com.edotensei.item.BloodSyringeItem;
import com.edotensei.item.ShikiFujinItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EdoTenseiAddon.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EdoTenseiAddon.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EdoTenseiAddon.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EdoTenseiAddon.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EdoTenseiAddon.MODID);

    // Blocos
    public static final RegistryObject<Block> SCROLL_BLOCK = BLOCKS.register("edo_tensei_scroll", EdoTenseiScrollBlock::new);

    public static final RegistryObject<BlockEntityType<EdoTenseiScrollBlockEntity>> SCROLL_BE = BLOCK_ENTITIES.register("edo_tensei_scroll",
            () -> BlockEntityType.Builder.of(EdoTenseiScrollBlockEntity::new, SCROLL_BLOCK.get()).build(null));

    // Itens
    public static final RegistryObject<Item> SCROLL_ITEM = ITEMS.register("edo_tensei_scroll",
            () -> new BlockItem(SCROLL_BLOCK.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SYRINGE = ITEMS.register("syringe", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BLOOD_SYRINGE = ITEMS.register("blood_syringe", BloodSyringeItem::new);
    public static final RegistryObject<Item> EDO_PAPER = ITEMS.register("edo_tensei_paper", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SHIKI_FUJIN = ITEMS.register("shiki_fujin", ShikiFujinItem::new);

    // Hokages (entidades + spawn eggs)
    public static final Map<HokageEntity.Kind, RegistryObject<EntityType<HokageEntity>>> HOKAGES = new EnumMap<>(HokageEntity.Kind.class);

    static {
        for (HokageEntity.Kind kind : HokageEntity.Kind.values()) {
            RegistryObject<EntityType<HokageEntity>> type = ENTITIES.register(kind.id,
                    () -> EntityType.Builder.<HokageEntity>of((t, l) -> new HokageEntity(t, l, kind), MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(EdoTenseiAddon.MODID, kind.id).toString()));
            HOKAGES.put(kind, type);
            ITEMS.register(kind.id + "_spawn_egg",
                    () -> new ForgeSpawnEggItem(type, kind.eggPrimary, kind.eggSecondary, new Item.Properties()));
        }
    }

    public static final RegistryObject<EntityType<CorpseEntity>> CORPSE = ENTITIES.register("corpse",
            () -> EntityType.Builder.<CorpseEntity>of(CorpseEntity::new, MobCategory.MISC)
                    .sized(1.6F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build(new ResourceLocation(EdoTenseiAddon.MODID, "corpse").toString()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.edotensei"))
                    .icon(() -> new ItemStack(SCROLL_ITEM.get()))
                    .displayItems((params, out) -> {
                        for (RegistryObject<Item> item : ITEMS.getEntries()) {
                            if (item.get() == BLOOD_SYRINGE.get()) {
                                for (HokageEntity.Kind kind : HokageEntity.Kind.values()) {
                                    out.accept(BloodSyringeItem.create(EdoTenseiAddon.MODID + ":" + kind.id));
                                }
                            } else {
                                out.accept(item.get());
                            }
                        }
                    })
                    .build());
}
