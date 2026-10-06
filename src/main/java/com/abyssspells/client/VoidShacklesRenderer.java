package com.abyssspells.client;

import com.abyssspells.AbyssSpells;
import com.abyssspells.entity.VoidShacklesEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class VoidShacklesRenderer extends EntityRenderer<VoidShacklesEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "textures/entity/void_shackles.png");

    private final VoidShacklesModel model;

    public VoidShacklesRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new VoidShacklesModel(context.bakeLayer(VoidShacklesModel.LAYER_LOCATION));
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(VoidShacklesEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float age = entity.tickCount + partialTick;
        // Вырастание из земли в первые 10 тиков
        float emerge = Math.min(1.0f, age / 10.0f);
        // Клетка расширяется под широких жертв (лошадь и т.п.)
        float victimWidth = entity.getFirstPassenger() != null
                ? entity.getFirstPassenger().getBbWidth() : 1.0f;
        float s = emerge * net.minecraft.util.Mth.clamp(victimWidth / 1.4f, 1.0f, 1.6f);
        poseStack.scale(s, 1.0f, s);

        model.setupAnim(entity, 0.0f, 0.0f, age, 0.0f, 0.0f);
        model.renderToBuffer(poseStack, buffer.getBuffer(model.renderType(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidShacklesEntity entity) {
        return TEXTURE;
    }
}
