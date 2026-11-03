package com.edotensei.client;

import com.edotensei.EdoTenseiAddon;
import com.edotensei.entity.HokageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class HokageRenderer extends HumanoidMobRenderer<HokageEntity, HumanoidModel<HokageEntity>> {
    public HokageRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    /** Sem o tombo de lado do Minecraft: o Hokage morto cai de bruços ou de costas. */
    @Override
    protected float getFlipDegrees(HokageEntity entity) {
        return 0.0F;
    }

    @Override
    protected void setupRotations(HokageEntity entity, PoseStack pose, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, pose, ageInTicks, rotationYaw, partialTicks);
        if (entity.deathTime > 0) {
            float t = Mth.clamp((entity.deathTime + partialTicks - 1.0F) / 8.0F, 0.0F, 1.0F);
            t = t * t * (3.0F - 2.0F * t);
            boolean faceDown = (entity.getUUID().hashCode() & 1) == 0;
            pose.translate(0.0D, 0.22D * t, 0.0D);
            pose.mulPose(Axis.XP.rotationDegrees((faceDown ? -90.0F : 90.0F) * t));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(HokageEntity entity) {
        return new ResourceLocation(EdoTenseiAddon.MODID, "textures/entity/" + entity.getKind().id + ".png");
    }
}
