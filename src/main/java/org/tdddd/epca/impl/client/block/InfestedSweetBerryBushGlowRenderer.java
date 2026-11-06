package org.tdddd.epca.impl.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Brightness;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.tdddd.epca.impl.client.render.EmissiveBlockRenderType;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSweetBerryBush;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.InfestedSweetBerryBushBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class InfestedSweetBerryBushGlowRenderer
        implements BlockEntityRenderer<InfestedSweetBerryBushBlockEntity, InfestedSweetBerryBushGlowRenderer.State> {

    private static final int RIPE_AGE = 3;

    private static final long MODEL_SEED = 42L;

    public InfestedSweetBerryBushGlowRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(InfestedSweetBerryBushBlockEntity bush, State state, float partialTick,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(bush, state, breakProgress);
        BlockState blockState = bush.getBlockState();
        state.ripe = blockState.hasProperty(InfestedSweetBerryBush.AGE)
                && blockState.getValue(InfestedSweetBerryBush.AGE) >= RIPE_AGE;
        state.quads = state.ripe ? collectQuads(blockState) : List.of();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        if (!state.ripe || state.quads.isEmpty()) {
            return;
        }
        RenderType renderType = EmissiveBlockRenderType.sweetBerryFruit();
        if (renderType == null) {
            return;
        }
        List<BakedQuadView> quads = state.quads;
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> emit(quads, consumer, pose));
    }

    private static List<BakedQuadView> collectQuads(BlockState blockState) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
        if (model == null) {
            return List.of();
        }
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(MODEL_SEED), parts);
        if (parts.isEmpty()) {
            return List.of();
        }
        List<BakedQuadView> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            append(quads, part.getQuads(null));
            for (Direction side : Direction.values()) {
                append(quads, part.getQuads(side));
            }
        }
        return quads;
    }

    private static void append(List<BakedQuadView> out, List<net.minecraft.client.resources.model.geometry.BakedQuad> quads) {
        for (net.minecraft.client.resources.model.geometry.BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            if (sprite == null) {
                continue;
            }
            out.add(new BakedQuadView(quad, sprite));
        }
    }

    private static void emit(List<BakedQuadView> quads, VertexConsumer glow, PoseStack.Pose pose) {
        for (BakedQuadView view : quads) {
            var quad = view.quad();
            TextureAtlasSprite sprite = view.sprite();
            float uSpan = sprite.getU1() - sprite.getU0();
            float vSpan = sprite.getV1() - sprite.getV0();
            for (int i = 0; i < 4; i++) {
                var position = quad.position(i);
                long packedUv = quad.packedUV(i);

                float u = uSpan == 0.0F ? 0.0F : (UVPair.unpackU(packedUv) - sprite.getU0()) / uSpan;
                float v = vSpan == 0.0F ? 0.0F : (UVPair.unpackV(packedUv) - sprite.getV0()) / vSpan;

                glow.addVertex(pose, position.x(), position.y(), position.z())
                        .setColor(0xFFFFFFFF)
                        .setUv(u, v)
                        .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                        .setLight(Brightness.FULL_BRIGHT.pack())
                        .setNormal(pose, 0.0F, 1.0F, 0.0F);
            }
        }
    }

    private record BakedQuadView(net.minecraft.client.resources.model.geometry.BakedQuad quad,
                                 TextureAtlasSprite sprite) {
    }

    public static class State extends BlockEntityRenderState {

        public boolean ripe;

        public List<BakedQuadView> quads = List.of();
    }

    public static Identifier fruitTexture() {
        return EmissiveBlockRenderType.SWEET_BERRY_FRUIT_TEXTURE;
    }
}
