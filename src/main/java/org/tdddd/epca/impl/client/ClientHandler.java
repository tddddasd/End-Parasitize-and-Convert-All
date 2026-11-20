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
import org.tdddd.epca.impl.client.block.InfestedSweetBerryBushGlowRenderer;
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

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {

        for (EntityType<?> type : EpcaEntityManager.consumeRenderTypes()) {
            if (type == ModEntities.RESHAPE_YELLOWEYE.get()) {

                continue;
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            EntityType rawType = type;
            event.registerEntityRenderer(rawType, EpcaGeoRenderer::new);
        }

        event.registerEntityRenderer(ModEntities.CONTAMINATED_WATER.get(), ContaminatedWaterRenderer::new);
        event.registerEntityRenderer(ModEntities.BONE_FRAGMENT.get(), BoneFragmentRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_ENDER_PEARL.get(), InfestedThrownEnderPearlRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_SPIDER_WEB_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_SPIDER_WEB_BLOOD_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_CAVE_SPIDER_WEB_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_ENDERMAN.get(), InfestedEndermanRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_ENDERMITE.get(), InfestedEndermiteRenderer::new);
        event.registerEntityRenderer(ModEntities.WALKING_ENDERMAN_HEAD.get(), WalkingEndermanHeadRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_ZOMBIE.get(), InfestedZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), org.tdddd.epca.impl.client.blackhole.BlackHoleEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ILLUSORY_BUTTERFLY.get(), org.tdddd.epca.impl.client.illusion.IllusoryButterflyRenderer::new);
        event.registerEntityRenderer(ModEntities.ILLUSORY_SHADOW.get(), org.tdddd.epca.impl.client.illusion.IllusoryShadowRenderer::new);
        event.registerEntityRenderer(ModEntities.RESHAPE_LONGARMS.get(), ReshapeLongarmsRenderer::new);
        event.registerEntityRenderer(ModEntities.RESHAPE_YELLOWEYE.get(), ReshapeYelloweyeRenderer::new);
        event.registerEntityRenderer(ModEntities.INFESTED_BAT.get(), InfestedBatRenderer::new);

        event.registerEntityRenderer(ModEntities.RIPPER.get(), ctx -> new EpcaGeoRenderer<>(ctx, new RipperModel()));
        event.registerEntityRenderer(ModEntities.FINS.get(), ctx -> new EpcaGeoRenderer<>(ctx, new FinsModel()));
        event.registerEntityRenderer(ModEntities.LIGHT_CARRIER.get(), ctx -> new EpcaGeoRenderer<>(ctx, new LightCarrierModel()));
        event.registerEntityRenderer(ModEntities.INFESTED_FOX.get(), ctx -> new EpcaGeoRenderer<>(ctx, new InfestedFoxModel()));
        event.registerEntityRenderer(ModEntities.WALKING_FOX_HEAD.get(), ctx -> new EpcaGeoRenderer<>(ctx, new WalkingFoxHeadModel()));
        event.registerEntityRenderer(ModEntities.INFESTED_SKELETON.get(), ctx -> new EpcaGeoRenderer<>(ctx, new InfestedSkeletonModel()));
        event.registerEntityRenderer(ModEntities.WALKING_SKELETON_HEAD.get(), ctx -> new EpcaGeoRenderer<>(ctx, new WalkingSkeletonHeadModel()));

        event.registerEntityRenderer(ModEntities.INFESTED_SPIDER.get(), ctx -> new EpcaGeoRenderer<>(ctx, new InfestedSpiderModel()));
        event.registerEntityRenderer(ModEntities.WALKING_SPIDER_HEAD.get(), ctx -> new EpcaGeoRenderer<>(ctx, new WalkingSpiderHeadModel()));

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

        event.registerBlockEntityRenderer(ModBlockEntities.SWALLOW_CYST.get(), ctx -> new GeoBlockRenderer<>(new SwallowCystModel()));

        event.registerBlockEntityRenderer(ModBlockEntities.INFESTED_SWEET_BERRY_BUSH.get(),
                InfestedSweetBerryBushGlowRenderer::new);
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
            // 虫染甜浆果丛只有两个生长阶段（age 0/1），物品模型 override 也按 0..1 匹配（见
            // assets/epca/models/item/infested_sweet_berry_bush.json 的 epca:age == 1）。
            // 物品栈没有 age 标签时返回 0（基础模型），不会误命中成熟模型。
            ItemProperties.register(ModItems.INFESTED_SWEET_BERRY_BUSH.get(), new ResourceLocation("epca", "age"),
                    (stack, level, entity, seed) -> {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("age")) {
                    return tag.getInt("age") >= 1 ? 1.0F : 0.0F;
                }
                return 0.0F;
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
     * 给原版玩家渲染器挂「内器官模型」渲染层。
     *
     * <p>选择 RenderLayer（而不是 mixin 到 PlayerRenderer / HumanoidModel）：</p>
     * <ul>
     *   <li>AddLayers 事件是 Forge 官方扩展点，不修改原版类，侵入性最小；</li>
     *   <li>layer 对每个玩家实例都执行，自己（第三人称）和其他玩家走同一份逻辑，
     *       显示数据来自服务端同步，因此"自己和其他玩家都能看到"是同一条渲染路径；</li>
     *   <li>人身模型（第一层皮肤）不删除，只用深红四边形覆盖正面窗口，
     *       第二层皮肤（帽子 / 外套，半径更大）依旧盖在深红层外面。</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer == null) {
                continue;
            }
            renderer.addLayer(new org.tdddd.epca.impl.client.organ.model.OrganModelRenderLayer(renderer));
        }
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        ShaderInstance shader = new ShaderInstance(
                event.getResourceProvider(),
                epca.MODID + ":gas_cloud",
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        event.registerShader(shader, GasCloudRenderType::registerShader);

    }

}
