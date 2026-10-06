package com.abyssspells.spells;

import com.abyssspells.AbyssSpells;
import com.abyssspells.entity.AbyssCoreEntity;
import com.abyssspells.registry.AbyssSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

/**
 * Ядро бездны: LONG-каст запускает дрейфующий шар (втяг каждые 2 сек).
 * Второе использование замораживает сферу на месте — она продолжает тянуть.
 */
public class AbyssCoreSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "abyss_core");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(60)
            .build();

    public AbyssCoreSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 60; // 3 сек чтения
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.abyss_spells.pulses", 9),
                Component.translatable("ui.abyss_spells.core_damage", Utils.stringTruncation(10.0, 1)),
                Component.translatable("ui.abyss_spells.core_drift", 20),
                Component.translatable("ui.abyss_spells.core_freeze")
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
        return Optional.of(SoundRegistry.BLACK_HOLE_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_ANIMATION;
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2; // запуск + заморозка
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @org.jetbrains.annotations.Nullable LivingEntity entity) {
        // Второе нажатие (заморозка) — мгновенно, без повторного чтения
        if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer && !entity.level().isClientSide) {
            if (MagicData.getPlayerMagicData(serverPlayer).getPlayerRecasts().hasRecastForSpell(getSpellId())) {
                return 0;
            }
        }
        return super.getEffectiveCastTime(spellLevel, entity);
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new CoreFreezeData();
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (level.isClientSide) {
            return;
        }
        // Сгущение перед запуском
        if (entity.tickCount % 4 == 0) {
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10 + entity.tickCount * 0.15;
                double r = 2.5 - (entity.tickCount % 20) * 0.08;
                MagicManager.spawnParticles(level, ParticleTypes.SCULK_SOUL,
                        entity.getX() + Math.cos(angle) * r, entity.getY() + 1.0, entity.getZ() + Math.sin(angle) * r,
                        1, -Math.cos(angle) * 0.2, 0.1, -Math.sin(angle) * 0.2, 0.2, false);
            }
        }
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        // Второе использование — заморозить ближайшее свое незамороженное ядро
        if (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
            freezeNearest(level, entity);
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        AbyssCoreEntity core = new AbyssCoreEntity(level, entity, entity.getLookAngle());
        level.addFreshEntity(core);
        var recast = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity),
                400, castSource, new CoreFreezeData());
        playerMagicData.getPlayerRecasts().addRecast(recast, playerMagicData);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void freezeNearest(Level level, LivingEntity caster) {
        AABB box = caster.getBoundingBox().inflate(48.0);
        AbyssCoreEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (AbyssCoreEntity core : level.getEntitiesOfClass(AbyssCoreEntity.class, box, c -> !c.isFrozen())) {
            if (caster.getUUID().equals(core.getOwnerUUID())) {
                double d = caster.distanceToSqr(core);
                if (d < bestDist) {
                    bestDist = d;
                    best = core;
                }
            }
        }
        if (best != null) {
            best.setFrozen(true);
            if (caster instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.abyss_spells.core_frozen")));
            }
        }
    }

    /** Пустые данные рекаста: состояние хранит сама сущность. */
    public static class CoreFreezeData implements ICastDataSerializable {
        @Override
        public void reset() {
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        }
    }
}
