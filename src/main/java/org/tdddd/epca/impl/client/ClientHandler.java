package org.tdddd.epca.impl.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.model.*;
import org.tdddd.epca.impl.client.entity.renderer.*;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.gen.model.SwallowCystModel;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.Map;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {

        // ── Auto-registered renderers (all entities via EpcaEntityManager.registerMobWithRender) ──
        // These use EpcaTypeGeoRenderer + EpcaTypeGeoModel — reads model/texture/animation
        // from EpcaEntityManager by entity type. No per-entity model class needed.
        for (EntityType<?> type : EpcaEntityManager.consumeRenderTypes()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            EntityType rawType = type;
            event.registerEntityRenderer(rawType, EpcaGeoRenderer::new);//报错不用管，能跑就行
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
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
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

            // 长矛蓄力（投掷）时切换到 *_spear_throwing 模型：投掷姿势 THROW_TRIDENT
            // 会把手臂绕 X 轴转 180°，物品再绕 X 轴转 180° 抵消它，手持方向/位置与 26.1.2 版一致。
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

    /** 值为 1 表示“正在蓄力投掷这把长矛”（与原版三叉戟的 throwing 判定一致）。 */
    private static void registerThrowingProperty(Item item) {
        ItemProperties.register(item, new ResourceLocation(epca.MODID, "throwing"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }
}
