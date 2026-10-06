package com.abyssspells.client;

import com.abyssspells.AbyssSpells;
import com.abyssspells.entity.AnchorMarkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class AnchorMarkerRenderer extends EntityRenderer<AnchorMarkerEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "textures/entity/abyss_core.png");

    private final AnchorMarkerModel model;

    public AnchorMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new AnchorMarkerModel(context.bakeLayer(AnchorMarkerModel.LAYER_LOCATION));
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(AnchorMarkerEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float age = entity.tickCount + partialTick;
        // Появление/исчезновение по краям жизни
        float grow = Math.min(1.0f, Math.min(age / 10.0f, 1.0f));
        poseStack.translate(0.0, Math.sin(age * 0.1f) * 0.1, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 25.0f));
        poseStack.scale(grow, grow, grow);

        model.setupAnim(entity, 0.0f, 0.0f, age, 0.0f, 0.0f);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AnchorMarkerEntity entity) {
        return TEXTURE;
    }
}
