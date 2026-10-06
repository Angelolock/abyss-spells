package com.abyssspells.registry;

import com.abyssspells.AbyssSpells;
import com.abyssspells.effect.RetributionEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, AbyssSpells.MODID);

    public static final Supplier<MobEffect> RETRIBUTION = EFFECTS.register("retribution", RetributionEffect::new);

    public static DeferredHolder<MobEffect, MobEffect> holder() {
        return (DeferredHolder<MobEffect, MobEffect>) RETRIBUTION;
    }
}
