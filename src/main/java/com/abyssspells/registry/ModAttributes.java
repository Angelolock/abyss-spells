package com.abyssspells.registry;

import com.abyssspells.AbyssSpells;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = AbyssSpells.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, AbyssSpells.MODID);

    public static final DeferredHolder<Attribute, Attribute> ABYSS_SPELL_POWER = ATTRIBUTES.register(
            "abyss_spell_power",
            () -> new MagicPercentAttribute("attribute.abyss_spells.abyss_spell_power", 1.0D, -100.0D, 100.0D).setSyncable(true)
    );

    public static final DeferredHolder<Attribute, Attribute> ABYSS_MAGIC_RESIST = ATTRIBUTES.register(
            "abyss_magic_resist",
            () -> new MagicPercentAttribute("attribute.abyss_spells.abyss_magic_resist", 1.0D, -100.0D, 100.0D).setSyncable(true)
    );

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent e) {
        // Атрибуты школы нужны игрокам (как делает сам Irons)
        e.getTypes().forEach(type -> {
            if (type == EntityType.PLAYER) {
                e.add(type, ABYSS_SPELL_POWER);
                e.add(type, ABYSS_MAGIC_RESIST);
            }
        });
    }
}
