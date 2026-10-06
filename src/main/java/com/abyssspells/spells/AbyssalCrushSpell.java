package com.abyssspells.spells;

import com.abyssspells.AbyssSpells;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Давление Бездны.
 * Стягивание (зарядка + имплозия) -> удар в землю: урон по области,
 * дебаффы, слабый отброс. Кастер никогда не затрагивается.
 */
public class AbyssalCrushSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "abyssal_crush");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(25)
            .build();

    public AbyssalCrushSpell() {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 40; // 2 сек зарядки
        this.baseManaCost = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getSpellPower(spellLevel, caster), 1)),
                Component.translatable("ui.abyss_spells.radius", Utils.stringTruncation(getRadius(spellLevel), 1))
        );
    }

    private float getRadius(int spellLevel) {
        return 3.0f + spellLevel; // 4..8
    }

    /** Цели: все живые, кроме самого кастера и его союзников. */
    private List<LivingEntity> findVictims(Level level, LivingEntity caster, float radius, float height) {
        AABB box = caster.getBoundingBox().inflate(radius, height, radius);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && !e.isAlliedTo(caster) && !caster.isAlliedTo(e));
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
        return Optional.of(SoundRegistry.BLACK_HOLE_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    // ---------- Зарядка: стягивание ----------

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (level.isClientSide) return;

        float pullRadius = getRadius(spellLevel) + 3.0f;
        Vec3 center = entity.position().add(0, 1.0, 0);

        for (LivingEntity target : findVictims(level, entity, pullRadius, 3.0f)) {
            // Тяга к центру + легкое притапливание, жертве трудно выйти пешком
            Vec3 pull = center.subtract(target.position().add(0, 0.5, 0)).normalize().scale(0.06);
            target.push(pull.x, -0.02, pull.z);
            target.hurtMarked = true;
            if (target.tickCount % 10 == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 0, false, false, false));
            }
        }

        if (entity.tickCount % 4 == 0) {
            // Сходящаяся спираль
            int steps = 16;
            float progress = chargeProgress(entity, playerMagicData);
            float currentR = pullRadius * (1.0f - progress * 0.6f);
            double y = entity.getY() + 0.2 + progress * 1.2;
            for (int i = 0; i < steps; i++) {
                double angle = (Math.PI * 2 * i) / steps + entity.tickCount * 0.12;
                double x = entity.getX() + Math.cos(angle) * currentR;
                double z = entity.getZ() + Math.sin(angle) * currentR;
                MagicManager.spawnParticles(level, ParticleTypes.SCULK_SOUL,
                        x, y, z, 1,
                        (entity.getX() - x) * 0.08, 0.05, (entity.getZ() - z) * 0.08,
                        0.35, false);
            }
            // Видимый эффект стягивания: сжимающиеся кольца на земле
            spawnRing(level, entity.getX(), entity.getY() + 0.1, entity.getZ(),
                    currentR, ParticleTypes.SCULK_SOUL, 24, 1);
            spawnRing(level, entity.getX(), entity.getY() + 0.1, entity.getZ(),
                    currentR * 0.6f, ParticleTypes.SQUID_INK, 18, 1);
            // Быстрые втягивающиеся streaks
            RandomSource random = level.random;
            for (int i = 0; i < 6; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = pullRadius * (0.7 + random.nextDouble() * 0.5);
                double x = entity.getX() + Math.cos(angle) * r;
                double z = entity.getZ() + Math.sin(angle) * r;
                MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL,
                        x, entity.getY() + 0.4 + random.nextDouble() * 1.6, z, 1,
                        (entity.getX() - x) * 0.25, -0.1, (entity.getZ() - z) * 0.25,
                        0.5, false);
            }
        }

        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    private float chargeProgress(LivingEntity entity, MagicData data) {
        try {
            int remaining = data.getCastDurationRemaining();
            int total = getEffectiveCastTime(5, entity);
            if (total <= 0) return 0.5f;
            return Math.min(1.0f, Math.max(0.0f, 1.0f - (remaining / (float) total)));
        } catch (Exception e) {
            return 0.5f;
        }
    }

    // ---------- Финал: имплозия -> удар -> слабый отброс ----------

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        float radius = getRadius(spellLevel);
        float damage = getSpellPower(spellLevel, entity);
        var damageSource = getDamageSource(entity);
        Vec3 center = entity.position().add(0, 0.5, 0);

        // Фаза 1 — резкая имплозия: всех дергает к центру
        List<LivingEntity> victims = findVictims(level, entity, radius + 2.0f, 2.5f);
        for (LivingEntity target : victims) {
            Vec3 yank = center.subtract(target.position()).normalize().scale(0.45);
            target.push(yank.x, yank.y * 0.3, yank.z);
            target.hurtMarked = true;
        }
        // Сходящиеся streaks в момент удара
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2 * i) / 24;
            double x = entity.getX() + Math.cos(angle) * (radius + 1.5);
            double z = entity.getZ() + Math.sin(angle) * (radius + 1.5);
            MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL,
                    x, entity.getY() + 1.0, z, 1,
                    (entity.getX() - x) * 0.35, -0.25, (entity.getZ() - z) * 0.35,
                    0.6, false);
        }

        // Фаза 2 — урон + дебаффы + слабый отброс от центра
        for (LivingEntity target : victims) {
            if (!target.isAlive()) continue;
            double dist = entity.distanceTo(target);
            float falloff = (float) Math.max(0.35, 1.0 - (dist / (radius * 1.6)));

            target.hurt(damageSource, damage * falloff);

            int debuffTicks = 70 + spellLevel * 25;
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, debuffTicks, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, debuffTicks,
                    Math.min(3, 1 + spellLevel / 2)));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, debuffTicks / 2, 0));
            if (spellLevel >= 4) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            }

            // Слабый отброс: чуть от центра и слегка вверх, далеко не улетают
            Vec3 away = target.position().subtract(center).normalize();
            target.push(away.x * 0.5 * falloff + away.x * 0.15, 0.22, away.z * 0.5 * falloff + away.z * 0.15);
            target.hurtMarked = true;
        }

        // Звуковые слои
        level.playSound(null, entity.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 0.65f);
        level.playSound(null, entity.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.35f, 1.4f);
        level.playSound(null, entity.blockPosition(), SoundEvents.SCULK_SHRIEKER_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);

        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();

        // Тройное расходящееся кольцо
        spawnRing(level, cx, cy + 0.15, cz, radius, ParticleTypes.SCULK_SOUL, 36, 2);
        spawnRing(level, cx, cy + 0.35, cz, radius * 0.66f, ParticleTypes.SCULK_CHARGE_POP, 26, 1);
        spawnRing(level, cx, cy + 0.6, cz, radius * 0.33f, ParticleTypes.SQUID_INK, 18, 2);

        // Вспышка + столб + дождь сгустков
        MagicManager.spawnParticles(level, ParticleTypes.FLASH, cx, cy + 1.0, cz, 1, 0, 0, 0, 0, false);
        MagicManager.spawnParticles(level, ParticleTypes.SONIC_BOOM, cx, cy + 1.0, cz, 2, 0, 0, 0, 0, false);
        for (int i = 0; i < 20; i++) {
            MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL,
                    cx, cy + 0.3 + i * 0.25, cz, 2, 0.25, 0.15, 0.25, 0.06, false);
        }
        RandomSource random = level.random;
        for (int i = 0; i < 30; i++) {
            double ox = (random.nextDouble() - 0.5) * radius * 1.4;
            double oz = (random.nextDouble() - 0.5) * radius * 1.4;
            MagicManager.spawnParticles(level, ParticleTypes.SQUID_INK,
                    cx + ox, cy + 4.5 + random.nextDouble() * 1.5, cz + oz,
                    1, 0, -0.55, 0, 0.2, false);
        }

        // Трещины-лучи по земле
        spawnCracks(level, cx, cy + 0.06, cz, radius + 2.0f, 10 + spellLevel * 2);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnRing(Level level, double cx, double cy, double cz, float radius,
                           net.minecraft.core.particles.ParticleOptions particle, int points, int countPerPoint) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2 * i) / points;
            MagicManager.spawnParticles(level, particle,
                    cx + Math.cos(angle) * radius, cy, cz + Math.sin(angle) * radius,
                    countPerPoint, 0, 0.35, 0, 0.1, false);
        }
    }

    /** Рваные трещины: лучи от центра с дрожанием, темные частицы у самой земли. */
    private void spawnCracks(Level level, double cx, double cy, double cz, float length, int rays) {
        RandomSource random = level.random;
        for (int r = 0; r < rays; r++) {
            double baseAngle = (Math.PI * 2 * r) / rays + (random.nextDouble() - 0.5) * 0.35;
            double jitter = (random.nextDouble() - 0.5) * 0.6;
            for (double t = 0.8; t < length; t += 0.55) {
                double wobble = Math.sin(t * 1.7 + r) * 0.35 + jitter * (t / length);
                double x = cx + Math.cos(baseAngle + wobble * 0.15) * t;
                double z = cz + Math.sin(baseAngle + wobble * 0.15) * t;
                int density = t < length * 0.5 ? 2 : 1;
                MagicManager.spawnParticles(level, ParticleTypes.SQUID_INK,
                        x, cy, z, density, 0.08, 0.02, 0.08, 0.02, false);
                if (random.nextFloat() < 0.25f) {
                    MagicManager.spawnParticles(level, ParticleTypes.SCULK_SOUL,
                            x, cy + 0.15, z, 1, 0, 0.12, 0, 0.05, false);
                }
                if (t > length * 0.7 && random.nextFloat() < 0.2f) {
                    MagicManager.spawnParticles(level, ParticleTypes.LARGE_SMOKE,
                            x, cy + 0.25, z, 1, 0, 0.1, 0, 0.04, false);
                }
            }
        }
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        if (level.isClientSide) {
            float radius = getRadius(spellLevel);
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                level.addParticle(ParticleTypes.PORTAL,
                        entity.getX() + Math.cos(angle) * radius * 0.7,
                        entity.getY() + 1.0,
                        entity.getZ() + Math.sin(angle) * radius * 0.7,
                        0, 0.4, 0);
            }
        }
        super.onClientCast(level, spellLevel, entity, castData);
    }
}
