package org.tdddd.epca.impl.client;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.gas.GasCloudRenderType;
import org.tdddd.epca.impl.client.entity.model.*;
import org.tdddd.epca.impl.client.entity.renderer.*;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.gen.model.SwallowCystModel;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.io.IOException;
import java.util.Map;

/**
 * Mod-bus client subscribers: entity renderer registration, client setup and the custom core shaders.
 *
 * <p>Every handler here receives a {@code net.minecraftforge.fml.event.IModBusEvent}, so the class
 * is annotated with {@code Bus.MOD}. Forge-bus client work (client ticking) lives in
 * {@link ClientEvents}, which keeps the default bus.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {

        // ── Auto-registered renderers (all entities via EpcaEntityManager.registerMobWithRender) ──
        // These use EpcaTypeGeoRenderer + EpcaTypeGeoModel — reads model/texture/animation
        // from EpcaEntityManager by entity type. No per-entity model class needed.
        for (EntityType<?> type : EpcaEntityManager.consumeRenderTypes()) {
            if (type == ModEntities.RESHAPE_YELLOWEYE.get()) {
                // Has a dedicated renderer below (it needs the shader-rendered gas cloud layer).
                // Registering the generic renderer as well would register this entity type twice.
                continue;
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            EntityType rawType = type;
            event.registerEntityRenderer(rawType, EpcaGeoRenderer::new);
        }

        // ── Custom renderers ONLY (entities with special rendering: non-GeckoLib, shaders, etc.) ──
        event.registerEntityRenderer(ModEntities.CONTAMINATED_WATER.get(), ContaminatedWaterRenderer::new);
        event.registerEntityRenderer(ModEntities.YAWNING_NYA.get(), YawningNyaRenderer::new);
        event.registerEntityRenderer(ModEntities.BONE_FRAGMENT.get(), BoneFragmentRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_ENDER_PEARL.get(), InfestedThrownEnderPearlRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_SPIDER_WEB_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_SPIDER_WEB_BLOOD_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_CAVE_SPIDER_WEB_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_ENDERMAN.get(), InfestedEndermanRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_ENDERMITE.get(), InfestedEndermiteRenderer::new);
        event.registerEntityRenderer(ModEntities.WALKING_ENDERMAN_HEAD.get(), WalkingEndermanHeadRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_ZOMBIE.get(), InfestedZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.RESHAPE_LONGARMS.get(), ReshapeLongarmsRenderer::new);
        event.registerEntityRenderer(ModEntities.RESHAPE_YELLOWEYE.get(), ReshapeYelloweyeRenderer::new);
        event.registerEntityRenderer(ModEntities.RESHAPE_PART.get(), ReshapeLongarmsCustomPartRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_BAT.get(), InfestedBatRenderer::new);

        event.registerEntityRenderer(ModEntities.RIPPER.get(), ctx -> new EpcaGeoRenderer<>(ctx, new RipperModel()));
        event.registerEntityRenderer(ModEntities.FINS.get(), ctx -> new EpcaGeoRenderer<>(ctx, new FinsModel()));
        event.registerEntityRenderer(ModEntities.LIGHT_CARRIER.get(), ctx -> new EpcaGeoRenderer<>(ctx, new LightCarrierModel()));
        event.registerEntityRenderer(ModEntities.INFESTED_FOX.get(), ctx -> new EpcaGeoRenderer<>(ctx, new InfestedFoxModel()));
        event.registerEntityRenderer(ModEntities.WALKING_FOX_HEAD.get(), ctx -> new EpcaGeoRenderer<>(ctx, new WalkingFoxHeadModel()));
        event.registerEntityRenderer(ModEntities.INFESTED_SKELETON.get(), ctx -> new EpcaGeoRenderer<>(ctx, new InfestedSkeletonModel()));
        event.registerEntityRenderer(ModEntities.WALKING_SKELETON_HEAD.get(), ctx -> new EpcaGeoRenderer<>(ctx, new WalkingSkeletonHeadModel()));

        event.registerEntityRenderer(ModEntities.THROWN_WOODEN_SPEAR.get(), ThrownWoodenSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_STONE_SPEAR.get(), ThrownStoneSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_FLINT_SPEAR.get(), ThrownFlintSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_COPPER_SPEAR.get(), ThrownCopperSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_IRON_SPEAR.get(), ThrownIronSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_GOLDEN_SPEAR.get(), ThrownGoldenSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_DIAMOND_SPEAR.get(), ThrownDiamondSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_NETHERITE_SPEAR.get(), ThrownNetheriteSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.ACID_BULLET.get(), AcidBulletRenderer::new);
        event.registerEntityRenderer(ModEntities.BONE_ARROW.get(), BoneArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.BIOMASS_EGG.get(), BiomassEggRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_PUMPKIN_HEAD.get(), InfestedPumpkinHeadRenderer::new);

        event.registerBlockEntityRenderer(ModBlockEntities.SWALLOW_CYST.get(), ctx -> new GeoBlockRenderer<>(new SwallowCystModel()));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.SWALLOW_CYST.get(),
                    new ResourceLocation(epca.MODID, "living"),
                    (stack, level, entity, seed) -> {
                        CompoundTag tag = stack.getTag();
                        if (tag != null && tag.contains("Living")) {
                            return tag.getBoolean("Living") ? 1.0f : 0.0f;
                        }
                        return 1.0f;
                    });
            ItemProperties.register(ModItems.INFESTED_SWEET_BERRY_BUSH.get(), new ResourceLocation("epca", "age"),
                    (stack, level, entity, seed) -> {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("age")) {
                    return tag.getInt("age");
                }
                return 1;
            });

            
            
            registerThrowingProperty(ModItems.WOODEN_SPEAR.get());
            registerThrowingProperty(ModItems.STONE_SPEAR.get());
            registerThrowingProperty(ModItems.FLINT_SPEAR.get());
            registerThrowingProperty(ModItems.COPPER_SPEAR.get());
            registerThrowingProperty(ModItems.IRON_SPEAR.get());
            registerThrowingProperty(ModItems.GOLDEN_SPEAR.get());
            registerThrowingProperty(ModItems.DIAMOND_SPEAR.get());
            registerThrowingProperty(ModItems.NETHERITE_SPEAR.get());
            }
        );
    }

    
    private static void registerThrowingProperty(Item item) {
        ItemProperties.register(item, new ResourceLocation(epca.MODID, "throwing"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    /**
     * Builds the custom core shader used by the shader-rendered gas clouds.
     *
     * <p>The shader assets live in {@code assets/epca/shaders/core/gas_cloud.json} (plus
     * {@code gas_cloud.vsh} / {@code gas_cloud.fsh}); the resulting {@link ShaderInstance} is handed
     * to {@link GasCloudRenderType}, which wraps it in a custom RenderType.</p>
     *
     * <p>This MUST be a mod-bus subscriber:
     * {@code net.minecraftforge.client.event.RegisterShadersEvent} implements
     * {@code net.minecraftforge.fml.event.IModBusEvent}, so it is only ever posted to the mod event
     * bus. Registering it on the forge bus would silently leave the shader unbuilt and the clouds
     * invisible.</p>
     */
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        ShaderInstance shader = new ShaderInstance(
                event.getResourceProvider(),
                epca.MODID + ":gas_cloud",
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        event.registerShader(shader, GasCloudRenderType::registerShader);
    }
}
