package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.overworld.registry.effects.buff.SoulProtectionEffect;
import org.tdddd.epca.impl.overworld.registry.effects.buff.CamouflageEffect;
import org.tdddd.epca.impl.overworld.registry.effects.buff.SpiritEffect;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.effects.buff.RageEffect;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.*;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, epca.MODID);

    public static final RegistryObject<MobEffect> BLEEDING =
            EFFECTS.register("bleeding", BleedingEffect::new);

    public static final RegistryObject<MobEffect> VIRAL =
            EFFECTS.register("viral", ViralEffect::new);

    public static final RegistryObject<MobEffect> FEAR =
            EFFECTS.register("fear", FearEffect::new);

    public static final RegistryObject<MobEffect> COTH =
            EFFECTS.register("coth", CothEffect::new);

    public static final RegistryObject<MobEffect> CONTEMPT_INORGANIC =
            EFFECTS.register("contempt_inorganic", ContemptInorganicEffect::new);

    public static final RegistryObject<MobEffect> CORROSIVE =
            EFFECTS.register("corrosive", CorrosiveEffect::new);

    public static final RegistryObject<MobEffect> RAGE =
            EFFECTS.register("rage", RageEffect::new);

    public static final RegistryObject<MobEffect> NEEDLER =
            EFFECTS.register("needler", NeedlerEffect::new);

    public static final RegistryObject<MobEffect> DEEP_SNEAK =
            EFFECTS.register("deep_sneak", DeepSneakEffect::new);

    public static final RegistryObject<MobEffect> SOLIDIFY =
            EFFECTS.register("solidify", SolidifyEffect::new);

    public static final RegistryObject<MobEffect> ENDER_EROSION =
            EFFECTS.register("ender_erosion", EnderErosionEffect::new);

    public static final RegistryObject<MobEffect> SPIRIT =
            EFFECTS.register("spirit", SpiritEffect::new);

    public static final RegistryObject<MobEffect> CAMOUFLAGE =
            EFFECTS.register("camouflage", CamouflageEffect::new);

    public static final RegistryObject<MobEffect> SOUL_PROTECTION =
            EFFECTS.register("soul_protection", SoulProtectionEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
