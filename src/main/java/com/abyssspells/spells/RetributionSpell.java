package com.abyssspells.spells;

import com.abyssspells.AbyssSpells;
import com.abyssspells.effect.RetributionTracker;
import com.abyssspells.registry.AbyssSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Возмездие: 5 секунд копит 80% входящего урона (до капа),
 * затем выпускает волной вокруг.
 */
public class RetributionSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "retribution");

    private static final int STORE_TICKS = 100; // 5 сек

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(60)
            .build();

    public RetributionSpell() {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    private float getCap(int spellLevel) {
        return 60 + spellLevel * 20; // 80 / 100 / 120
    }

    private float getRadius(int spellLevel) {
        return 5 + spellLevel; // 6 / 7 / 8
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.abyss_spells.retribution_store", 80),
                Component.translatable("ui.abyss_spells.retribution_cap", Utils.stringTruncation(getCap(spellLevel), 0)),
                Component.translatable("ui.abyss_spells.radius", Utils.stringTruncation(getRadius(spellLevel), 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.DARK_MAGIC_BUFF_03_CUSTOM_1.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_ANIMATION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        RetributionTracker.ignite(entity, STORE_TICKS, getCap(spellLevel), getRadius(spellLevel));
        MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL,
                entity.getX(), entity.getY() + 1.0, entity.getZ(), 12, 0.5, 0.7, 0.5, 0.1, false);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
