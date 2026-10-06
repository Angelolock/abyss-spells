package com.abyssspells.client;

import com.abyssspells.item.AbyssArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class AbyssArmorRenderer extends GeoArmorRenderer<AbyssArmorItem> {
    public AbyssArmorRenderer() {
        super(new AbyssArmorModel());
    }
}
