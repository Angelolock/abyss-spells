package com.abyssspells.effect;

import com.abyssspells.registry.AbyssSchools;
import com.abyssspells.registry.ModMobEffects;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Копит входящий урон носителя Знака (часть гасится),
 * по истечении — взрыв волной вокруг.
 */
@EventBusSubscriber
public class RetributionTracker {
    private record Charge(UUID entity, ResourceKey<Level> dim, float stored, float cap,
                          float radius, long expiresTick) {
        Charge add(float amount) {
            return new Charge(entity, dim, Math.min(cap, stored + amount), cap, radius, expiresTick);
        }
    }

    private static final Map<UUID, Charge> CHARGES = new HashMap<>();

    public static void ignite(LivingEntity caster, int durationTicks, float cap, float radius) {
        Holder<MobEffect> effect = ModMobEffects.holder();
        caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, durationTicks, 0, false, false, true));
        CHARGES.put(caster.getUUID(), new Charge(caster.getUUID(),
                caster.level().dimension(), 0f, cap, radius,
                caster.level().getGameTime() + durationTicks));
    }

    public static float storedFor(LivingEntity entity) {
        Charge c = CHARGES.get(entity.getUUID());
        return c == null ? 0f : c.stored();
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        Charge charge = CHARGES.get(victim.getUUID());
        if (charge == null) {
            return;
        }
        if (!victim.hasEffect(ModMobEffects.holder())) {
            CHARGES.remove(victim.getUUID());
            return;
        }
        // Гасим 80% входящего, кладем в накопление
        float incoming = event.getNewDamage();
        float absorbed = incoming * 0.8f;
        event.setNewDamage(incoming - absorbed);
        CHARGES.put(victim.getUUID(), charge.add(absorbed));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (CHARGES.isEmpty()) {
            return;
        }
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Charge>> it = CHARGES.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Charge charge = entry.getValue();
            ServerLevel level = server.getLevel(charge.dim());
            if (level == null || now - charge.expiresTick() > 1200) {
                it.remove();
                continue;
            }
            if (now < charge.expiresTick()) {
                continue;
            }
            it.remove();
            if (!(level.getEntity(charge.entity()) instanceof LivingEntity caster) || !caster.isAlive()) {
                continue; // погиб до разрядки — бездна забирает молча
            }
            release(level, caster, charge);
        }
    }

    private static void release(ServerLevel level, LivingEntity caster, Charge charge) {
        if (charge.stored() < 1f) {
            MagicManager.spawnParticles(level, ParticleTypes.SMOKE, caster.getX(), caster.getY() + 1.0, caster.getZ(),
                    8, 0.5, 0.5, 0.5, 0.05, false);
            return; // нечего выпускать
        }
        DamageSource source = level.damageSources().source(AbyssSchools.ABYSS_DAMAGE_KEY, caster);
        Vec3 center = caster.position().add(0, 0.5, 0);
        AABB box = caster.getBoundingBox().inflate(charge.radius(), 2.5, charge.radius());
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && !e.isAlliedTo(caster))) {
            double dist = victim.position().distanceTo(center);
            float falloff = (float) Math.max(0.3, 1.0 - (dist / (charge.radius() * 1.5)));
            victim.hurt(source, charge.stored() * falloff);
            Vec3 away = victim.position().subtract(center).normalize();
            victim.push(away.x * 0.8, 0.35, away.z * 0.8);
            victim.hurtMarked = true;
        }

        double x = caster.getX(), y = caster.getY() + 1.0, z = caster.getZ();
        MagicManager.spawnParticles(level, ParticleTypes.FLASH, x, y, z, 2, 0, 0, 0, 0, false);
        for (int ring = 0; ring < 3; ring++) {
            float r = charge.radius() * (0.4f + ring * 0.3f);
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                MagicManager.spawnParticles(level, ParticleTypes.SCULK_SOUL,
                        x + Math.cos(angle) * r, y - 0.5 + ring * 0.4, z + Math.sin(angle) * r,
                        1, 0, 0.25, 0, 0.08, false);
            }
        }
        level.playSound(null, caster.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.7f);
    }
}
