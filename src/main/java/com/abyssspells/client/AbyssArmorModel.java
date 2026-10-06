package com.abyssspells.client;

import com.abyssspells.AbyssSpells;
import com.abyssspells.item.AbyssArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AbyssArmorModel extends GeoModel<AbyssArmorItem> {

    @Override
    public ResourceLocation getModelResource(AbyssArmorItem object) {
        return ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "geo/abyss_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AbyssArmorItem object) {
        return ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "textures/models/armor/abyss_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AbyssArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "animations/abyss_armor.animation.json");
    }
}
