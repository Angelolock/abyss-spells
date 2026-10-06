package com.abyssspells.spells;

import com.abyssspells.AbyssSpells;
import com.abyssspells.registry.AbyssSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Отложенная кара: метка на цель, затем 1/2/3 взрыва с интервалом 5 секунд.
 * Взрывы наводятся на текущую позицию жертвы.
 */
public class DoomMarkSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "doom_mark");

    private static final long BLAST_INTERVAL_TICKS = 120; // 6 сек

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(50)
            .build();

    public DoomMarkSpell() {
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 40; // 2 сек чтения
        this.baseManaCost = 60;
    }

    private float getBlastDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster); // 8 / 12 / 16 с силой школы
    }

    private float getBlastRadius(int spellLevel) {
        return 3 + spellLevel; // 4 / 5 / 6
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.abyss_spells.blasts", spellLevel),
                Component.translatable("ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getBlastDamage(spellLevel, caster), 1)),
                Component.translatable("ui.abyss_spells.blast_interval", 6)
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
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
        return Optional.of(SoundRegistry.DARK_SPELL_02.get());
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.ELDRITCH_PREPARE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_ANIMATION;
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (level.isClientSide) {
            return;
        }
        // Нагнетание кары
        if (entity.tickCount % 4 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8 - entity.tickCount * 0.15;
                MagicManager.spawnParticles(level, ParticleTypes.SQUID_INK,
                        entity.getX() + Math.cos(angle) * 1.5, entity.getY() + 1.2, entity.getZ() + Math.sin(angle) * 1.5,
                        1, -Math.cos(angle) * 0.25, 0.15, -Math.sin(angle) * 0.25, 0.2, false);
            }
        }
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 20, .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(level instanceof ServerLevel serverLevel)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        if (!(playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targeting)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        LivingEntity target = targeting.getTarget(serverLevel);
        if (target == null || !target.isAlive() || target == entity) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        float damage = getBlastDamage(spellLevel, entity);
        float radius = getBlastRadius(spellLevel);
        // Взрывы на +6с, +12с, +18с по уровню (метка = spellLevel взрывов)
        for (int i = 1; i <= spellLevel; i++) {
            DoomScheduler.schedule(serverLevel, target, entity, spellLevel, damage, radius,
                    BLAST_INTERVAL_TICKS * i);
        }

        // Печать метки: кольцо + столб
        double x = target.getX(), y = target.getY(), z = target.getZ();
        for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2 * i) / 20;
            MagicManager.spawnParticles(level, ParticleTypes.SQUID_INK,
                    x + Math.cos(angle) * 1.2, y + 0.1, z + Math.sin(angle) * 1.2,
                    1, 0, 0.3, 0, 0.05, false);
        }
        MagicManager.spawnParticles(level, ParticleTypes.WHITE_ASH,
                x, y + 1.0, z, 8, 0.3, 0.6, 0.3, 0.1, false);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
