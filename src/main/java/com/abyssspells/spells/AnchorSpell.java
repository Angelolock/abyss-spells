package com.abyssspells.spells;

import com.abyssspells.AbyssSpells;
import com.abyssspells.registry.AbyssSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/**
 * Якорь судьбы: первый каст запоминает место/HP/ману,
 * второй за 30 секунд возвращает все назад.
 */
public class AnchorSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "fate_anchor");

    private static final int RECAST_WINDOW_TICKS = 600; // 30 сек

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(AbyssSchools.ABYSS_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(120)
            .build();

    public AnchorSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 80;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.abyss_spells.anchor_window", 30),
                Component.translatable("ui.abyss_spells.anchor_restore")
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
        return Optional.of(SoundRegistry.ABYSSAL_SHROUD.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2; // якорь + возврат
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new AnchorData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        // Второе нажатие — перемотка
        if (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
            var recast = playerMagicData.getPlayerRecasts().getRecastInstance(getSpellId());
            if (recast.getCastData() instanceof AnchorData anchor && anchor.valid) {
                rewind(level, entity, anchor);
            }
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        // Первое нажатие — якорь + видимый маркер
        AnchorData anchor = AnchorData.snapshot(entity, playerMagicData);
        var recast = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity),
                RECAST_WINDOW_TICKS, castSource, anchor);
        playerMagicData.getPlayerRecasts().addRecast(recast, playerMagicData);
        clearMarkers(level, entity);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(new com.abyssspells.entity.AnchorMarkerEntity(level, entity, RECAST_WINDOW_TICKS));
        }
        Vec3Helper.burst(level, entity.position().add(0, 1.0, 0));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void rewind(Level level, LivingEntity entity, AnchorData anchor) {
        Vec3Helper.burst(level, entity.position().add(0, 1.0, 0));
        clearMarkers(level, entity);
        boolean sameDim = entity.level().dimension().equals(anchor.dim);
        if (sameDim) {
            Utils.handleSpellTeleport(this, entity,
                    new net.minecraft.world.phys.Vec3(anchor.x, anchor.y, anchor.z));
            entity.setYRot(anchor.yaw);
            entity.setXRot(anchor.pitch);
        }
        // Возврат HP и маны (точно как было, в пределах максимумов)
        entity.setHealth(Math.min(entity.getMaxHealth(), Math.max(1.0f, anchor.hp)));
        if (entity instanceof ServerPlayer serverPlayer) {
            MagicData data = MagicData.getPlayerMagicData(serverPlayer);
            double max = serverPlayer.getAttributeValue(AttributeRegistry.MAX_MANA);
            data.setMana((float) Math.min(max, anchor.mana));
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(data));
        }
        Vec3Helper.burst(level, new net.minecraft.world.phys.Vec3(anchor.x, anchor.y + 1.0, anchor.z));
        level.playSound(null, entity.blockPosition(), SoundRegistry.ABYSSAL_TELEPORT.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** Убрать свои маркеры (новый якорь / возврат). */
    private void clearMarkers(Level level, LivingEntity entity) {
        if (level.isClientSide) {
            return;
        }
        for (com.abyssspells.entity.AnchorMarkerEntity marker : level.getEntitiesOfClass(
                com.abyssspells.entity.AnchorMarkerEntity.class,
                entity.getBoundingBox().inflate(128.0),
                m -> entity.getUUID().equals(m.getOwnerUUID()))) {
            Vec3Helper.burst(level, marker.position());
            marker.discard();
        }
    }

    /** Снапшот судьбы: позиция, взгляд, измерение, HP, мана. */
    public static class AnchorData implements ICastDataSerializable {
        boolean valid;
        double x, y, z;
        float yaw, pitch, hp, mana;
        ResourceKey<Level> dim;

        @Override
        public void reset() {
            valid = false;
        }

        static AnchorData snapshot(LivingEntity entity, MagicData data) {
            AnchorData a = new AnchorData();
            a.valid = true;
            a.x = entity.getX();
            a.y = entity.getY();
            a.z = entity.getZ();
            a.yaw = entity.getYRot();
            a.pitch = entity.getXRot();
            a.hp = entity.getHealth();
            a.dim = entity.level().dimension();
            a.mana = (data != null) ? data.getMana() : 0f;
            return a;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
            buffer.writeBoolean(valid);
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeFloat(yaw);
            buffer.writeFloat(pitch);
            buffer.writeFloat(hp);
            buffer.writeFloat(mana);
            buffer.writeUtf(dim == null ? "" : dim.location().toString());
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
            valid = buffer.readBoolean();
            x = buffer.readDouble();
            y = buffer.readDouble();
            z = buffer.readDouble();
            yaw = buffer.readFloat();
            pitch = buffer.readFloat();
            hp = buffer.readFloat();
            mana = buffer.readFloat();
            String id = buffer.readUtf();
            dim = id.isEmpty() ? null : ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(id));
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("valid", valid);
            tag.putDouble("x", x);
            tag.putDouble("y", y);
            tag.putDouble("z", z);
            tag.putFloat("yaw", yaw);
            tag.putFloat("pitch", pitch);
            tag.putFloat("hp", hp);
            tag.putFloat("mana", mana);
            tag.putString("dim", dim == null ? "" : dim.location().toString());
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            valid = nbt.getBoolean("valid");
            x = nbt.getDouble("x");
            y = nbt.getDouble("y");
            z = nbt.getDouble("z");
            yaw = nbt.getFloat("yaw");
            pitch = nbt.getFloat("pitch");
            hp = nbt.getFloat("hp");
            mana = nbt.getFloat("mana");
            String id = nbt.getString("dim");
            dim = id.isEmpty() ? null : ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(id));
        }
    }

    private static final class Vec3Helper {
        static void burst(Level level, net.minecraft.world.phys.Vec3 pos) {
            MagicManager.spawnParticles(level, ParticleTypes.REVERSE_PORTAL,
                    pos.x, pos.y, pos.z, 16, 0.6, 0.8, 0.6, 0.1, false);
            MagicManager.spawnParticles(level, ParticleTypes.WHITE_ASH,
                    pos.x, pos.y, pos.z, 8, 0.4, 0.4, 0.4, 0.08, false);
        }
    }
}
