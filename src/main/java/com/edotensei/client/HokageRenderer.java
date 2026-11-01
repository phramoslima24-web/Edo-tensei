package com.edotensei.client;

import com.edotensei.EdoTenseiAddon;
import com.edotensei.entity.HokageEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HokageRenderer extends HumanoidMobRenderer<HokageEntity, HumanoidModel<HokageEntity>> {
    public HokageRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(HokageEntity entity) {
        return new ResourceLocation(EdoTenseiAddon.MODID, "textures/entity/" + entity.getKind().id + ".png");
    }
}
