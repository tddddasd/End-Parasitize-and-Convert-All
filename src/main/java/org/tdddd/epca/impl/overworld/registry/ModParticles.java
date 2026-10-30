package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.particles.ConversionParticleType;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, epca.MODID);

    public static final RegistryObject<SimpleParticleType> SPLASHI =
            REGISTRY.register("splashi", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> PARA_CURSE =
            REGISTRY.register("para_curse", () -> new SimpleParticleType(false));

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

    public static final RegistryObject<ConversionParticleType> CONVERSION =
            REGISTRY.register("conversion", ConversionParticleType::new);
    public static final RegistryObject<ConversionParticleType> INFESTED_CONVERSION =
            REGISTRY.register("infested_conversion", ConversionParticleType::new);
    public static final RegistryObject<ConversionParticleType> ONESENT_CONVERSION =
            REGISTRY.register("onesent_conversion", ConversionParticleType::new);
    public static final RegistryObject<ConversionParticleType> POVERTY_CONVERSION =
            REGISTRY.register("poverty_conversion", ConversionParticleType::new);
    public static final RegistryObject<ConversionParticleType> RESHAPE_CONVERSION =
            REGISTRY.register("reshape_conversion", ConversionParticleType::new);
    public static final RegistryObject<ConversionParticleType> HORIZONTAL_CONVERSION =
            REGISTRY.register("horizontal_conversion", ConversionParticleType::new);

    public static ConversionParticleType byVariant(int variant) {
        return switch (variant) {
            case 1 -> INFESTED_CONVERSION.get();
            case 2 -> ONESENT_CONVERSION.get();
            case 3 -> POVERTY_CONVERSION.get();
            case 4 -> RESHAPE_CONVERSION.get();
            case 5 -> HORIZONTAL_CONVERSION.get();
            default -> CONVERSION.get();
        };
    }

    public static void spawnConversion(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.entity.Entity entity, int variant, int color) {
        spawnConversion(level, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), variant, color);
    }

    public static void spawnConversion(net.minecraft.server.level.ServerLevel level,
                                       double x, double y, double z, int variant, int color) {
        level.sendParticles(new org.tdddd.epca.impl.overworld.registry.particles.ConversionParticleOptions(color, variant),
                x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}