package com.edotensei.client;

import com.edotensei.jutsu.Jutsus;
import com.edotensei.network.LearnJutsuPacket;
import com.edotensei.network.ModNetwork;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Categoria "Jutsus Proibidos": Shiki Fujin e Edo Tensei. */
public class ForbiddenJutsuScreen extends Screen {
    @Nullable
    private final Screen parent;

    public ForbiddenJutsuScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.edotensei.forbidden_jutsus"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("jutsu.edotensei.shiki_fujin").append(" (" + Jutsus.COST_SHIKI_FUJIN + " XP)"),
                        b -> learn(Jutsus.SHIKI_FUJIN))
                .bounds(cx - 110, y, 220, 20).build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("jutsu.edotensei.edo_tensei").append(" (" + Jutsus.COST_EDO_TENSEI + " XP)"),
                        b -> learn(Jutsus.EDO_TENSEI))
                .bounds(cx - 110, y + 50, 220, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
                .bounds(cx - 50, y + 100, 100, 20).build());
    }

    private void learn(String id) {
        ModNetwork.CHANNEL.sendToServer(new LearnJutsuPacket(id));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int cx = this.width / 2;
        int y = this.height / 2 - 40;
        graphics.drawCenteredString(this.font, this.title, cx, 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("jutsu.edotensei.shiki_fujin.desc"), cx, y + 24, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.translatable("jutsu.edotensei.edo_tensei.desc"), cx, y + 74, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
