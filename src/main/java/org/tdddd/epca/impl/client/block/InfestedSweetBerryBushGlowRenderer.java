package org.tdddd.epca.impl.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.client.render.EmissiveBlockRenderType;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSweetBerryBush;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.InfestedSweetBerryBushBlockEntity;

import java.util.List;

/**
 * <b></b>
 *
 * <h2></h2>
 * <p> {@code age == 3}<b></b>
 * {@link EmissiveBlockRenderType#SWEET_BERRY_FRUIT}  +
 * {@code infested_sweet_berry_bush_growed_e}
 * alpha <b></b>
 * {@code age = 0/1/2}</p>
 *
 * <h2> BlockEntityRenderer </h2>
 * <p>1.20.1 {@code RenderType.chunkBufferLayers()}
 * {@code LIGHTMAP}
 * " + " {@code render_type}
 *  {@link InfestedSweetBerryBushBlockEntity}
 *  {@code ModBlockEntities.INFESTED_SWEET_BERRY_BUSH}
 *  /  / </p>
 *
 * <h2></h2>
 * <p><b></b>
 * {@link BakedQuad} UV  45
 *  UV <b></b><b> sprite  0..1</b>
 * {@code _growed}  {@code _growed_e}  16x16 sprite
 * 0..1  0..1</p>
 *
 * <h2>""</h2>
 * <p>{@code minecraft:block/cross}
 * {@code north}+{@code south}{@code west}+{@code east}uv  u
 * {@code cutout}  {@code NO_CULL}
 * <b></b>
 *  {@code CULL} {@link EmissiveBlockRenderType}
 * ""</p>
 *
 * <p><b></b>{@code getQuads(state, null, )}
 *  cullface6  {@code SimpleBakedModel} </p>
 */
public class InfestedSweetBerryBushGlowRenderer implements BlockEntityRenderer<InfestedSweetBerryBushBlockEntity> {

    /** vanilla  {@code age=3} 3  {@code _growed}  */
    private static final int RIPE_AGE = 3;

    private static final Direction[] DIRECTIONS = Direction.values();

    /**
     * {@code DefaultVertexFormat.BLOCK}  8  int
     * Position(3f) / Color(1 int) / UV0(2f) / UV2(2 shorts) / Normal+Padding(1 int)
     *  0..2  int  4..5  UV0
     */
    private static final int BLOCK_VERTEX_INTS = 8;

    /**
     *  V <b> 1 </b>1/16
     *
     * <h2></h2>
     * <p> UV <b></b>{@code getVOffset}  vanilla
     * {@link TextureAtlasSprite#getV(double)}
     * {@code TextureAtlasSprite.java:84-87}  {@code getV(t) = v0 + (v1 - v0) * t / 16}
     *  {@code getVOffset(V) = (V - v0) / (v1 - v0) * 16}{@code :89-92}
     * {@code BakedQuad}  V " sprite  0..16 " {@code /16}
     *  0..1{@code FaceBakery.fillVertex}{@code FaceBakery.java:139-140}
     * {@code sprite.getV( uv)} vanilla
     * {@code VertexConsumer.putBulkData}  {@code :107-112} sprite
     * "" {@link BakedQuad}
     * /</p>
     *
     * <p><b></b>
     * {@code infested_sweet_berry_bush_growed_e.png}
     * {@code infested_sweet_berry_bush_growed.png}  1
     *  1 <b></b>
     *  PNG 1 </p>
     *
     * <h2></h2>
     * <p> {@code V * 16}V V=0  PNG  0
     *  V  1/16 "" 1
     *  1/16 </p>
     *
     * <p><b></b> 1/16
     *  {@link EmissiveBlockRenderType}  {@code VIEW_OFFSET_Z_LAYERING}
     * </p>
     *
     * <h2></h2>
     * <p> 1 <b></b>
     *  PNG  16x16  nudge
     * ""<b></b>
     * {@code v -= V_OFFSET_UP_ONE_TEXEL} </p>
     */
    private static final float V_OFFSET_UP_ONE_TEXEL = 1.0F / 16.0F;

    public InfestedSweetBerryBushGlowRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(InfestedSweetBerryBushBlockEntity bush, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = bush.getBlockState();
        // ""
        if (!state.hasProperty(InfestedSweetBerryBush.AGE)
                || state.getValue(InfestedSweetBerryBush.AGE) < RIPE_AGE) {
            return;
        }

        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        VertexConsumer glow = bufferSource.getBuffer(EmissiveBlockRenderType.SWEET_BERRY_FRUIT);
        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create(bush.getBlockPos().asLong());

        //  vanilla  6  null
        // ModelBlockRenderer#renderModel :241-247ForgeModelBlockRenderer#render :78-108
        //  null
        // SimpleBakedModel#getQuads :86-88  null  unculledFaces
        //  culledFaces.get(dir)age=3  epca:block/infested_sweet_berry_bush_age2
        //  minecraft:block/cross cullfaceElementsModel#addQuads :61-64
        //  addUnculledFace null 6
        //  BakedQuad
        for (Direction side : DIRECTIONS) {
            emit(model.getQuads(state, side, random), glow, pose);
        }
        emit(model.getQuads(state, null, random), glow, pose);
    }

    /**
     *
     *
     * <p> {@link BakedQuad} /uv u
     *  {@link EmissiveBlockRenderType#SWEET_BERRY_FRUIT}
     *  {@code cutout} ""
     *  {@link EmissiveBlockRenderType} ""</p>
     *
     * @param quads 1  = 1.0
     *               {@code null} unculled
     * @param glow  {@link EmissiveBlockRenderType#SWEET_BERRY_FRUIT}
     *              {@code NEW_ENTITY}
     * @param pose
     */
    private static void emit(List<BakedQuad> quads, VertexConsumer glow, PoseStack.Pose pose) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            if (data.length != BLOCK_VERTEX_INTS * 4) {
                //  BLOCK
                continue;
            }

            TextureAtlasSprite sprite = quad.getSprite();
            for (int i = 0; i < 4; i++) {
                int offset = i * BLOCK_VERTEX_INTS;
                float x = Float.intBitsToFloat(data[offset]);
                float y = Float.intBitsToFloat(data[offset + 1]);
                float z = Float.intBitsToFloat(data[offset + 2]);
                //  UV -> sprite  0..16getUOffset  *16-> 0..1
                float u = sprite.getUOffset(Float.intBitsToFloat(data[offset + 4])) / 16.0F;
                float v = sprite.getVOffset(Float.intBitsToFloat(data[offset + 5])) / 16.0F;
                //  1  V_OFFSET_UP_ONE_TEXEL
                // PNG  16x16

                // NEW_ENTITY Position -> Color -> UV0 -> UV1(overlay) -> UV2(light) -> Normal
                glow.vertex(pose.pose(), x, y, z)
                        .color(1.0F, 1.0F, 1.0F, 1.0F)
                        .uv(u, v)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(LightTexture.FULL_BRIGHT)
                        .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                        .endVertex();
            }
        }
    }
}

