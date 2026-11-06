package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.tdddd.epca.impl.epca;

import java.util.function.Supplier;

public abstract class ItemShaderRenderTypes extends RenderType {

    private static final RenderStateShard.LayeringStateShard LAYER_DEPTH_BIAS =
            new RenderStateShard.LayeringStateShard(
                    "epca_item_layer_depth_bias",
                    () -> {
                        RenderSystem.polygonOffset(-1.0F, -32.0F);
                        RenderSystem.enablePolygonOffset();
                    },
                    () -> {
                        RenderSystem.polygonOffset(0.0F, 0.0F);
                        RenderSystem.disablePolygonOffset();
                    }
            );

    private static final int BUFFER_SIZE = 2097152;

    private final RenderType immediate;
    private final RenderType afterLevel;
    private final RenderType handAfterLevel;

    private ItemShaderRenderTypes(String name, Supplier<ShaderInstance> shader, boolean affectsCrumbling) {

        super(epca.MODID + ":item_layer/" + name + "_holder",
                DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, BUFFER_SIZE,
                false, false, () -> {
                }, () -> {
                });

        RenderStateShard.ShaderStateShard shaderState = new RenderStateShard.ShaderStateShard(shader);

        this.immediate = RenderType.create(
                epca.MODID + ":item_layer/" + name,
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                affectsCrumbling,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setDepthTestState(EQUAL_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(BLOCK_SHEET)
                        .createCompositeState(true)
        );

        this.afterLevel = RenderType.create(
                epca.MODID + ":item_layer/" + name + "_after_level",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(BLOCK_SHEET)
                        .setLayeringState(LAYER_DEPTH_BIAS)
                        .setOutputState(MAIN_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );

        this.handAfterLevel = RenderType.create(
                epca.MODID + ":item_layer/" + name + "_hand_after_level",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(BLOCK_SHEET)
                        .setOutputState(MAIN_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
    }

    public static ItemShaderRenderTypes create(String name, Supplier<ShaderInstance> shader, boolean affectsCrumbling) {
        return new ItemShaderRenderTypes(name, shader, affectsCrumbling) {
        };
    }

    public RenderType immediate() {
        return immediate;
    }

    public RenderType afterLevel() {
        return afterLevel;
    }

    public RenderType handAfterLevel() {
        return handAfterLevel;
    }

    public RenderType lateFor(ItemDisplayContext context) {
        return isFirstPersonHand(context) ? handAfterLevel : afterLevel;
    }

    public static boolean isFirstPersonHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(epca.MODID, path);
    }
}
