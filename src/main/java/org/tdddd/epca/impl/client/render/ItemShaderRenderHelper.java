package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;

/**
 * 把一个 {@link ItemLayerBinding} 真正画到屏幕上。
 *
 * <p>调用前必须保证基础物品模型已经渲染并 {@code endBatch()} 过，
 * 这样深度缓冲里才有物品轮廓，{@code EQUAL} 深度测试才能精确贴合。</p>
 */
public final class ItemShaderRenderHelper {

    private ItemShaderRenderHelper() {
    }

    /**
     * 绘制一个层。
     *
     * @param lateRender {@code true} 表示这是光影延迟回放（走 after_level RenderType）
     */
    public static void drawBinding(ItemLayerBinding binding, ItemStack stack, ItemDisplayContext ctx,
                                   PoseStack poseStack, MultiBufferSource buffer,
                                   int packedLight, int packedOverlay, boolean lateRender) {
        if (binding == null || stack == null || stack.isEmpty()) {
            return;
        }
        IItemShaderLayer layer = binding.layer();
        ItemLayerConfig config = binding.config();

        ShaderInstance shader = layer.shader();
        if (shader == null) {
            // shader 还没加载（资源重载中）——静默跳过，不影响原版渲染
            return;
        }

        if (!layer.prepare(stack, config, ctx, lateRender)) {
            return;
        }

        applyCustomUniforms(config, stack, shader);

        Minecraft mc = Minecraft.getInstance();
        TextureAtlas atlas = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);

        ResourceLocation mask = layer.maskTexture(stack, config);
        if (mask == null) {
            mask = ItemRenderRegistry.defaultMaskFor(stack);
        }
        TextureAtlasSprite sprite = atlas.getSprite(mask);

        ItemShaderRenderTypes types = layer.renderTypes();
        RenderType renderType = lateRender ? types.lateFor(ctx) : types.immediate();

        VertexConsumer consumer = buffer.getBuffer(renderType);
        mc.getItemRenderer().renderQuadList(poseStack, consumer, ItemShaderBakery.quads(sprite),
                stack, packedLight, packedOverlay);

        if (!lateRender && buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            // 每条独立 flush：不同物品的 uniform 值不同，不能合并批次
            bufferSource.endBatch(renderType);
        }
    }

    /**
     * 应用用户自定义 uniform（在本层默认 uniform 之后，因而可以覆盖）。
     */
    private static void applyCustomUniforms(ItemLayerConfig config, ItemStack stack, ShaderInstance shader) {
        var shaper = config.uniformShaper();
        if (shaper != null) {
            try {
                shaper.accept(stack, shader);
            } catch (Exception ignored) {
                // 自定义 uniform 出错不应影响渲染
            }
        }
    }

    /**
     * 便捷方法：由物品注册名推导“物品自己的贴图”作为遮罩。
     * 与 {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry#defaultMaskFor} 等价，这里再暴露一次方便层实现调用。
     */
    public static ResourceLocation itemTexture(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return new ResourceLocation("minecraft", "item/missingno");
        }
        return new ResourceLocation(itemId.getNamespace(), "item/" + itemId.getPath());
    }
}
