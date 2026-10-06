package com.abyssspells.entity;

import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Оковы Пустоты — ч/б щупальца.
 * Вся логика оков (езда жертвы, таймер, ломание уроном, NBT) унаследована от RootEntity,
 * здесь только свой тип и звуки. Внешность — свой рендер (см. client).
 */
public class VoidShacklesEntity extends RootEntity {

    public VoidShacklesEntity(EntityType<VoidShacklesEntity> entityType, Level level) {
        super(entityType, level);
    }

    public VoidShacklesEntity(Level level, LivingEntity owner) {
        this(com.abyssspells.registry.ModEntities.VOID_SHACKLES.get(), level);
        setOwner(owner);
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundRegistry.VOID_TENTACLES_AMBIENT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundRegistry.VOID_TENTACLES_LEAVE.get();
    }
}
