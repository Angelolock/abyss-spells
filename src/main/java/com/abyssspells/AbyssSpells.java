package com.abyssspells;

import com.abyssspells.registry.AbyssSchools;
import com.abyssspells.registry.ModAttributes;
import com.abyssspells.registry.ModCreativeTabs;
import com.abyssspells.registry.ModEntities;
import com.abyssspells.registry.ModItems;
import com.abyssspells.registry.ModMobEffects;
import com.abyssspells.registry.ModSpells;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(AbyssSpells.MODID)
public class AbyssSpells {
    public static final String MODID = "abyss_spells";

    public AbyssSpells(IEventBus modEventBus) {
        ModAttributes.ATTRIBUTES.register(modEventBus);
        AbyssSchools.SCHOOLS.register(modEventBus);
        ModSpells.SPELLS.register(modEventBus);
        ModMobEffects.EFFECTS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }
}
