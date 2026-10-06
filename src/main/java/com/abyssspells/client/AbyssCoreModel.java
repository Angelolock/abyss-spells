package com.abyssspells.client;

import com.abyssspells.AbyssSpells;
import com.abyssspells.entity.AbyssCoreEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Граненое ядро: ступенчатая сфера + костяное кольцо + 6 шипов с наконечниками.
 */
public class AbyssCoreModel extends HierarchicalModel<AbyssCoreEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "abyss_core"), "main");

    private final ModelPart core;
    private final ModelPart moon1;
    private final ModelPart moon2;

    public AbyssCoreModel(ModelPart root) {
        this.core = root.getChild("core");
        this.moon1 = core.getChild("moon1");
        this.moon2 = core.getChild("moon2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create(), PartPose.ZERO);

        // Ступенчатая сфера
        core.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -2.0f, -5.0f, 10.0f, 4.0f, 10.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("up",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, 2.0f, -4.0f, 8.0f, 3.0f, 8.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("down",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -5.0f, -4.0f, 8.0f, 3.0f, 8.0f),
                PartPose.ZERO);
        // Костяное кольцо из 4 планок
        core.addOrReplaceChild("ringN",
                CubeListBuilder.create().texOffs(40, 0).addBox(-5.0f, -1.0f, -6.0f, 10.0f, 2.0f, 1.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("ringS",
                CubeListBuilder.create().texOffs(40, 0).addBox(-5.0f, -1.0f, 5.0f, 10.0f, 2.0f, 1.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("ringE",
                CubeListBuilder.create().texOffs(40, 0).addBox(5.0f, -1.0f, -5.0f, 1.0f, 2.0f, 10.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("ringW",
                CubeListBuilder.create().texOffs(40, 0).addBox(-6.0f, -1.0f, -5.0f, 1.0f, 2.0f, 10.0f),
                PartPose.ZERO);
        // Хребты-гребешки на сфере
        core.addOrReplaceChild("ridgeT",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.0f, 5.0f, -0.5f, 8.0f, 0.5f, 1.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("ridgeF",
                CubeListBuilder.create().texOffs(40, 0).addBox(-0.5f, -2.0f, -5.5f, 1.0f, 4.0f, 0.5f),
                PartPose.ZERO);
        core.addOrReplaceChild("ridgeR",
                CubeListBuilder.create().texOffs(40, 0).addBox(4.5f, -2.0f, -0.5f, 0.5f, 4.0f, 1.0f),
                PartPose.ZERO);
        // Наклонное кольцо-спутник
        PartDefinition tilt = core.addOrReplaceChild("tiltring", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.3f));
        tilt.addOrReplaceChild("tiltN",
                CubeListBuilder.create().texOffs(40, 0).addBox(-5.0f, -0.5f, -7.0f, 10.0f, 1.0f, 1.0f),
                PartPose.ZERO);
        tilt.addOrReplaceChild("tiltS",
                CubeListBuilder.create().texOffs(40, 0).addBox(-5.0f, -0.5f, 6.0f, 10.0f, 1.0f, 1.0f),
                PartPose.ZERO);
        tilt.addOrReplaceChild("tiltE",
                CubeListBuilder.create().texOffs(40, 0).addBox(6.0f, -0.5f, -5.0f, 1.0f, 1.0f, 10.0f),
                PartPose.ZERO);
        tilt.addOrReplaceChild("tiltW",
                CubeListBuilder.create().texOffs(40, 0).addBox(-7.0f, -0.5f, -5.0f, 1.0f, 1.0f, 10.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("merN",
                CubeListBuilder.create().texOffs(40, 0).addBox(-0.5f, -5.0f, -5.5f, 1.0f, 10.0f, 1.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("merS",
                CubeListBuilder.create().texOffs(40, 0).addBox(-0.5f, -5.0f, 4.5f, 1.0f, 10.0f, 1.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("merE",
                CubeListBuilder.create().texOffs(40, 0).addBox(4.5f, -5.0f, -0.5f, 1.0f, 10.0f, 1.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("merW",
                CubeListBuilder.create().texOffs(40, 0).addBox(-5.5f, -5.0f, -0.5f, 1.0f, 10.0f, 1.0f),
                PartPose.ZERO);
        // Костяные колпачки полюсов
        core.addOrReplaceChild("capT",
                CubeListBuilder.create().texOffs(40, 0).addBox(-2.0f, 5.0f, -2.0f, 4.0f, 1.0f, 4.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("capB",
                CubeListBuilder.create().texOffs(40, 0).addBox(-2.0f, -6.0f, -2.0f, 4.0f, 1.0f, 4.0f),
                PartPose.ZERO);
        // Две луны на орбите
        core.addOrReplaceChild("moon1",
                CubeListBuilder.create().texOffs(0, 0).addBox(7.0f, 1.0f, -1.5f, 3.0f, 3.0f, 3.0f),
                PartPose.ZERO);
        core.addOrReplaceChild("moon2",
                CubeListBuilder.create().texOffs(40, 0).addBox(-10.0f, -4.0f, -1.0f, 2.0f, 2.0f, 2.0f),
                PartPose.ZERO);
        // Удлиненные верхний/нижний шипы-шпили
        spike(core, "U", new float[]{-1, 5, -1}, new float[]{2, 3, 2}, new float[]{-1, 8, -1}, new float[]{2, 1, 2});
        spike(core, "D", new float[]{-1, -8, -1}, new float[]{2, 3, 2}, new float[]{-1, -9, -1}, new float[]{2, 1, 2});
        spike(core, "N", new float[]{-1, -1, -8}, new float[]{2, 2, 3}, new float[]{-1, -1, -9}, new float[]{2, 2, 1});
        spike(core, "S", new float[]{-1, -1, 5}, new float[]{2, 2, 3}, new float[]{-1, -1, 8}, new float[]{2, 2, 1});
        spike(core, "E", new float[]{5, -1, -1}, new float[]{3, 2, 2}, new float[]{8, -1, -1}, new float[]{1, 2, 2});
        spike(core, "W", new float[]{-8, -1, -1}, new float[]{3, 2, 2}, new float[]{-9, -1, -1}, new float[]{1, 2, 2});

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void spike(PartDefinition core, String name,
                              float[] baseO, float[] baseS, float[] tipO, float[] tipS) {
        core.addOrReplaceChild("spike" + name,
                CubeListBuilder.create().texOffs(0, 0).addBox(baseO[0], baseO[1], baseO[2], (int) baseS[0], (int) baseS[1], (int) baseS[2]),
                PartPose.ZERO);
        core.addOrReplaceChild("spike" + name + "tip",
                CubeListBuilder.create().texOffs(40, 0).addBox(tipO[0], tipO[1], tipO[2], (int) tipS[0], (int) tipS[1], (int) tipS[2]),
                PartPose.ZERO);
    }

    @Override
    public ModelPart root() {
        return core;
    }

    @Override
    public void setupAnim(AbyssCoreEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Луны на орбите в разные стороны
        moon1.yRot = ageInTicks * 0.05f;
        moon2.yRot = -ageInTicks * 0.035f + 2.0f;
    }
}
