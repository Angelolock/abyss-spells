package com.abyssspells.client;

import com.abyssspells.AbyssSpells;
import com.abyssspells.entity.VoidShacklesEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Клетка оков: широкий диск, костяное кольцо рун, 8 толстых темных щупалец
 * с костяными когтями, сходящимися над жертвой. Покачивание процедурное.
 */
public class VoidShacklesModel extends HierarchicalModel<VoidShacklesEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "void_shackles"), "main");

    private final ModelPart base;
    private final ModelPart[] lowers = new ModelPart[8];
    private final ModelPart[] middles = new ModelPart[8];
    private final ModelPart[] tips = new ModelPart[8];

    private static final float LOWER_TILT = -0.12f;
    private static final float MIDDLE_TILT = -0.5f;
    private static final float TIP_TILT = -0.55f;

    /** Текстура 128x64: верхняя половина темная, нижняя костяная. */
    private static final int DARK_U = 48, DARK_V = 0;
    private static final int BONE_U = 0, BONE_V = 40;

    public VoidShacklesModel(ModelPart root) {
        this.base = root.getChild("base");
        for (int i = 0; i < 8; i++) {
            lowers[i] = base.getChild("tentacle_" + i).getChild("lower");
            middles[i] = lowers[i].getChild("middle");
            tips[i] = middles[i].getChild("tip");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create(), PartPose.ZERO);

        // Диска-плиты больше нет (выглядел как черный ковер).
        // Вместо него у каждого щупальца корневая лапа + общее кольцо рун.

        // Костяное кольцо рун на диске: 12 камней
        for (int i = 0; i < 12; i++) {
            double a = (Math.PI * 2 * i) / 12;
            base.addOrReplaceChild("rune_" + i,
                    CubeListBuilder.create().texOffs(BONE_U, BONE_V).addBox(-1.0f, -1.0f, -1.0f, 2.0f, 1.0f, 2.0f),
                    PartPose.offset((float) (Math.cos(a) * 9.0), 22.0f, (float) (Math.sin(a) * 9.0)));
        }

        // 8 щупалец кольцом радиуса 14 (вне тела жертвы)
        for (int i = 0; i < 8; i++) {
            double a = (Math.PI * 2 * i) / 8;
            float px = (float) (Math.cos(a) * 14.0);
            float pz = (float) (Math.sin(a) * 14.0);
            float yaw = (float) (Math.PI / 2 - a); // локальный +Z смотрит наружу

            PartDefinition tent = base.addOrReplaceChild("tentacle_" + i, CubeListBuilder.create(),
                    PartPose.offsetAndRotation(px, 24.0f, pz, 0.0f, yaw, 0.0f));

            // Корневая лапа: темное утолщение в земле
            tent.addOrReplaceChild("foot",
                    CubeListBuilder.create().texOffs(DARK_U, DARK_V).addBox(-3.0f, -2.0f, -3.0f, 6.0f, 2.0f, 6.0f),
                    PartPose.offset(0.0f, 0.0f, 0.0f));

            // Нижний сегмент: толстый, вверх 14
            PartDefinition lower = tent.addOrReplaceChild("lower",
                    CubeListBuilder.create().texOffs(DARK_U, DARK_V).addBox(-2.0f, -14.0f, -2.0f, 4.0f, 14.0f, 4.0f),
                    PartPose.offsetAndRotation(0.0f, 0.0f, 1.2f, LOWER_TILT, 0.0f, 0.0f));

            // Средний сегмент: загибается внутрь
            PartDefinition middle = lower.addOrReplaceChild("middle",
                    CubeListBuilder.create().texOffs(DARK_U, DARK_V).addBox(-1.5f, -11.0f, -1.5f, 3.0f, 11.0f, 3.0f),
                    PartPose.offsetAndRotation(0.0f, -14.0f, 1.0f, MIDDLE_TILT, 0.0f, 0.0f));

            // Костяной коготь на конце: сходится над жертвой
            middle.addOrReplaceChild("tip",
                    CubeListBuilder.create().texOffs(BONE_U, BONE_V).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f),
                    PartPose.offsetAndRotation(0.0f, -11.0f, -0.8f, TIP_TILT, 0.0f, 0.0f));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return base;
    }

    @Override
    public void setupAnim(VoidShacklesEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        for (int i = 0; i < 8; i++) {
            float sway = (float) Math.sin(ageInTicks * 0.15f + i * 0.9f);
            lowers[i].xRot = LOWER_TILT + sway * 0.03f;
            middles[i].xRot = MIDDLE_TILT + sway * 0.05f;
            tips[i].xRot = TIP_TILT + (float) Math.sin(ageInTicks * 0.22f + i * 1.3f) * 0.09f;
        }
    }
}
