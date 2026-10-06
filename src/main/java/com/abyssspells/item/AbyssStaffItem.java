package com.abyssspells.item;

import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.world.item.Item;

/**
 * Посох Бездны — настоящий стафф Irons (CastingItem):
 * работает имплементом для кастов, НЕ меч (нет sweep, НЕ имбуится на наковальне).
 * Характеристики задаются через Properties.
 */
public class AbyssStaffItem extends StaffItem {
    public AbyssStaffItem(Item.Properties properties) {
        super(properties);
    }
}
