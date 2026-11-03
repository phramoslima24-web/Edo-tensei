package com.edotensei.event;

import com.edotensei.EdoTenseiAddon;
import com.edotensei.jutsu.HokageTeachings;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /edotensei_aprender <jutsu>: usado pelos botoes clicaveis do dialogo dos Hokages. */
@Mod.EventBusSubscriber(modid = EdoTenseiAddon.MODID)
public final class CommandEvents {
    private CommandEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("edotensei_aprender")
                .then(Commands.argument("jutsu", StringArgumentType.word())
                        .executes(ctx -> HokageTeachings.requestLearn(
                                ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "jutsu")))));
    }
}
