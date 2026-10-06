package com.abyssspells.registry;

import com.abyssspells.AbyssSpells;
import com.abyssspells.spells.AbyssCoreSpell;
import com.abyssspells.spells.AnchorSpell;
import com.abyssspells.spells.AbyssalCrushSpell;
import com.abyssspells.spells.BloodPriceSpell;
import com.abyssspells.spells.DoomMarkSpell;
import com.abyssspells.spells.EquilibriumSpell;
import com.abyssspells.spells.OfferingSpell;
import com.abyssspells.spells.RetributionSpell;
import com.abyssspells.spells.TranspositionSpell;
import com.abyssspells.spells.VoidClaimSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, AbyssSpells.MODID);

    public static final Supplier<AbstractSpell> ABYSSAL_CRUSH =
            SPELLS.register("abyssal_crush", AbyssalCrushSpell::new);

    public static final Supplier<AbstractSpell> VOID_CLAIM =
            SPELLS.register("void_claim", VoidClaimSpell::new);

    public static final Supplier<AbstractSpell> BLOOD_PRICE =
            SPELLS.register("blood_price", BloodPriceSpell::new);

    public static final Supplier<AbstractSpell> EQUILIBRIUM =
            SPELLS.register("equilibrium", EquilibriumSpell::new);

    public static final Supplier<AbstractSpell> DOOM_MARK =
            SPELLS.register("doom_mark", DoomMarkSpell::new);

    public static final Supplier<AbstractSpell> ABYSS_CORE =
            SPELLS.register("abyss_core", AbyssCoreSpell::new);

    public static final Supplier<AbstractSpell> OFFERING =
            SPELLS.register("abyss_offering", OfferingSpell::new);

    public static final Supplier<AbstractSpell> TRANSPOSITION =
            SPELLS.register("transposition", TranspositionSpell::new);

    public static final Supplier<AbstractSpell> RETRIBUTION =
            SPELLS.register("retribution", RetributionSpell::new);

    public static final Supplier<AbstractSpell> FATE_ANCHOR =
            SPELLS.register("fate_anchor", AnchorSpell::new);
}
