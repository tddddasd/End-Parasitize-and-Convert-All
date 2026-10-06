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

/**
 * <b></b>
 *
 * <h2></h2>
 * <p> {@code age == 3}<b></b>
 * {@link EmissiveBlockRenderType#sweetBerryFruit()}  +
 * {@code infested_sweet_berry_bush_growed_e}
 * alpha <b></b>
 * {@code age = 0/1/2}</p>
 *
 * <h2> BlockEntityRenderer </h2>
 * <p> {@link EmissiveBlockRenderType}
 * " + " {@code render_type}
 *
 * {@link InfestedSweetBerryBushBlockEntity} {@code ModBlockEntities.INFESTED_SWEET_BERRY_BUSH}
 *  /  / </p>
 *
 * <h2>1.20.1 -&gt; 26.1.2 </h2>
 * <ul>
 *   <li>1.20.1  {@code BlockEntityRenderer#render(be, partialTick, poseStack, bufferSource, light, overlay)}
 *       26.1.2  {@code createRenderState} / {@code extractRenderState} +
 *       {@code submit(state, poseStack, submitNodeCollector, camera)}{@code submit}
 *        {@code extractRenderState} </li>
 *   <li>1.20.1  {@code Minecraft#getBlockRenderer()#getBlockModel(state)} +
 *       {@code BakedModel#getQuads(state, direction, random)} 26.1.2
 *       {@code ModelManager#getBlockStateModelSet()#get(state)}  {@code BlockStateModel}
 *        {@code collectParts(random, list)}  {@code BlockStateModelPart}
 *       {@code part.getQuads(direction)}  {@code BakedQuad}
 *        {@code ArayaFireRenderer}{@code InfestedPumpkinPhysicsRenderer} </li>
 *   <li>1.20.1  {@code MultiBufferSource#getBuffer(renderType)} 26.1.2
 *       {@code SubmitNodeCollector#submitCustomGeometry(poseStack, renderType, (pose, consumer) -> ...)}
 *        {@code VertexConsumer}  buffer</li>
 *   <li>{@code BakedQuad} "int[8]  + {@code getSprite()}"
 *       "{@code position(i)} + {@code packedUV(i)} + {@code materialInfo().sprite()}"
 *       UV "" {@code setUv}  0..1
 *       {@code UVPair.unpackU/unpackV} sprite  0..1
 *       {@code TextureAtlasSprite#getU0/getU1/getV0/getV1}1.20.1
 *       {@code getUOffset/getVOffset}  26.1.2  {@code getU/getV} "sprite
 *       "{@code u0 + (u1-u0)*offset}</li>
 *   <li>{@code LightTexture.FULL_BRIGHT}int  {@code Brightness.FULL_BRIGHT.pack()}
 *       {@code ArayaFireRenderer} {@code OverlayTexture.NO_OVERLAY} </li>
 * </ul>
 *
 * <h2></h2>
 * <p><b></b> {@code BakedQuad}
 * UV  45  UV
 * <b></b><b> sprite  0..1</b> {@code _growed}  {@code _growed_e}
 *  16x16 sprite  0..1  0..1</p>
 *
 * <h2>""</h2>
 * <p>{@code minecraft:block/cross}
 * {@code north}+{@code south}{@code west}+{@code east}uv  u
 * {@code cutout}  {@code NO_CULL}
 * <b></b>
 *  {@code CULL} {@link EmissiveBlockRenderType}</p>
 *
 * <p><b></b>{@code getQuads(null)}
 *  cullface6 </p>
 */
public class InfestedSweetBerryBushGlowRenderer
        implements BlockEntityRenderer<InfestedSweetBerryBushBlockEntity, InfestedSweetBerryBushGlowRenderer.State> {

    /** vanilla  {@code age=3} 3  {@code _growed}  */
    private static final int RIPE_AGE = 3;

    /**
     *
     *
     * <p>{@code WeightedVariants}  {@code collectParts}
     * {@link RandomSource}</p>
     */
    private static final long MODEL_SEED = 42L;

    public InfestedSweetBerryBushGlowRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /**
     * "" {@code AGE}
     *  {@code submit}  {@link InfestedSweetBerryBushBlockEntity}
     */
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

    /**
     * 1  = 1.0
     *
     * <p> vanilla  6  {@code null}
     * {@code QuadCollection}  {@code null}  unculled
     *  culled{@code age=3}
     * {@code epca:block/infested_sweet_berry_bush_age2}  {@code minecraft:block/cross}
     *  cullface unculled 6
     *  {@code BakedQuad} </p>
     */
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

    /**
     *  {@code BakedQuad} ""
     *
     * <p>26.1.2  {@code BakedQuad}  {@code Vector3fc} + {@code packedUV} +
     * {@code materialInfo().sprite()} 26.1.2
     *  sprite  UV </p>
     */
    private static void append(List<BakedQuadView> out, List<net.minecraft.client.resources.model.geometry.BakedQuad> quads) {
        for (net.minecraft.client.resources.model.geometry.BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            if (sprite == null) {
                continue;
            }
            out.add(new BakedQuadView(quad, sprite));
        }
    }

    /**
     *
     *
     * <p>/uv u
     * {@link EmissiveBlockRenderType}  {@code cutout}
     * ""</p>
     *
     * @param quads    1  = 1.0
     * @param glow
     * @param pose
     */
    private static void emit(List<BakedQuadView> quads, VertexConsumer glow, PoseStack.Pose pose) {
        for (BakedQuadView view : quads) {
            var quad = view.quad();
            TextureAtlasSprite sprite = view.sprite();
            float uSpan = sprite.getU1() - sprite.getU0();
            float vSpan = sprite.getV1() - sprite.getV0();
            for (int i = 0; i < 4; i++) {
                var position = quad.position(i);
                long packedUv = quad.packedUV(i);
                //  ->  sprite  0..10..1
                float u = uSpan == 0.0F ? 0.0F : (UVPair.unpackU(packedUv) - sprite.getU0()) / uSpan;
                float v = vSpan == 0.0F ? 0.0F : (UVPair.unpackV(packedUv) - sprite.getV0()) / vSpan;
                //  1  PNG
                //  PNG  16x16
                glow.addVertex(pose, position.x(), position.y(), position.z())
                        .setColor(0xFFFFFFFF)
                        .setUv(u, v)
                        .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                        .setLight(Brightness.FULL_BRIGHT.pack())
                        .setNormal(pose, 0.0F, 1.0F, 0.0F);
            }
        }
    }

    /**  +  spriteUV  */
    private record BakedQuadView(net.minecraft.client.resources.model.geometry.BakedQuad quad,
                                 TextureAtlasSprite sprite) {
    }

    /** 26.1.2  {@code submit}  */
    public static class State extends BlockEntityRenderState {
        /** {@code AGE >= 3} */
        public boolean ripe;
        /**  {@link #ripe}  */
        public List<BakedQuadView> quads = List.of();
    }

    /**  {@link Identifier} */
    public static Identifier fruitTexture() {
        return EmissiveBlockRenderType.SWEET_BERRY_FRUIT_TEXTURE;
    }
}

