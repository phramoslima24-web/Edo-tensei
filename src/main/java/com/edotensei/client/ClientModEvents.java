package com.edotensei.client;

import com.edotensei.EdoTenseiAddon;
import com.edotensei.ModRegistry;
import com.edotensei.entity.HokageEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EdoTenseiAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (HokageEntity.Kind kind : HokageEntity.Kind.values()) {
            event.registerEntityRenderer(ModRegistry.HOKAGES.get(kind).get(), HokageRenderer::new);
        }
        event.registerEntityRenderer(ModRegistry.CORPSE.get(), CorpseRenderer::new);
    }
}
