package com.abyssspells.registry;

import com.abyssspells.AbyssSpells;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AbyssSpells.MODID);

    /** Фокус школы Бездны для Кузницы свитков (в 3-й слот, вместо эха). */
    public static final Supplier<Item> ABYSS_HEART = ITEMS.register("abyss_heart",
            () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));

    // ---------- Материал брони Бездны (без реестра, Holder.direct) ----------
    public static final net.minecraft.core.Holder<ArmorMaterial> ABYSS_ARMOR_MATERIAL =
            net.minecraft.core.Holder.direct(new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.BOOTS, 3);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(Items.ECHO_SHARD),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, "abyss"))),
                    3.0f,
                    0.0f
            ));

    private static ResourceLocation modId(String path) {
        return ResourceLocation.fromNamespaceAndPath(AbyssSpells.MODID, path);
    }

    private static Item.Properties armorProps(ArmorItem.Type type, int defense, int durability, String id) {
        var full = ItemAttributeModifiers.builder();
        EquipmentSlotGroup slot = slotFor(type);
        full.add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                new AttributeModifier(modId("abyss_armor_" + id), defense, AttributeModifier.Operation.ADD_VALUE), slot);
        full.add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(modId("abyss_tough_" + id), 3.0, AttributeModifier.Operation.ADD_VALUE), slot);
        full.add(ModAttributes.ABYSS_SPELL_POWER,
                new AttributeModifier(modId("abyss_power_" + id), 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
        full.add(AttributeRegistry.SPELL_POWER,
                new AttributeModifier(modId("abyss_spell_" + id), 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
        full.add(AttributeRegistry.MAX_MANA,
                new AttributeModifier(modId("abyss_mana_" + id), 125.0, AttributeModifier.Operation.ADD_VALUE), slot);
        return new Item.Properties().attributes(full.build()).rarity(Rarity.RARE).durability(durability);
    }

    private static EquipmentSlotGroup slotFor(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> EquipmentSlotGroup.HEAD;
            case CHESTPLATE -> EquipmentSlotGroup.CHEST;
            case LEGGINGS -> EquipmentSlotGroup.LEGS;
            case BOOTS -> EquipmentSlotGroup.FEET;
            case BODY -> EquipmentSlotGroup.BODY;
        };
    }

    // ---------- Сет Бездны (прочность уровня алмаза, Gecko-модель) ----------
    public static final Supplier<Item> ABYSS_HOOD = ITEMS.register("abyss_hood",
            () -> new com.abyssspells.item.AbyssArmorItem(ABYSS_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    armorProps(ArmorItem.Type.HELMET, 3, 363, "hood")));

    public static final Supplier<Item> ABYSS_ROBE = ITEMS.register("abyss_robe",
            () -> new com.abyssspells.item.AbyssArmorItem(ABYSS_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    armorProps(ArmorItem.Type.CHESTPLATE, 8, 528, "robe")));

    public static final Supplier<Item> ABYSS_LEGGINGS = ITEMS.register("abyss_leggings",
            () -> new com.abyssspells.item.AbyssArmorItem(ABYSS_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    armorProps(ArmorItem.Type.LEGGINGS, 6, 495, "leggings")));

    public static final Supplier<Item> ABYSS_BOOTS = ITEMS.register("abyss_boots",
            () -> new com.abyssspells.item.AbyssArmorItem(ABYSS_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    armorProps(ArmorItem.Type.BOOTS, 3, 429, "boots")));

    // ---------- Посох Бездны (настоящий стафф Irons, без имбумента) ----------
    public static final Supplier<Item> ABYSS_STAFF = ITEMS.register("abyss_staff",
            () -> new com.abyssspells.item.AbyssStaffItem(staffProps()));

    private static Item.Properties staffProps() {
        var builder = ItemAttributeModifiers.builder();
        builder.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        builder.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.5, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        builder.add(ModAttributes.ABYSS_SPELL_POWER,
                new AttributeModifier(modId("abyss_power_staff"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.MAINHAND);
        builder.add(AttributeRegistry.SPELL_POWER,
                new AttributeModifier(modId("abyss_spell_staff"), 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.MAINHAND);
        builder.add(AttributeRegistry.CAST_TIME_REDUCTION,
                new AttributeModifier(modId("abyss_cast_staff"), 0.1, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        return new Item.Properties().attributes(builder.build()).rarity(Rarity.EPIC);
    }
}
