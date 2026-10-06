package com.abyssspells.entity;

import com.abyssspells.registry.AbyssSchools;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Ядро бездны: медленно дрейфует по прямой сквозь блоки,
 * каждые 2 секунды — 2-секундное мягкое втяжение.
 * Чем ближе сущность, тем больше урон + тьма.
 */
public class AbyssCoreEntity extends Entity {
    private static final int LIFETIME_TICKS = 400;      // 20 сек
    private static final int PULSE_INTERVAL_TICKS = 40; // втяг каждые 2 сек
    private static final int SUCTION_TICKS = 40;        // окно втяжения 2 сек
    private static final float RADIUS = 8.0f;
    private static final float MAX_DAMAGE = 10.0f;
    private static final double SPEED = 0.09;

    private static final EntityDataAccessor<org.joml.Vector3f> DATA_DIRECTION =
            SynchedEntityData.defineId(AbyssCoreEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> DATA_FROZEN =
            SynchedEntityData.defineId(AbyssCoreEntity.class, EntityDataSerializers.BOOLEAN);

    private Vec3 direction = Vec3.ZERO;
    private int age;
    private int suctionLeft;
    @Nullable
    private UUID ownerUUID;

    public AbyssCoreEntity(EntityType<AbyssCoreEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AbyssCoreEntity(Level level, LivingEntity owner, Vec3 direction) {
        this(com.abyssspells.registry.ModEntities.ABYSS_CORE.get(), level);
        this.direction = direction.normalize();
        org.joml.Vector3f d = this.direction.toVector3f();
        this.entityData.set(DATA_DIRECTION, d);
        this.ownerUUID = owner.getUUID();
        this.setPos(owner.getX() + direction.x, owner.getEyeY() - 0.3 + direction.y, owner.getZ() + direction.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DIRECTION, new org.joml.Vector3f(0, 0, 0));
        builder.define(DATA_FROZEN, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.direction = new Vec3(tag.getDouble("dx"), tag.getDouble("dy"), tag.getDouble("dz"));
        this.entityData.set(DATA_DIRECTION, this.direction.toVector3f());
        this.age = tag.getInt("Age");
        this.suctionLeft = tag.getInt("Suction");
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        this.entityData.set(DATA_FROZEN, tag.getBoolean("Frozen"));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("dx", direction.x);
        tag.putDouble("dy", direction.y);
        tag.putDouble("dz", direction.z);
        tag.putInt("Age", age);
        tag.putInt("Suction", suctionLeft);
        tag.putBoolean("Frozen", isFrozen());
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    public boolean isFrozen() {
        return this.entityData.get(DATA_FROZEN);
    }

    public void setFrozen(boolean frozen) {
        this.entityData.set(DATA_FROZEN, frozen);
        if (frozen && !level().isClientSide) {
            Vec3 pos = position();
            MagicManager.spawnParticles(level(), ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, false);
            MagicManager.spawnParticles(level(), ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 24, 1.5, 1.5, 1.5, 0.1, false);
            level().playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS,
                    0.7f, 0.6f);
        }
    }

    @Nullable
    private LivingEntity resolveOwner() {
        if (!level().isClientSide && ownerUUID != null && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (serverLevel.getEntity(ownerUUID) instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private DamageSource damageSource() {
        LivingEntity owner = resolveOwner();
        if (owner != null) {
            return level().damageSources().source(AbyssSchools.ABYSS_DAMAGE_KEY, owner);
        }
        return level().damageSources().magic();
    }

    private boolean isVictim(LivingEntity e) {
        if (!e.isAlive()) {
            return false;
        }
        LivingEntity owner = resolveOwner();
        if (owner != null && (e == owner || e.isAlliedTo(owner))) {
            return false;
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        Vec3 pos = position();
        if (level().isClientSide) {
            // Клиент НЕ двигает: позицию присылает сервер, ванилла плавно интерполирует.
            // Иначе свое движение борется с серверным — дерганье.
            // Шлейф на клиенте
            level().addParticle(ParticleTypes.SCULK_SOUL,
                    pos.x, pos.y, pos.z, 0, 0.05, 0);
            if (random.nextFloat() < 0.3f) {
                level().addParticle(ParticleTypes.REVERSE_PORTAL,
                        pos.x, pos.y, pos.z, 0, 0.1, 0);
            }
            return;
        }

        if (age >= LIFETIME_TICKS) {
            MagicManager.spawnParticles(level(), ParticleTypes.SQUID_INK,
                    pos.x, pos.y, pos.z, 20, 0.8, 0.8, 0.8, 0.2, false);
            discard();
            return;
        }

        // Дрейф сквозь блоки (замороженное стоит)
        if (!isFrozen()) {
            setPos(pos.add(direction.scale(SPEED)));
        }

        if (suctionLeft > 0) {
            suctionLeft--;
            doSuctionTick();
        } else if (age % PULSE_INTERVAL_TICKS == 0) {
            suctionLeft = SUCTION_TICKS;
            // Видимый старт втяга: вспышка + сходящееся кольцо
            MagicManager.spawnParticles(level(), ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, false);
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                double rx = pos.x + Math.cos(angle) * RADIUS;
                double rz = pos.z + Math.sin(angle) * RADIUS;
                MagicManager.spawnParticles(level(), ParticleTypes.REVERSE_PORTAL,
                        rx, pos.y, rz, 1,
                        (pos.x - rx) * 0.3, 0.05, (pos.z - rz) * 0.3, 0.4, false);
            }
            level().playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.3f);
        }

        // Постоянный шлейф (сервер, разлетится клиентам)
        if (age % 3 == 0) {
            MagicManager.spawnParticles(level(), ParticleTypes.PORTAL,
                    pos.x, pos.y, pos.z, 1, 0.2, 0.2, 0.2, 0.05, false);
        }
    }

    /** Мягкое втяжение: маленькая тяга каждый тик + урон/тьма раз в 20 тиков. */
    private void doSuctionTick() {
        Vec3 pos = position();
        // Честный объемный куб: +-8 по горизонтали, +-5 по вертикали
        AABB box = new AABB(pos.x - RADIUS, pos.y - 5.0, pos.z - RADIUS,
                pos.x + RADIUS, pos.y + 5.0, pos.z + RADIUS);
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, box, this::isVictim);
        for (LivingEntity target : targets) {
            Vec3 toCore = pos.subtract(target.position().add(0, 0.5, 0));
            double dist = toCore.length();
            if (dist > RADIUS || dist < 0.001) {
                continue;
            }
            // Постепенная тяга к ядру (не рывок)
            Vec3 pull = toCore.normalize().scale(0.20);
            target.push(pull.x, pull.y * 0.25, pull.z);
            target.hurtMarked = true;

            // Урон по близости: чем ближе, тем больнее
            if (age % 20 == 0) {
                float closeness = (float) (1.0 - dist / RADIUS);
                target.hurt(damageSource(), MAX_DAMAGE * closeness);
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0));
            }

            // Сходящиеся streaks втяга — чаще и гуще
            if (age % 2 == 0) {
                Vec3 tp = target.position().add(0, 1.0, 0);
                MagicManager.spawnParticles(level(), ParticleTypes.SCULK_SOUL,
                        tp.x, tp.y, tp.z, 2,
                        (pos.x - tp.x) * 0.15, 0.1, (pos.z - tp.z) * 0.15, 0.3, false);
                MagicManager.spawnParticles(level(), ParticleTypes.WHITE_ASH,
                        tp.x, tp.y + 0.5, tp.z, 1,
                        (pos.x - tp.x) * 0.1, 0.05, (pos.z - tp.z) * 0.1, 0.2, false);
            }
        }
    }
}
