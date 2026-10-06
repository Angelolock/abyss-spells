package com.abyssspells.client;

import com.abyssspells.AbyssSpells;
import com.abyssspells.entity.AnchorMarkerEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Маркер якоря: темный ромб-кристалл в костяном кольце.
 */
public class AnchorMarkerModel extends HierarchicalModel<AnchorMarkerEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "anchor_marker"), "main");

    private final ModelPart root;

    public AnchorMarkerModel(ModelPart root) {
        this.root = root.getChild("root");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition main = root.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);

        // Ромб-кристалл: широкая середина, острые верх/низ
        main.addOrReplaceChild("gemMid",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -2.0f, -3.0f, 6.0f, 4.0f, 6.0f),
                PartPose.ZERO);
        main.addOrReplaceChild("gemTop",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, 2.0f, -2.0f, 4.0f, 3.0f, 4.0f),
                PartPose.ZERO);
        main.addOrReplaceChild("gemBot",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -5.0f, -2.0f, 4.0f, 3.0f, 4.0f),
                PartPose.ZERO);
        main.addOrReplaceChild("gemTipT",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 5.0f, -1.0f, 2.0f, 2.0f, 2.0f),
                PartPose.ZERO);
        main.addOrReplaceChild("gemTipB",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 2.0f, 2.0f),
                PartPose.ZERO);
        // Костяное кольцо вокруг
        main.addOrReplaceChild("ringN",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.0f, -0.5f, -5.0f, 8.0f, 1.0f, 1.0f),
                PartPose.ZERO);
        main.addOrReplaceChild("ringS",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.0f, -0.5f, 4.0f, 8.0f, 1.0f, 1.0f),
                PartPose.ZERO);
        main.addOrReplaceChild("ringE",
                CubeListBuilder.create().texOffs(40, 0).addBox(4.0f, -0.5f, -4.0f, 1.0f, 1.0f, 8.0f),
                PartPose.ZERO);
        main.addOrReplaceChild("ringW",
                CubeListBuilder.create().texOffs(40, 0).addBox(-5.0f, -0.5f, -4.0f, 1.0f, 1.0f, 8.0f),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AnchorMarkerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }
}
