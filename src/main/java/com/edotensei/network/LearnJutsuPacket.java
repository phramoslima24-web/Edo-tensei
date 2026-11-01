package com.edotensei.network;

import com.edotensei.jutsu.Jutsus;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class LearnJutsuPacket {
    private final String id;

    public LearnJutsuPacket(String id) {
        this.id = id;
    }

    public static void encode(LearnJutsuPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.id, 32);
    }

    public static LearnJutsuPacket decode(FriendlyByteBuf buf) {
        return new LearnJutsuPacket(buf.readUtf(32));
    }

    public static void handle(LearnJutsuPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) Jutsus.requestLearn(player, msg.id);
        });
        context.setPacketHandled(true);
    }
}
