package com.abyssspells.spells;

import com.abyssspells.AbyssSpells;
import com.abyssspells.registry.AbyssSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/**
 * Цена глубин: бездна берет здоровьем и отдает маной.
 * Мгновенно, на себя. Убить себя нельзя (пол — 1 HP).
 */
public class BloodPriceSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "blood_price");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(45)
            .build();

    public BloodPriceSpell() {
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 10;
    }

    private float getHpCost(int spellLevel) {
        return 2 + spellLevel; // 3..7 HP
    }

    private float getManaGain(int spellLevel, LivingEntity caster) {
        return getHpCost(spellLevel) * 7 * getEntityPowerMultiplier(caster);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.abyss_spells.hp_cost", Utils.stringTruncation(getHpCost(spellLevel) / 2, 1)),
                Component.translatable("ui.irons_spellbooks.mana_recovery", Utils.stringTruncation(getManaGain(spellLevel, caster), 0))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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
        return Optional.of(SoundEvents.SCULK_CATALYST_BLOOM);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_ANIMATION;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        // Нельзя платить, если после платы останется меньше 1 HP
        return entity.getHealth() > getHpCost(spellLevel) + 1;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        float cost = getHpCost(spellLevel);
        // Точная цена напрямую, без hurt(): урон по себе с собой-же атакующим
        // дает NaN-нокбэк (деление на нулевой вектор) — отсюда отброс.
        entity.setHealth(Math.max(1.0f, entity.getHealth() - cost));

        float gain = getManaGain(spellLevel, entity);
        if (entity instanceof ServerPlayer serverPlayer) {
            MagicData data = MagicData.getPlayerMagicData(serverPlayer);
            double max = serverPlayer.getAttributeValue(AttributeRegistry.MAX_MANA);
            data.setMana((float) Math.min(max, data.getMana() + gain));
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(data));
        }

        level.playSound(null, entity.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8f, 0.6f);
        MagicManager.spawnParticles(level, ParticleTypes.SCULK_SOUL,
                entity.getX(), entity.getY() + 1.0, entity.getZ(), 16, 0.4, 0.6, 0.4, 0.15, false);
        MagicManager.spawnParticles(level, ParticleTypes.CRIMSON_SPORE,
                entity.getX(), entity.getY() + 1.0, entity.getZ(), 12, 0.4, 0.5, 0.4, 0.1, false);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        if (level.isClientSide) {
            level.addParticle(ParticleTypes.SCULK_CHARGE_POP,
                    entity.getX(), entity.getY() + 1.2, entity.getZ(), 0, 0.5, 0);
        }
        super.onClientCast(level, spellLevel, entity, castData);
    }
}
