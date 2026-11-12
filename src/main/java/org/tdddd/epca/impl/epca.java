package org.tdddd.epca.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdddd.eej.api.AltarInteractionRegistry;
import org.tdddd.epca.impl.overworld.data.*;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganEffects;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatManager;
import org.tdddd.epca.impl.overworld.registry.blocks.EpcaAltarInteractionHandler;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.capability.EpcaAttachments;
import org.tdddd.epca.impl.client.ClientSetup;
import org.tdddd.epca.impl.commands.*;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.events.EvolutionStageEvents;
import org.tdddd.epca.impl.events.ShieldCapabilityHandler;
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
    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    };

    public epca(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(RegisterEvent.class, event -> {
            if (event.getRegistryKey().equals(Registries.GAME_RULE)) {
                DO_INFESTED_FALLBACK = GameRules.registerBoolean(
                        "epca_hardness_conversion_block", GameRuleCategory.MISC, true);
            }
        });

        IEventBus forgeBus = NeoForge.EVENT_BUS;

        ModNetwork.register(modEventBus);

        EpcaAttachments.register(modEventBus);

        ShieldCapabilityHandler.init();
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

        ModConfig.register(modContainer);
        WingChestManager.init();

        if (org.tdddd.epca.impl.compat.EejCompat.loaded()) {
            org.tdddd.epca.impl.compat.EejAltarCompat.registerInteraction();
        }

        if (FMLEnvironment.getDist() == Dist.CLIENT) {

        }

        modEventBus.addListener(this::clientSetup);

        modEventBus.addListener(org.tdddd.epca.impl.datagen.DataGenEvent::gatherClientData);
        modEventBus.addListener(org.tdddd.epca.impl.datagen.DataGenEvent::gatherServerData);

        forgeBus.addListener(this::onRegisterCommands);
        forgeBus.addListener(this::onServerStarted);
        forgeBus.addListener(this::onAddReloadListeners);
        forgeBus.addListener(this::onPlayerTick);
    }

    public static final net.neoforged.neoforge.registries.DeferredHolder<
            net.neoforged.neoforge.attachment.AttachmentType<?>,
            net.neoforged.neoforge.attachment.AttachmentType<
                    org.tdddd.epca.impl.overworld.registry.capability.LifetimeCapability>> LIFETIME_CAPABILITY =
            EpcaAttachments.LIFETIME;

    private void clientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("jade")) {
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        org.tdddd.epca.impl.commands.BiomassCountCommand.register(event.getDispatcher());
        EvolutionCommand.register(event.getDispatcher());
        NegativeDamageCommand.register(event.getDispatcher());
        event.getDispatcher().register(ParasiteSummonCommand.register());
        event.getDispatcher().register(ParasiteSummonCommand.registerSetParasite());
        HiveLeaderCommand.register(event);
        org.tdddd.epca.impl.commands.LikeBiomeCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        EvolutionDataStorage.get(overworld);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        LivingArmorBox.applyBiomassEffects(event.getEntity());

        if (!event.getEntity().level().isClientSide()) {
            if (HiveLeaderManager.isHiveLeader(event.getEntity().getUUID())) {
                HiveLeaderOrganEffects.tick(event.getEntity());
            } else {
                HiveLeaderOrganEffects.notLeader(event.getEntity());
            }
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddServerReloadListenersEvent event) {

        event.addListener(asResource("entity_conversion"), new EntityConversionManager());
        event.addListener(asResource("entity_integration"), new EntityIntegrationManager());
        event.addListener(asResource("entity_kill_count"), new EntityBiomassCountManager());
        event.addListener(asResource("carry_config"), CarryConfigManager.INSTANCE);
        event.addListener(asResource("biomass_spawn"), new BiomassSpawnManager());

        event.addListener(asResource("organ_stats"), new OrganStatManager());
    }

    public static net.minecraft.world.level.gamerules.GameRule<Boolean> DO_INFESTED_FALLBACK;
}
