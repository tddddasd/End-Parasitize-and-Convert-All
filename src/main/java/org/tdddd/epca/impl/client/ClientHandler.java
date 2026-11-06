package org.tdddd.epca.impl.client;

import com.geckolib.renderer.GeoBlockRenderer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.client.block.InfestedSweetBerryBushGlowRenderer;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.gas.GasCloudRenderType;
import org.tdddd.epca.impl.client.entity.model.*;
import org.tdddd.epca.impl.client.entity.renderer.*;
import org.tdddd.epca.impl.client.render.EmissiveBlockRenderType;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.gen.model.SwallowCystModel;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModItems;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {

        for (EntityType<?> type : EpcaEntityManager.consumeRenderTypes()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            EntityType rawType = type;
            event.registerEntityRenderer(rawType, EpcaGeoRenderer::new);
        }

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

        event.registerEntityRenderer(ModEntities.INFESTED_SPIDER.get(), ctx -> new EpcaGeoRenderer<>(ctx, new InfestedSpiderModel()));
        event.registerEntityRenderer(ModEntities.WALKING_SPIDER_HEAD.get(), ctx -> new EpcaGeoRenderer<>(ctx, new WalkingSpiderHeadModel()));
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

        event.registerBlockEntityRenderer(ModBlockEntities.SWALLOW_CYST.get(),
                ctx -> new GeoBlockRenderer<>(ctx, new SwallowCystModel()));

        event.registerBlockEntityRenderer(ModBlockEntities.INFESTED_SWEET_BERRY_BUSH.get(),
                InfestedSweetBerryBushGlowRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterItemModelProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(epca.MODID, "living"), LivingProperty.CODEC);
        event.register(Identifier.fromNamespaceAndPath(epca.MODID, "age"), AgeProperty.CODEC);
    }

    public record LivingProperty() implements RangeSelectItemModelProperty {
        public static final MapCodec<LivingProperty> CODEC = MapCodec.unit(new LivingProperty());

        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!tag.isEmpty() && tag.contains("Living")) {
                return tag.getBooleanOr("Living", false) ? 1.0F : 0.0F;
            }

            return 0.0F;
        }

        @Override
        public MapCodec<? extends RangeSelectItemModelProperty> type() {
            return CODEC;
        }
    }

    public record AgeProperty() implements RangeSelectItemModelProperty {
        public static final MapCodec<AgeProperty> CODEC = MapCodec.unit(new AgeProperty());

        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!tag.isEmpty() && tag.contains("age")) {
                return tag.getIntOr("age", 0);
            }
            return 1.0F;
        }

        @Override
        public MapCodec<? extends RangeSelectItemModelProperty> type() {
            return CODEC;
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {

        GasCloudRenderType.registerPipeline(event);

        org.tdddd.epca.impl.client.render.sky.SkyRuptureShaders.registerPipeline(event);

        org.tdddd.epca.impl.client.render.ItemShaderPipelines.registerPipeline(event);

        org.tdddd.epca.impl.client.render.ritual.RitualQuadRenderType.registerPipeline(event);

        org.tdddd.epca.impl.client.render.araya.ArayaSlashRenderType.registerPipeline(event);

        org.tdddd.epca.impl.client.render.araya.ArayaFireRenderType.registerPipeline(event);

        EmissiveBlockRenderType.registerPipeline(event);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

        event.enqueueWork(() -> {
        });
    }
}
