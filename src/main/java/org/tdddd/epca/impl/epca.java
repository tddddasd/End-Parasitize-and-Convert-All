package org.tdddd.epca.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import org.tdddd.epca.impl.overworld.data.*;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.capability.ILifetimeCapability;
import org.tdddd.epca.impl.client.ClientSetup;
import org.tdddd.epca.impl.commands.*;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.ParasiteAttractionManager;
import org.tdddd.epca.impl.events.EvolutionStageEvents;
import org.tdddd.epca.impl.events.ShieldAttachHandler;
import org.tdddd.epca.impl.fluid.ModFluids;
import org.tdddd.epca.impl.overworld.registry.ModMenus;
import org.tdddd.epca.impl.overworld.registry.items.ModCreativeTabs;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.WingChestManager;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;

@Mod(epca.MODID)
public class epca {
    public static final String MODID = "epca";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MODID, path);
    };

    public epca() {
        Mixins.addConfiguration("epca.mixins.json");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModParticles.REGISTRY.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModEffects.register(modEventBus);
        ModSoundEvents.SOUNDS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        ModConfig.register();
        WingChestManager.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ClientSetup.class);
        }
        
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(EvolutionStageEvents.class);
        MinecraftForge.EVENT_BUS.register(new ShieldAttachHandler());
        forgeBus.addListener(this::onRegisterCommands);
        forgeBus.addListener(this::onServerStarted);
        forgeBus.addListener(this::onAddReloadListeners);
        forgeBus.addListener(this::onServerTickForAttraction);
        forgeBus.addListener(this::onPlayerTick);
    }

    public static Capability<ILifetimeCapability> LIFETIME_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            LIFETIME_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
        });
    }

    private void clientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("jade")) {
        }
    }

    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EvolutionCommand.register(event.getDispatcher());
        ParasiteEnemyCommand.register(event.getDispatcher());
        NegativeDamageCommand.register(event.getDispatcher());
        event.getDispatcher().register(ParasiteSummonCommand.register());
        event.getDispatcher().register(ParasiteSummonCommand.registerSetParasite());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        EvolutionDataStorage.get(overworld);
    }

    
    @SubscribeEvent
    public void onServerTickForAttraction(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = event.getServer();
            
            for (ServerLevel level : server.getAllLevels()) {
                ParasiteAttractionManager.tick(level);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            LivingArmorBox.applyBiomassEffects(event.player);
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EntityConversionManager());
        event.addListener(new EntityIntegrationManager());
        event.addListener(new EntityKillCountManager());
        event.addListener(CarryConfigManager.INSTANCE);
        event.addListener(new BiomassSpawnManager());
        event.addListener(new AltarPointManager());
    }

    // Attribute registration is now handled by ModEntityEvents.onEntityAttributeCreation()
    // which delegates to both EpcaEntityManager.createAttributes() (auto-registration)
    // and registerManualAttributes() (backward compat for existing entities).

    public static final GameRules.Key<GameRules.BooleanValue> DO_INFESTED_FALLBACK =
            GameRules.register("epca_hardnessConversionBlock", GameRules.Category.MISC,
                    GameRules.BooleanValue.create(true));
}