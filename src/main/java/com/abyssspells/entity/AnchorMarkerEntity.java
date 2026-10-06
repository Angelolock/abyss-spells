package com.abyssspells.entity;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Видимый маркер якоря судьбы: парит на месте снапшота,
 * пока открыто окно возврата. Ни с чем не взаимодействует.
 */
public class AnchorMarkerEntity extends Entity {
    private int age;
    private int duration = 600;
    @Nullable
    private UUID ownerUUID;

    public AnchorMarkerEntity(EntityType<AnchorMarkerEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AnchorMarkerEntity(Level level, LivingEntity owner, int durationTicks) {
        this(com.abyssspells.registry.ModEntities.ANCHOR_MARKER.get(), level);
        this.ownerUUID = owner.getUUID();
        this.duration = durationTicks;
        this.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.duration = tag.getInt("Duration");
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putInt("Duration", duration);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (level().isClientSide) {
            if (age % 6 == 0) {
                level().addParticle(ParticleTypes.SCULK_SOUL,
                        getX(), getY() - 0.3, getZ(), 0, 0.12, 0);
            }
            return;
        }
        if (age >= duration) {
            discard();
        }
    }
}
