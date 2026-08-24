package org.tdddd.epca.impl.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.renderer.ItemDisplayRenderer;
import org.tdddd.epca.impl.overworld.registry.ModMenus;
import org.tdddd.epca.impl.overworld.registry.gui.menus.SwallowCystScreen;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.particles.AdaptationParticleProvider;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.particles.partices.*;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup1(FMLClientSetupEvent event) {
        
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(ModBlockEntities.PACKED_MUD_PEDESTAL.get(),
                    ItemDisplayRenderer::new);
            MenuScreens.register(ModMenus.SWALLOW_CYST.get(), SwallowCystScreen::new);
            Item bloodyClock = ModItems.BLOODY_CLOCK.get();
            ItemProperties.register(bloodyClock, new ResourceLocation("stage"),
                    (stack, level, entity, seed) -> {
                        Level world = level;
                        if (entity instanceof LivingEntity) world = entity.level();
                        if (world == null) return 0f;   
                        int stage = ClientEvolutionData.getStageForDimension(world);
                        
                        return (float)(stage + 2);
                    });
        });
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                ModParticles.SPLASHI.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new SplashiParticle.Provider(spriteSet);
                });

        event.registerSpriteSet(
                ModParticles.COTH.get(),
                spriteSet -> {
                    if (spriteSet == null) {
                        return null;
                    }
                    return new CothParticle.CothParticleProvider(spriteSet);
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