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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Desenha o ninja original caindo e ficando deitado no chao, de bruços ou de costas. */
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
        Entity ghost = ghostFor(corpse.getEntityTypeId(), corpse.level());
        if (ghost == null) return;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-corpse.getYRot()));
        // Cai de bruços (para frente) ou de costas (para trás); ao reaparecer já fica deitado.
        float t = corpse.isSettled() ? 1.0F : Mth.clamp((corpse.tickCount + partialTick) / CorpseEntity.FALL_TICKS, 0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);
        pose.translate(0.0D, 0.22D * t, 0.0D); // o corpo fica apoiado no chão, não afundado
        pose.mulPose(Axis.XP.rotationDegrees((corpse.isFaceDown() ? 90.0F : -90.0F) * t));
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
