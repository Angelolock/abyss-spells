package com.abyssspells.registry;

import com.abyssspells.AbyssSpells;
import com.abyssspells.entity.AbyssCoreEntity;
import com.abyssspells.entity.AnchorMarkerEntity;
import com.abyssspells.entity.VoidShacklesEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@EventBusSubscriber(modid = AbyssSpells.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AbyssSpells.MODID);

    public static final Supplier<EntityType<VoidShacklesEntity>> VOID_SHACKLES = ENTITY_TYPES.register("void_shackles",
            () -> EntityType.Builder.<VoidShacklesEntity>of(VoidShacklesEntity::new, MobCategory.MISC)
                    .sized(1.4f, 0.7f)
                    .clientTrackingRange(10)
                    .build("void_shackles"));

    public static final Supplier<EntityType<AbyssCoreEntity>> ABYSS_CORE = ENTITY_TYPES.register("abyss_core",
            () -> EntityType.Builder.<AbyssCoreEntity>of(AbyssCoreEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .fireImmune()
                    .build("abyss_core"));

    public static final Supplier<EntityType<AnchorMarkerEntity>> ANCHOR_MARKER = ENTITY_TYPES.register("anchor_marker",
            () -> EntityType.Builder.<AnchorMarkerEntity>of(AnchorMarkerEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.2f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .fireImmune()
                    .build("anchor_marker"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(VOID_SHACKLES.get(), LivingEntity.createLivingAttributes().build());
    }
}
