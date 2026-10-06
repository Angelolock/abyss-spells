package com.abyssspells.client;

import com.abyssspells.AbyssSpells;
import com.abyssspells.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AbyssSpells.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(VoidShacklesModel.LAYER_LOCATION, VoidShacklesModel::createBodyLayer);
        event.registerLayerDefinition(AbyssCoreModel.LAYER_LOCATION, AbyssCoreModel::createBodyLayer);
        event.registerLayerDefinition(AnchorMarkerModel.LAYER_LOCATION, AnchorMarkerModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VOID_SHACKLES.get(), VoidShacklesRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSS_CORE.get(), AbyssCoreRenderer::new);
        event.registerEntityRenderer(ModEntities.ANCHOR_MARKER.get(), AnchorMarkerRenderer::new);
    }
}
