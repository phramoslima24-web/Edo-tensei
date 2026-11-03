package com.edotensei.client;

import com.edotensei.entity.CorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Desenha o mob original deitado no chao. */
public class CorpseRenderer extends EntityRenderer<CorpseEntity> {
    private final Map<String, Entity> ghosts = new HashMap<>();

    public CorpseRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private Entity ghostFor(String id, Level level) {
        Entity cached = ghosts.get(id);
        if (cached != null && cached.level() == level) return cached;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(rl);
        if (type == null) return null;
        Entity ghost = type.create(level);
        if (ghost == null) return null;
        ghost.setYRot(0.0F);
        ghost.setXRot(0.0F);
        ghost.setYHeadRot(0.0F);
        if (ghost instanceof LivingEntity living) {
            living.yBodyRot = 0.0F;
            living.yBodyRotO = 0.0F;
            living.yHeadRotO = 0.0F;
        }
        ghosts.put(id, ghost);
        return ghost;
    }

    @Override
    public void render(CorpseEntity corpse, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        // Durante o primeiro segundo o proprio mob ainda esta fazendo a animacao de morte.
        if (corpse.tickCount < 20) return;
        Entity ghost = ghostFor(corpse.getEntityTypeId(), corpse.level());
        if (ghost == null) return;

        // Corpo deitado: de costas (barriga pra cima) ou de bruços, com o meio do corpo no centro do hitbox
        // e a cabeça apontando pro lado de onde ele caiu.
        float height = ghost.getBbHeight();
        float thick = Math.max(0.12F, ghost.getBbWidth() * 0.22F);
        boolean faceUp = corpse.isFaceUp();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-corpse.getYRot()));
        pose.translate(0.0D, thick, faceUp ? height * 0.5D : -height * 0.5D);
        pose.mulPose(Axis.XP.rotationDegrees(faceUp ? -90.0F : 90.0F));
        try {
            Minecraft.getInstance().getEntityRenderDispatcher().render(ghost, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, pose, buffer, light);
        } catch (Throwable ignored) {
            // alguns renderizadores nao gostam de entidades "fantasma"; nesse caso so nao desenha
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseEntity corpse) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
}
