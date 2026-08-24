package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, epca.MODID);

    public static final RegistryObject<SimpleParticleType> SPLASHI =
            REGISTRY.register("splashi", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> COTH =
            REGISTRY.register("coth", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> BLEEDING =
            REGISTRY.register("bleeding", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> LIVING_FLESH =
            REGISTRY.register("living_flesh", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> WAVE =
            REGISTRY.register("wave", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> WAVE_SMALL =
            REGISTRY.register("wave_small", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> INFESTIVE_GAS =
            REGISTRY.register("infestive_gas", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> BIOMASS =
            REGISTRY.register("biomass", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> BIOMASS_BOOM_SMALL =
            REGISTRY.register("biomass_boom_small", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> BIOMASS_BOOM_MEDI =
            REGISTRY.register("biomass_boom_medi", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> F_ADAPTATION =
            REGISTRY.register("f_adaptation", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> P_ADAPTATION =
            REGISTRY.register("p_adaptation", () -> new SimpleParticleType(false));
}