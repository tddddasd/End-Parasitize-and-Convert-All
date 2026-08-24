package org.tdddd.epca.impl.overworld.registry.blocks.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.PackedMudPedestalBlockEntity;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ItemDisplayRenderer implements BlockEntityRenderer<PackedMudPedestalBlockEntity> {
    private final ItemRenderer itemRenderer;

    public ItemDisplayRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }


    @Override
    public void render(PackedMudPedestalBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        long gameTime = Minecraft.getInstance().level.getGameTime();
        if (blockEntity.hasItem()) {
            ItemStack itemStack = blockEntity.getItem();
            float rotation = (gameTime + partialTick) * 2.0f;

            poseStack.pushPose();
            poseStack.translate(0.5D, 1.2D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            poseStack.scale(1.0F, 1.0F, 1.0F);

            itemRenderer.renderStatic(itemStack, ItemDisplayContext.GROUND, packedLight,
                    OverlayTexture.NO_OVERLAY, poseStack, bufferSource, blockEntity.getLevel(), 0);

            poseStack.popPose();
        }
        // 渲染过滤标记：当filterData非空时，在四个侧面显示小型物品展示框（静止）
        List<String> filterData = blockEntity.getFilterData();
        if (!filterData.isEmpty()) {
            // 构造小型物品展示框物品栈（优先使用自定义的小型物品展示框，否则回退到普通物品展示框）
            ItemStack filterStack = new ItemStack(ModItems.SMALL_ITEM_FRAME.get(), 1);
            if (!filterStack.isEmpty()) {
                Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                for (Direction dir : directions) {
                    renderFilterItemOnSide(poseStack, bufferSource, packedLight, filterStack, dir, level);
                }
            }
        }
    }

    /**
     * 在指定方向的外侧渲染一个物品，使其正面朝向该方向，位置在顶部平台侧面外1像素处，且向下偏移1像素。
     */
    private void renderFilterItemOnSide(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                        ItemStack stack, Direction direction, Level level) {
        double y = 0.6875; // 调整后
        // 水平偏移：从中心向外6像素（5像素平台外表面 + 1像素额外偏移）
        double offset = 0.34375;

        double xOff = 0.0, zOff = 0.0;
        switch (direction) {
            case NORTH: zOff = -offset; break;
            case SOUTH: zOff =  offset; break;
            case EAST:  xOff =  offset; break;
            case WEST:  xOff = -offset; break;
            default: return;
        }

        // 旋转使物品立起并面向外部
        float angle;
        switch (direction) {
            case NORTH: angle = 0;   break;
            case SOUTH: angle = 180; break;
            case EAST:  angle = 90;  break;
            case WEST:  angle = 270; break;
            default: angle = 0;
        }

        poseStack.pushPose();
        poseStack.translate(0.5 + xOff, y, 0.5 + zOff);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level, 0);
        poseStack.popPose();
    }
}