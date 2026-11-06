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

public class InfestedSweetBerryBushGlowRenderer implements BlockEntityRenderer<InfestedSweetBerryBushBlockEntity> {

    private static final int RIPE_AGE = 3;

    private static final Direction[] DIRECTIONS = Direction.values();

    private static final int BLOCK_VERTEX_INTS = 8;

    private static final float V_OFFSET_UP_ONE_TEXEL = 1.0F / 16.0F;

    public InfestedSweetBerryBushGlowRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(InfestedSweetBerryBushBlockEntity bush, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = bush.getBlockState();

        if (!state.hasProperty(InfestedSweetBerryBush.AGE)
                || state.getValue(InfestedSweetBerryBush.AGE) < RIPE_AGE) {
            return;
        }

        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        VertexConsumer glow = bufferSource.getBuffer(EmissiveBlockRenderType.SWEET_BERRY_FRUIT);
        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create(bush.getBlockPos().asLong());

        for (Direction side : DIRECTIONS) {
            emit(model.getQuads(state, side, random), glow, pose);
        }
        emit(model.getQuads(state, null, random), glow, pose);
    }

    private static void emit(List<BakedQuad> quads, VertexConsumer glow, PoseStack.Pose pose) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            if (data.length != BLOCK_VERTEX_INTS * 4) {

                continue;
            }

            TextureAtlasSprite sprite = quad.getSprite();
            for (int i = 0; i < 4; i++) {
                int offset = i * BLOCK_VERTEX_INTS;
                float x = Float.intBitsToFloat(data[offset]);
                float y = Float.intBitsToFloat(data[offset + 1]);
                float z = Float.intBitsToFloat(data[offset + 2]);

                float u = sprite.getUOffset(Float.intBitsToFloat(data[offset + 4])) / 16.0F;
                float v = sprite.getVOffset(Float.intBitsToFloat(data[offset + 5])) / 16.0F;

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
