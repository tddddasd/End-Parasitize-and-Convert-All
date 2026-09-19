package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.epca;

// 26.1.2: removed @EventBusSubscriber -- this class declares no @SubscribeEvent methods,
// and the loader now throws IllegalArgumentException when such a class is registered.
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, epca.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPLASHI =
            REGISTRY.register("splashi", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COTH =
            REGISTRY.register("coth", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLEEDING =
            REGISTRY.register("bleeding", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIVING_FLESH =
            REGISTRY.register("living_flesh", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WAVE =
            REGISTRY.register("wave", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WAVE_SMALL =
            REGISTRY.register("wave_small", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> INFESTIVE_GAS =
            REGISTRY.register("infestive_gas", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIOMASS =
            REGISTRY.register("biomass", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIOMASS_BOOM_SMALL =
            REGISTRY.register("biomass_boom_small", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIOMASS_BOOM_MEDI =
            REGISTRY.register("biomass_boom_medi", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> F_ADAPTATION =
            REGISTRY.register("f_adaptation", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> P_ADAPTATION =
            REGISTRY.register("p_adaptation", () -> new SimpleParticleType(false));
}
