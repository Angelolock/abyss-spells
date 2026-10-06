package com.abyssspells.registry;

import com.abyssspells.AbyssSpells;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AbyssSchools {
    // Регистрируем в том же реестре школ, что и сам Irons, но от нашего modid
    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, AbyssSpells.MODID);

    public static final ResourceLocation ABYSS_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "abyss");

    public static final TagKey<Item> ABYSS_FOCUS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "abyss_focus"));

    public static final ResourceKey<DamageType> ABYSS_DAMAGE_KEY =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "abyss_magic"));

    /** Чистый урон Собственности Пустоты (мимо брони, см. теги bypass). */
    public static final ResourceKey<DamageType> VOID_CLAIM_DAMAGE_KEY =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "void_claim"));

    public static final Supplier<SchoolType> ABYSS = SCHOOLS.register("abyss", () -> new SchoolType(
            ABYSS_RESOURCE,
            ABYSS_FOCUS,
            Component.translatable("school.abyss_spells.abyss").withStyle(Style.EMPTY.withColor(0x1a0b3d).withBold(true)),
            ModAttributes.ABYSS_SPELL_POWER,
            ModAttributes.ABYSS_MAGIC_RESIST,
            SoundRegistry.ENDER_CAST,
            ABYSS_DAMAGE_KEY
    ));
}
