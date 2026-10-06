package com.abyssspells.spells;

import com.abyssspells.AbyssSpells;
import com.abyssspells.registry.AbyssSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import com.abyssspells.entity.VoidShacklesEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Собственность Пустоты.
 * Ч/б щупальца сковывают цель (RootEntity — двигаться нельзя, оковы ломаются уроном),
 * вытягивают 25% текущей маны цели кастеру, тьма, 1 сердечко чистого урона мимо брони.
 */
public class VoidClaimSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "void_claim");

    /** Доля текущей маны цели, которую забираем. */
    private static final float MANA_STEAL = 0.25f;
    /** Фикс чистый урон: 1 сердечко, броня игнорируется (см. void_claim damage type tags). */
    private static final float TRUE_DAMAGE = 2.0f;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(90)
            .build();

    public VoidClaimSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 30; // 1.5 сек — цель видит, что ее берут
        this.baseManaCost = 175;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getShackleDuration(spellLevel, caster), 1)),
                Component.translatable("ui.abyss_spells.mana_steal", 25),
                Component.translatable("ui.irons_spellbooks.damage", "2")
        );
    }

    private int getShackleDuration(int spellLevel, LivingEntity caster) {
        return 60 + spellLevel * 40; // 5с .. 9с
    }

    private float getRootHealth(int spellLevel, LivingEntity caster) {
        return (12 + spellLevel * 6) * getEntityPowerMultiplier(caster);
    }

    /** Последний источник каста по игроку — нужен чтобы выставить кд по уровню после каста. */
    private static final Map<UUID, CastSource> LAST_SOURCE = new ConcurrentHashMap<>();

    /** Кд по уровню: 1 -> 90с, 2 -> 75с, 3 -> 60с. */
    private static int cooldownForLevel(int spellLevel) {
        return switch (spellLevel) {
            case 1 -> 1800;
            case 2 -> 1500;
            default -> 1200;
        };
    }

    private int getRange(int spellLevel) {
        return 12 + spellLevel * 2; // 14/16/18
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
        return SpellAnimations.CHARGE_RAISED_HAND;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ONE_HANDED_RAY_SHOOT;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, getRange(spellLevel), .35f);
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
        LAST_SOURCE.put(entity.getUUID(), castSource);

        // 1. Оковы-щупальца (кроме боссов из CANT_ROOT — тем только остальное)
        int duration = getShackleDuration(spellLevel, entity);
        if (!target.getType().is(ModTags.CANT_ROOT)) {
            float health = getRootHealth(spellLevel, entity);
            VoidShacklesEntity shackles = new VoidShacklesEntity(level, entity);
            shackles.setDuration(duration);
            shackles.setTarget(target);
            shackles.moveTo(target.position());
            shackles.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            shackles.setHealth(health);
            level.addFreshEntity(shackles);
            target.stopRiding();
            target.startRiding(shackles, true);
        }

        // 2. Чистый урон 1 сердечко мимо брони
        var damageSource = level.damageSources().source(AbyssSchools.VOID_CLAIM_DAMAGE_KEY, entity);
        target.hurt(damageSource, TRUE_DAMAGE);

        // 3. Тьма на время оков
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0));

        // 4. Кража маны — всегда видимая. Игроки отдают 25% текущей,
        //    за мобов (у них маны нет) доплачивает сама пустота.
        if (entity instanceof ServerPlayer casterPlayer) {
            MagicData casterData = MagicData.getPlayerMagicData(casterPlayer);
            double casterMax = casterPlayer.getAttributeValue(AttributeRegistry.MAX_MANA);
            float gained = 0f;
            if (target instanceof ServerPlayer victimPlayer) {
                MagicData victimData = MagicData.getPlayerMagicData(victimPlayer);
                float stolen = victimData.getMana() * MANA_STEAL;
                if (stolen > 0.5f) {
                    victimData.setMana(victimData.getMana() - stolen);
                    PacketDistributor.sendToPlayer(victimPlayer, new SyncManaPacket(victimData));
                    gained = stolen;
                }
            } else {
                gained = 6 + spellLevel * 3;
            }
            if (gained > 0) {
                casterData.setMana((float) Math.min(casterMax, casterData.getMana() + gained));
                PacketDistributor.sendToPlayer(casterPlayer, new SyncManaPacket(casterData));
                casterPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.abyss_spells.mana_stolen", Utils.stringTruncation(gained, 1))));
            }
        }

        // 5. Ч/б щупальца: столб чернил + белые всполохи вокруг цели
        double tx = target.getX(), ty = target.getY(), tz = target.getZ();
        for (int i = 0; i < 26; i++) {
            double angle = (Math.PI * 2 * i) / 26;
            double r = 0.9 + (i % 3) * 0.25;
            MagicManager.spawnParticles(level, ParticleTypes.SQUID_INK,
                    tx + Math.cos(angle) * r, ty + 0.2 + (i % 5) * 0.4, tz + Math.sin(angle) * r,
                    2, 0, 0.55, 0, 0.15, false);
        }
        for (int i = 0; i < 14; i++) {
            double angle = (Math.PI * 2 * i) / 14 + 0.2;
            MagicManager.spawnParticles(level, ParticleTypes.WHITE_ASH,
                    tx + Math.cos(angle) * 1.3, ty + 0.3 + (i % 4) * 0.5, tz + Math.sin(angle) * 1.3,
                    1, -Math.cos(angle) * 0.3, 0.35, -Math.sin(angle) * 0.3, 0.2, false);
        }
        MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL,
                tx, ty + 1.0, tz, 10, 0.4, 0.6, 0.4, 0.1, false);

        // 6. Звук: скрежет бездны на цели
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.9f, 0.6f);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
        // Базовый поток уже выставил кд 90с — перезаписываем точным по уровню.
        // Свитки (CastSource.SCROLL) кд не получают, как принято в Irons.
        if (!cancelled && !level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            CastSource source = LAST_SOURCE.remove(entity.getUUID());
            if (source != null && source != CastSource.SCROLL) {
                playerMagicData.getPlayerCooldowns().addCooldown(this, cooldownForLevel(spellLevel));
                playerMagicData.getPlayerCooldowns().syncToPlayer(serverPlayer);
            }
        }
    }
}
