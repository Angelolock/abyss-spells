package com.abyssspells.spells;

import com.abyssspells.AbyssSpells;
import com.abyssspells.entity.VoidShacklesEntity;
import com.abyssspells.registry.AbyssSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Подношение Бездне: 10 секунд чтения, щупальце утаскивает жертву.
 * Обычные сущности (до 150 макс. HP) умирают мгновенно.
 * Игроки и боссы вместо смерти получают 50% текущего HP чистым уроном.
 */
public class OfferingSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "abyss_offering");

    private static final float EXECUTE_MAX_HEALTH = 150.0f;
    private static final int SHACKLE_TICKS = 80;
    private static final int KILL_DELAY_TICKS = 40;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(300)
            .build();

    public OfferingSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 200; // 10 сек чтения
        this.baseManaCost = 800;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.abyss_spells.offering_kill"),
                Component.translatable("ui.abyss_spells.offering_mercy")
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
        return Optional.of(SoundRegistry.VOID_TENTACLES_START.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VOID_TENTACLES_FINISH.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.PREPARE_CROSS_ARMS;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 20, .35f);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (level.isClientSide) {
            return;
        }
        // 10 секунд нагнетания: тьма сгущается
        if (entity.tickCount % 4 == 0) {
            float progress = chargeProgress(entity, playerMagicData);
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10 - entity.tickCount * 0.1;
                double r = 3.0 - progress * 1.5;
                MagicManager.spawnParticles(level, ParticleTypes.SCULK_SOUL,
                        entity.getX() + Math.cos(angle) * r, entity.getY() + 0.3 + progress, entity.getZ() + Math.sin(angle) * r,
                        1, 0, 0.2, 0, 0.1, false);
            }
        }
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    private float chargeProgress(LivingEntity entity, MagicData data) {
        try {
            int remaining = data.getCastDurationRemaining();
            int total = getEffectiveCastTime(1, entity);
            if (total <= 0) return 0.5f;
            return Math.min(1.0f, Math.max(0.0f, 1.0f - (remaining / (float) total)));
        } catch (Exception e) {
            return 0.5f;
        }
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

        // Щупальце хватает жертву
        if (!target.getType().is(ModTags.CANT_ROOT)) {
            VoidShacklesEntity shackles = new VoidShacklesEntity(level, entity);
            shackles.setDuration(SHACKLE_TICKS);
            shackles.setTarget(target);
            shackles.moveTo(target.position());
            shackles.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);
            shackles.setHealth(60);
            level.addFreshEntity(shackles);
            target.stopRiding();
            target.startRiding(shackles, true);
        }
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, SHACKLE_TICKS + 40, 0));

        boolean executing = !(target instanceof Player) && target.getMaxHealth() <= EXECUTE_MAX_HEALTH;
        if (executing) {
            DoomScheduler.scheduleKill(serverLevel, target, KILL_DELAY_TICKS);
        } else {
            // Пощада для игроков и боссов: половина текущего HP чистым уроном
            float mercy = target.getHealth() * 0.5f;
            if (mercy > 0.5f) {
                target.hurt(level.damageSources().source(AbyssSchools.VOID_CLAIM_DAMAGE_KEY, entity), mercy);
            }
        }

        double x = target.getX(), y = target.getY() + 1.0, z = target.getZ();
        MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL, x, y, z, 16, 0.5, 0.8, 0.5, 0.1, false);
        MagicManager.spawnParticles(level, ParticleTypes.SQUID_INK, x, y, z, 16, 0.5, 0.5, 0.5, 0.15, false);
        level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.9f, 0.5f);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
