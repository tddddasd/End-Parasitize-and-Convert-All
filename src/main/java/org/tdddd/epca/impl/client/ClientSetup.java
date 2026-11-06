package org.tdddd.epca.impl.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.client.organ.HiveLeaderOrganKeys;
import org.tdddd.epca.impl.overworld.registry.ModMenus;
import org.tdddd.epca.impl.overworld.registry.gui.menus.SwallowCystScreen;
import org.tdddd.epca.impl.overworld.registry.particles.AdaptationParticleProvider;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.particles.partices.*;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup1(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

        });

        org.tdddd.epca.impl.client.render.EpcaRenderClient.onClientSetup(event);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(
                org.tdddd.epca.impl.client.render.EpcaGibRenderer.class);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SWALLOW_CYST.get(), SwallowCystScreen::new);
    }

    @SubscribeEvent
    public static void registerItemModelProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(epca.MODID, "evolution_stage"), EvolutionStageProperty.MAP_CODEC);
    }

    public static class EvolutionStageProperty implements RangeSelectItemModelProperty {
        public static final MapCodec<EvolutionStageProperty> MAP_CODEC =
                MapCodec.unit(new EvolutionStageProperty());

        @Override
        public float get(ItemStack itemStack, ClientLevel level, ItemOwner owner, int seed) {
            Level world = level;
            if (owner != null) {
                LivingEntity living = owner.asLivingEntity();
                if (living != null) {
                    world = living.level();
                }
            }
            if (world == null) return 0f;
            return (float) (ClientEvolutionData.getStageForDimension(world) + 2);
        }

        @Override
        public MapCodec<EvolutionStageProperty> type() {
            return MAP_CODEC;
        }
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(HiveLeaderOrganKeys.OPEN_ORGANS);
        event.register(HiveLeaderOrganKeys.TELEPORT);
        event.register(HiveLeaderOrganKeys.DECOMPOSE_PARASITE);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.CONVERSION.get(), spriteSet ->
                spriteSet == null ? null : new org.tdddd.epca.impl.client.particles.ConversionParticle.Provider(spriteSet, true));

        event.registerSpriteSet(ModParticles.HORIZONTAL_CONVERSION.get(), spriteSet ->
                spriteSet == null ? null : new org.tdddd.epca.impl.client.particles.HorizontalConversionParticle.Provider(spriteSet));
        for (var type : java.util.List.of(ModParticles.INFESTED_CONVERSION, ModParticles.ONESENT_CONVERSION,
                ModParticles.POVERTY_CONVERSION, ModParticles.RESHAPE_CONVERSION)) {
            event.registerSpriteSet(type.get(), spriteSet ->
                    spriteSet == null ? null : new org.tdddd.epca.impl.client.particles.ConversionParticle.Provider(spriteSet, false));
        }

        event.registerSpriteSet(
                ModParticles.SPLASHI.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new SplashiParticle.Provider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.PARA_CURSE.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new ParaCurseParticle.ParaCurseParticleProvider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.BLEEDING.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new BleedingParticle.BleedingParticleProvider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.LIVING_FLESH.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new LivingFleshParticle.Provider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.WAVE.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new WaveParticle.WaveParticleProvider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.WAVE_SMALL.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new WaveSmallParticle.WaveSmallParticleProvider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.INFESTIVE_GAS.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new InfestiveGasParticle.InfestiveGasParticleProvider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.BIOMASS.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new BiomassParticle.BiomassParticleProvider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.BIOMASS_BOOM_SMALL.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new BiomassBoomSmallParticle.BiomassBoomSmallParticleProvider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.BIOMASS_BOOM_MEDI.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new BiomassBoomMediParticle.BiomassBoomMediParticleProvider(spriteSet);
                });

        event.registerSpriteSet(ModParticles.F_ADAPTATION.get(), AdaptationParticleProvider::new);
        event.registerSpriteSet(ModParticles.P_ADAPTATION.get(), AdaptationParticleProvider::new);
    }
}
