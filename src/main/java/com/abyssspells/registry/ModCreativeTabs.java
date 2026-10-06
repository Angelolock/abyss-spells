package com.abyssspells.registry;

import com.abyssspells.AbyssSpells;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AbyssSpells.MODID);

    public static final Supplier<CreativeModeTab> ABYSS_TAB = TABS.register("abyss",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.abyss_spells"))
                    .icon(() -> new ItemStack(ModItems.ABYSS_HEART.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ABYSS_STAFF.get());
                        output.accept(ModItems.ABYSS_HOOD.get());
                        output.accept(ModItems.ABYSS_ROBE.get());
                        output.accept(ModItems.ABYSS_LEGGINGS.get());
                        output.accept(ModItems.ABYSS_BOOTS.get());
                        output.accept(ModItems.ABYSS_HEART.get());
                    })
                    .build());
}
