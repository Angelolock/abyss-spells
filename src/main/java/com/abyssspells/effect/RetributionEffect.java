package com.abyssspells.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Знак возмездия: сама иконка-бафф, логика в RetributionTracker.
 */
public class RetributionEffect extends MobEffect {
    public RetributionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x1a1a2e);
    }
}
