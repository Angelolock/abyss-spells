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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Равновесие бездны: кастер и цель меняются ПРОЦЕНТОМ здоровья.
 * На цель вешается тьма на 10 секунд. Один уровень — как задумано.
 */
public class EquilibriumSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "equilibrium");

    private static final int DARKNESS_TICKS = 200; // 10 сек

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(60)
            .build();

    public EquilibriumSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 30; // 1.5 сек наведения
        this.baseManaCost = 120;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.abyss_spells.equilibrium"),
                Component.translatable("ui.abyss_spells.darkness_10s")
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.ABYSSAL_SHROUD.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.ABYSSAL_TELEPORT.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_RAISED_HAND;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ONE_HANDED_RAY_SHOOT;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 16, .35f);
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

        // Обмен процентами здоровья
        float casterFrac = entity.getHealth() / entity.getMaxHealth();
        float targetFrac = target.getHealth() / target.getMaxHealth();
        entity.setHealth(Math.max(1.0f, entity.getMaxHealth() * targetFrac));
        target.setHealth(Math.max(1.0f, target.getMaxHealth() * casterFrac));

        // Тьма на цель
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS, 0));

        // Мост обмена: линия частиц между кастером и целью
        Vec3 from = entity.position().add(0, 1.2, 0);
        Vec3 to = target.position().add(0, 1.2, 0);
        Utils.particleTrail(level, from, to, ParticleTypes.SCULK_SOUL);
        Utils.particleTrail(level, to, from, ParticleTypes.WHITE_ASH);
        Vec3 mid = from.add(to.subtract(from).scale(0.5));
        MagicManager.spawnParticles(level, ParticleTypes.FLASH, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0, false);
        MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL,
                to.x, to.y, to.z, 8, 0.3, 0.5, 0.3, 0.08, false);

        level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.8f, 1.0f);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
