package org.tdddd.epca.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
import org.tdddd.epca.impl.overworld.registry.blocks.EpcaAltarInteractionHandler;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.capability.EpcaAttachments;
import org.tdddd.epca.impl.client.ClientSetup;
import org.tdddd.epca.impl.commands.*;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.ParasiteAttractionManager;
import org.tdddd.epca.impl.events.EvolutionStageEvents;
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

    // 26.1.2: FMLJavaModLoadingContext is gone; the mod event bus is injected
    // into the mod constructor instead (see EPCA-PORT-GUIDE.md).
    public epca(IEventBus modEventBus, ModContainer modContainer) {
        // 26.1.2: register the custom game rule while its registry is still open (a static
        // initializer would run after the registry is frozen).
        modEventBus.addListener(RegisterEvent.class, event -> {
            if (event.getRegistryKey().equals(Registries.GAME_RULE)) {
                DO_INFESTED_FALLBACK = GameRules.registerBoolean(
                        "epca_hardness_conversion_block", GameRuleCategory.MISC, true);
            }
        });
        // 26.1.2 用 neoforge.mods.toml 的 [[mixins]] 声明 Mixin 配置（FML 会读），
        // 不再需要运行期手工 Mixins.addConfiguration(...)。
        // 保留原调用会与 FML 的配置加载重复，故删除。

        IEventBus forgeBus = NeoForge.EVENT_BUS;
        // 26.1.2 的负载注册事件 RegisterPayloadHandlersEvent 是模组总线事件，
        // 原 FMLCommonSetupEvent 里的 ModNetwork.register() 直接搬到这里。
        ModNetwork.register(modEventBus);
        // 数据附件必须在模组总线注册
        EpcaAttachments.register(modEventBus);
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

        // 祭坛方块本体在前置模组 eej 中，这里把 EPCA 的献祭仪式等交互挂上去
        AltarInteractionRegistry.register(new EpcaAltarInteractionHandler());

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
        // 26.1.2: ClientSetup 自带 @EventBusSubscriber，FML 会自动注册；这里再注册会导致
        // RegisterMenuScreensEvent 处理器执行两次 -> "Duplicate attempt to register screen"。故移除。
        }

        modEventBus.addListener(this::clientSetup);
        // 数据生成：GatherDataEvent 拆成 Client/Server 两个模组总线事件
        modEventBus.addListener(org.tdddd.epca.impl.datagen.DataGenEvent::gatherClientData);
        modEventBus.addListener(org.tdddd.epca.impl.datagen.DataGenEvent::gatherServerData);
        // 26.1.2: 该类自带 @EventBusSubscriber，FML 已自动注册；重复注册会让监听器执行两次，故移除。
        forgeBus.addListener(this::onRegisterCommands);
        forgeBus.addListener(this::onServerStarted);
        forgeBus.addListener(this::onAddReloadListeners);
        forgeBus.addListener(this::onServerTickForAttraction);
        forgeBus.addListener(this::onPlayerTick);
    }

    /**
     * 26.1.2：Forge Capability 已删除，{@code LIFETIME_CAPABILITY} 现在指向注册在
     * {@code NeoForgeRegistries.Keys.ATTACHMENT_TYPES} 上的数据附件类型。
     * 读取方式：{@code livingEntity.getData(EpcaAttachments.LIFETIME)}。
     */
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
        EvolutionCommand.register(event.getDispatcher());
        NegativeDamageCommand.register(event.getDispatcher());
        event.getDispatcher().register(ParasiteSummonCommand.register());
        event.getDispatcher().register(ParasiteSummonCommand.registerSetParasite());
        NestLeaderCommand.register(event);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        EvolutionDataStorage.get(overworld);
    }

    
    @SubscribeEvent
    public void onServerTickForAttraction(ServerTickEvent.Post event) {
        // 26.1.2: TickEvent.Phase 被 Pre/Post 取代，等价于原来的 Phase.END
        MinecraftServer server = event.getServer();

        for (ServerLevel level : server.getAllLevels()) {
            ParasiteAttractionManager.tick(level);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        // 26.1.2: TickEvent.Phase 被 Pre/Post 取代，等价于原来的 Phase.END
        LivingArmorBox.applyBiomassEffects(event.getEntity());
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddServerReloadListenersEvent event) {
        // 26.1.2: addListener 需要显式的 Identifier key（原 API 不需要）
        event.addListener(asResource("entity_conversion"), new EntityConversionManager());
        event.addListener(asResource("entity_integration"), new EntityIntegrationManager());
        event.addListener(asResource("entity_kill_count"), new EntityKillCountManager());
        event.addListener(asResource("carry_config"), CarryConfigManager.INSTANCE);
        event.addListener(asResource("biomass_spawn"), new BiomassSpawnManager());
    }

    // Attribute registration is now handled by ModEntityEvents.onEntityAttributeCreation()
    // which delegates to both EpcaEntityManager.createAttributes() (auto-registration)
    // and registerManualAttributes() (backward compat for existing entities).

    // 26.1.2: GameRules.register 变成了按类型分名的 registerBoolean/registerInteger，
    // Category 改名为 GameRuleCategory，并且布尔规则直接返回 GameRule<Boolean>。
    // 26.1.2: game rules live in the built-in minecraft:game_rule registry, which is frozen
    // before the mod constructor runs. The rule therefore cannot be created in a static
    // initializer; it is registered by the RegisterEvent listener in the constructor.
    public static net.minecraft.world.level.gamerules.GameRule<Boolean> DO_INFESTED_FALLBACK;
}
