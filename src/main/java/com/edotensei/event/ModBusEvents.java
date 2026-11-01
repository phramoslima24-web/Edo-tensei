package com.edotensei.event;

import com.edotensei.EdoTenseiAddon;
import com.edotensei.ModRegistry;
import com.edotensei.entity.HokageEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EdoTenseiAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBusEvents {
    private ModBusEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        for (HokageEntity.Kind kind : HokageEntity.Kind.values()) {
            event.put(ModRegistry.HOKAGES.get(kind).get(), HokageEntity.createAttributes(kind).build());
        }
    }

    @SubscribeEvent
    public static void onSpawnPlacement(SpawnPlacementRegisterEvent event) {
        for (HokageEntity.Kind kind : HokageEntity.Kind.values()) {
            event.register(ModRegistry.HOKAGES.get(kind).get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    HokageEntity::checkSpawn,
                    SpawnPlacementRegisterEvent.Operation.AND);
        }
    }
}
