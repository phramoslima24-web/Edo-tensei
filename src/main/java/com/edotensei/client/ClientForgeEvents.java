package com.edotensei.client;

import com.edotensei.EdoTargets;
import com.edotensei.EdoTenseiAddon;
import com.edotensei.entity.CorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = EdoTenseiAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    /** Telas do Narutoto onde o botao "Jutsus Proibidos" aparece (canto superior direito). */
    private static boolean isNarutotoJutsuScreen(Screen screen) {
        String name = screen.getClass().getName();
        return name.equals("net.mcreator.narutoto.client.gui.JutsuBoardScreen")
                || name.equals("net.mcreator.narutoto.client.gui.OtherBoardScreen");
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!isNarutotoJutsuScreen(screen)) return;

        event.addListener(Button.builder(
                        Component.translatable("gui.edotensei.forbidden_jutsus"),
                        b -> Minecraft.getInstance().setScreen(new ForbiddenJutsuScreen(screen)))
                .bounds(screen.width - 130, 8, 122, 20)
                .build());
    }

    /**
     * Ninja morrendo: esconde o tombo de lado do mob original, porque o corpo (CorpseEntity)
     * ja cai de bruços ou de costas no mesmo lugar. Só ninjas; os demais mobs morrem normalmente.
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!entity.isDeadOrDying() || !EdoTargets.isRevivable(entity)) return;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null) return;
        String id = key.toString();
        if (!entity.level().getEntitiesOfClass(CorpseEntity.class, entity.getBoundingBox().inflate(1.0D),
                c -> c.getEntityTypeId().equals(id)).isEmpty()) {
            event.setCanceled(true);
        }
    }

    /** Plano B: /jutsusproibidos abre a mesma tela. */
    @SubscribeEvent
    public static void onClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jutsusproibidos").executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            mc.tell(() -> mc.setScreen(new ForbiddenJutsuScreen(null)));
            return 1;
        }));
    }
}
