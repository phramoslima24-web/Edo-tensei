package com.edotensei.client;

import com.edotensei.EdoTenseiAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
