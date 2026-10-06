package com.abyssspells.spells;

import com.abyssspells.AbyssSpells;
import com.abyssspells.registry.AbyssSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
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
 * Перестановка: кастер и цель мгновенно меняются местами.
 * Цель получает короткую тьму (дезориентация).
 */
public class TranspositionSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "transposition");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(25)
            .build();

    public TranspositionSpell() {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 20; // 1 сек чтения
        this.baseManaCost = 50;
    }

    private int getRange(int spellLevel) {
        return 20; // всегда 20, уровень один
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel), 1))
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
        return Optional.of(SoundRegistry.ABYSSAL_TELEPORT.get());
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
        // Стягивание тьмы перед обменом
        if (entity.tickCount % 4 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8 + entity.tickCount * 0.2;
                MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL,
                        entity.getX() + Math.cos(angle) * 1.5, entity.getY() + 1.0, entity.getZ() + Math.sin(angle) * 1.5,
                        1, -Math.cos(angle) * 0.25, 0.1, -Math.sin(angle) * 0.25, 0.15, false);
            }
        }
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, getRange(spellLevel), .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(level instanceof ServerLevel)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        if (!(playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targeting)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        LivingEntity target = targeting.getTarget((ServerLevel) level);
        if (target == null || !target.isAlive() || target == entity) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        Vec3 casterPos = entity.position();
        Vec3 targetPos = target.position();
        float casterYaw = entity.getYRot();
        float casterPitch = entity.getXRot();
        float targetYaw = target.getYRot();
        float targetPitch = target.getXRot();

        // Снять с маунтов, чтобы не тащить лошадей через бездну
        entity.stopRiding();
        target.stopRiding();

        if (Utils.handleSpellTeleport(this, entity, targetPos)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return; // другой мод отменил телепорт — не меняем ничего
        }
        entity.setYRot(targetYaw);
        entity.setXRot(targetPitch);
        target.teleportTo(casterPos.x, casterPos.y, casterPos.z);
        target.setYRot(casterYaw);
        target.setXRot(casterPitch);

        // Дезориентация жертвы
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));

        burst(level, casterPos);
        burst(level, targetPos);
        level.playSound(null, entity.blockPosition(), SoundRegistry.ABYSSAL_TELEPORT.get(), SoundSource.PLAYERS, 0.9f, 0.9f);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void burst(Level level, Vec3 pos) {
        MagicManager.spawnParticles(level, ParticleTypes.SCULK_SOUL, pos.x, pos.y + 1.0, pos.z, 14, 0.5, 0.8, 0.5, 0.12, false);
        MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1.0, pos.z, 8, 0.4, 0.6, 0.4, 0.08, false);
    }
}
