package com.abyssspells.client;

import com.abyssspells.AbyssSpells;
import com.abyssspells.entity.AbyssCoreEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class AbyssCoreRenderer extends EntityRenderer<AbyssCoreEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "textures/entity/abyss_core.png");

    private final AbyssCoreModel model;

    public AbyssCoreRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new AbyssCoreModel(context.bakeLayer(AbyssCoreModel.LAYER_LOCATION));
        this.shadowRadius = 0.6f;
    }

    @Override
    public void render(AbyssCoreEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float age = entity.tickCount + partialTick;
        // Появление: вырастание за 10 тиков
        float grow = Math.min(1.0f, age / 10.0f);
        // Левитация: легкое покачивание вверх-вниз
        float bob = (float) Math.sin(age * 0.12f) * 0.12f;
        // Модель центрируем в хитбоксе 3x3 (он идет от ног вверх)
        poseStack.translate(0.0, 1.5 + bob + (1.0f - grow) * -0.4f, 0.0);
        // Медленное вращение (замороженное почти стоит)
        poseStack.mulPose(Axis.YP.rotationDegrees(age * (entity.isFrozen() ? 3.0f : 14.0f)));
        poseStack.scale(3.2f * grow, 3.2f * grow, 3.2f * grow);

        model.setupAnim(entity, 0.0f, 0.0f, age, 0.0f, 0.0f);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        // Полный свет — ядро светится во тьме
        model.renderToBuffer(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssCoreEntity entity) {
        return TEXTURE;
    }
}
