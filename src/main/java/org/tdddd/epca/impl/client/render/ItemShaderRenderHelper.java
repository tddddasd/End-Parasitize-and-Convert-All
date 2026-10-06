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
 *  {@link ItemLayerBinding}
 *
 * <p> {@code endBatch()}
 * {@code EQUAL} </p>
 */
public final class ItemShaderRenderHelper {

    private ItemShaderRenderHelper() {
    }

    /**
     *
     *
     * @param lateRender {@code true}  after_level RenderType
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
            // shader
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
            //  flush uniform
            bufferSource.endBatch(renderType);
        }
    }

    /**
     *  uniform uniform
     */
    private static void applyCustomUniforms(ItemLayerConfig config, ItemStack stack, ShaderInstance shader) {
        var shaper = config.uniformShaper();
        if (shaper != null) {
            try {
                shaper.accept(stack, shader);
            } catch (Exception ignored) {
                //  uniform
            }
        }
    }

    /**
     *
     *  {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry#defaultMaskFor}
     */
    public static ResourceLocation itemTexture(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return new ResourceLocation("minecraft", "item/missingno");
        }
        return new ResourceLocation(itemId.getNamespace(), "item/" + itemId.getPath());
    }
}

