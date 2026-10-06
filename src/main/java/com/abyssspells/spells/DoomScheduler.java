package com.abyssspells.spells;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Планировщик отложенных взрывов Метки кары.
 * Взрывы идут по серверным тикам, каждый наводится на ТЕКУЩУЮ позицию жертвы.
 */
@EventBusSubscriber
public class DoomScheduler {
    private record PendingBlast(ResourceKey<Level> dim, UUID target, UUID caster,
                                int spellLevel, float damage, float radius, long dueTick) {
    }

    private static final List<PendingBlast> QUEUE = new ArrayList<>();
    private record PendingKill(ResourceKey<Level> dim, UUID target, long dueTick) {
    }

    private static final List<PendingKill> KILL_QUEUE = new ArrayList<>();

    /** Отложенное убийство жертвы Подношения (с лутом, через обычный урон). */
    public static void scheduleKill(ServerLevel level, LivingEntity target, long delayTicks) {
        KILL_QUEUE.add(new PendingKill(level.dimension(), target.getUUID(),
                level.getGameTime() + delayTicks));
    }

    public static void schedule(ServerLevel level, LivingEntity target, LivingEntity caster,
                                int spellLevel, float damage, float radius, long delayTicks) {
        QUEUE.add(new PendingBlast(level.dimension(), target.getUUID(),
                caster.getUUID(), spellLevel, damage, radius,
                level.getGameTime() + delayTicks));
        // Метка видна: свечение до последнего взрыва
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                (int) (delayTicks + 20), 0, false, false, false));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (QUEUE.isEmpty() && KILL_QUEUE.isEmpty()) {
            return;
        }
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        long now = server.overworld().getGameTime();
        tickBlasts(server, now);
        tickKills(server, now);
    }

    private static void tickKills(MinecraftServer server, long now) {
        Iterator<PendingKill> it = KILL_QUEUE.iterator();
        while (it.hasNext()) {
            PendingKill kill = it.next();
            if (now - kill.dueTick() > 1200) {
                it.remove();
                continue;
            }
            if (now < kill.dueTick()) {
                continue;
            }
            it.remove();
            ServerLevel level = server.getLevel(kill.dim());
            if (level == null) {
                continue;
            }
            if (!(level.getEntity(kill.target()) instanceof LivingEntity target) || !target.isAlive()) {
                continue;
            }
            Vec3 p = target.position().add(0, 1.0, 0);
            target.kill();
            MagicManager.spawnParticles(level, ParticleTypes.FLASH, p.x, p.y, p.z, 2, 0, 0, 0, 0, false);
            MagicManager.spawnParticles(level, ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 30, 1.0, 1.5, 1.0, 0.15, false);
            MagicManager.spawnParticles(level, ParticleTypes.SQUID_INK, p.x, p.y, p.z, 30, 1.0, 1.0, 1.0, 0.2, false);
            level.playSound(null, target.blockPosition(),
                    net.minecraft.sounds.SoundEvents.WITHER_DEATH, net.minecraft.sounds.SoundSource.PLAYERS,
                    0.8f, 0.5f);
        }
    }

    private static void tickBlasts(MinecraftServer server, long now) {
        Iterator<PendingBlast> it = QUEUE.iterator();
        while (it.hasNext()) {
            PendingBlast blast = it.next();
            if (now - blast.dueTick() > 1200) {
                it.remove(); // протухшая метка (рестарт/выгрузка) — выкинуть
                continue;
            }
            if (now < blast.dueTick()) {
                continue;
            }
            it.remove();
            ServerLevel level = server.getLevel(blast.dim());
            if (level == null) {
                continue;
            }
            if (!(level.getEntity(blast.target()) instanceof LivingEntity target) || !target.isAlive()) {
                continue; // жертва ушла — кара рассеялась
            }
            detonate(level, target, blast);
        }
    }

    private static void detonate(ServerLevel level, LivingEntity target, PendingBlast blast) {
        LivingEntity caster = level.getEntity(blast.caster()) instanceof LivingEntity living ? living : null;
        DamageSource source = (caster != null)
                ? level.damageSources().source(
                        com.abyssspells.registry.AbyssSchools.ABYSS_DAMAGE_KEY, caster)
                : level.damageSources().magic();

        Vec3 center = target.position().add(0, 0.5, 0);
        AABB box = target.getBoundingBox().inflate(blast.radius(), 2.0, blast.radius());
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (victim == caster || (caster != null && (victim.isAlliedTo(caster) || caster.isAlliedTo(victim)))) {
                continue;
            }
            double dist = victim.position().distanceTo(center);
            float falloff = (float) Math.max(0.3, 1.0 - (dist / (blast.radius() * 1.5)));
            victim.hurt(source, blast.damage() * falloff);
            victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            Vec3 away = victim.position().subtract(center).normalize();
            victim.push(away.x * 0.5, 0.35, away.z * 0.5);
            victim.hurtMarked = true;
        }

        double x = target.getX(), y = target.getY() + 0.5, z = target.getZ();
        MagicManager.spawnParticles(level, ParticleTypes.FLASH, x, y, z, 2, 0, 0, 0, 0, false);
        MagicManager.spawnParticles(level, ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 24, 1.2, 1.2, 1.2, 0.15, false);
        MagicManager.spawnParticles(level, ParticleTypes.SQUID_INK, x, y, z, 20, 1.0, 1.0, 1.0, 0.2, false);
        MagicManager.spawnParticles(level, ParticleTypes.SONIC_BOOM, x, y + 0.5, z, 1, 0, 0, 0, 0, false);
        level.playSound(null, target.blockPosition(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS,
                0.8f, 0.6f);
    }
}
