package org.tdddd.epca.impl.client.entity.gas;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.tdddd.epca.impl.epca;

import java.util.Optional;

public final class GasCloudRenderType extends RenderType {

    public static final ResourceLocation GAS_TEXTURE =
            new ResourceLocation(epca.MODID, "textures/particle/infestive_gas.png");

    public static final float GAS_TEXTURE_FRAMES = 9.0F;

    public static final float GAS_FRAME_INDEX = 0.0F;

    public static final float TINT_RED = 1.0F;
    public static final float TINT_GREEN = 0.33F;
    public static final float TINT_BLUE = 0.34F;

    public static final float SPEC_COLOR_RED = 0.42F;
    public static final float SPEC_COLOR_GREEN = 0.06F;
    public static final float SPEC_COLOR_BLUE = 0.06F;

    public static final int SPEC_STYLE_CHANNEL = 250;

    public static final int HEART_STYLE_CHANNEL = 252;

    public static final int HEART_MOTE_STYLE_CHANNEL = 253;

    public static final float SPEC_BASE_ALPHA = 1.0F;

    public static final float TINT_STRENGTH = 0.85F;

    public static final float MASK_STRENGTH = 1.0F;

    public static final float MASK_DEPTH = 0.5F;

    public static final float MIN_TEXTURE_ALPHA = 0.65F;

    public static final float MIN_NOISE_MASK = 0.6F;

    public static final float ALPHA_BOOST = 0.85F;

    public static final float RADIAL_PLATEAU = 0.55F;

    public static final float NOISE_UV_SCALE = 2.4F;

    public static final float DISTANCE_FADE_START = 48.0F;

    public static final float DISTANCE_FADE_END = 96.0F;

    public static final float SEED_SCALE = 0.004F;

    public static final float SUBQUAD_SEED_SCALE = 13.73F;

    public static final float SEED_PACK_SCALE = 250.0F;

    public static final int MAX_PACKED_UV2 = Short.MAX_VALUE;

    private static final int BUFFER_SIZE = RenderType.TRANSIENT_BUFFER_SIZE;
    private static final String RENDER_TYPE_NAME = "epca_gas_cloud";

    private static final String ADDITIVE_RENDER_TYPE_NAME = "epca_gas_cloud_additive";

    private static ShaderInstance gasCloudShader;

    private static boolean debugFlushed;

    private static final RenderStateShard.ShaderStateShard GAS_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(GasCloudRenderType::getShader);

    private static final RenderStateShard.TextureStateShard GAS_TEXTURE_STATE =
            new RenderStateShard.TextureStateShard(GAS_TEXTURE, false, false);

    private static final RenderType GAS_CLOUD = new GasCloudRenderType(RENDER_TYPE_NAME, false);

    private static final RenderType GAS_CLOUD_ADDITIVE =
            new GasCloudRenderType(ADDITIVE_RENDER_TYPE_NAME, true);

    private final int bufferSize;
    private final String renderTypeName;

    private GasCloudRenderType(String renderTypeName, boolean additive) {
        super(renderTypeName, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, BUFFER_SIZE, false, false,
                () -> {
                    if (GasCloudManager.DEBUG && !debugFlushed) {
                        debugFlushed = true;
                        epca.LOGGER.info("[gascloud] render type: first flush of {} - setup state ran "
                                        + "while shaderReady={}", renderTypeName, isShaderReady());
                    }
                    GAS_TEXTURE_STATE.setupRenderState();
                    GAS_SHADER_STATE.setupRenderState();

                    if (additive) {
                        ADDITIVE_TRANSPARENCY.setupRenderState();
                    } else {
                        TRANSLUCENT_TRANSPARENCY.setupRenderState();
                    }
                    LEQUAL_DEPTH_TEST.setupRenderState();

                    COLOR_WRITE.setupRenderState();
                    NO_CULL.setupRenderState();
                    NO_LIGHTMAP.setupRenderState();
                    NO_OVERLAY.setupRenderState();
                },
                () -> {
                    NO_OVERLAY.clearRenderState();
                    NO_LIGHTMAP.clearRenderState();
                    NO_CULL.clearRenderState();
                    COLOR_WRITE.clearRenderState();
                    LEQUAL_DEPTH_TEST.clearRenderState();
                    if (additive) {
                        ADDITIVE_TRANSPARENCY.clearRenderState();
                    } else {
                        TRANSLUCENT_TRANSPARENCY.clearRenderState();
                    }
                    GAS_SHADER_STATE.clearRenderState();
                    GAS_TEXTURE_STATE.clearRenderState();
                });
        this.bufferSize = BUFFER_SIZE;
        this.renderTypeName = renderTypeName;
    }

    public static void registerShader(ShaderInstance shader) {
        gasCloudShader = shader;
        if (GasCloudManager.DEBUG) {
            epca.LOGGER.info("[gascloud] shader: RegisterShadersEvent consumer ran, epca:gas_cloud ready={}",
                    shader != null);
        }
    }

    public static boolean isShaderReady() {
        return gasCloudShader != null;
    }

    public static RenderType get() {
        return GAS_CLOUD;
    }

    public static RenderType getAdditive() {
        return GAS_CLOUD_ADDITIVE;
    }

    public static float getFrameZeroVScale() {
        return 1.0F / GAS_TEXTURE_FRAMES;
    }

    public static int packSeed(float renderSeed) {
        int packed = (int) (renderSeed * SEED_PACK_SCALE);
        return Mth.clamp(packed, 0, MAX_PACKED_UV2);
    }

    private static ShaderInstance getShader() {
        return gasCloudShader;
    }

    @Override
    public int bufferSize() {
        return this.bufferSize;
    }

    @Override
    public VertexFormat format() {
        return DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;
    }

    @Override
    public VertexFormat.Mode mode() {
        return VertexFormat.Mode.QUADS;
    }

    @Override
    public boolean canConsolidateConsecutiveGeometry() {
        return false;
    }

    @Override
    public Optional<RenderType> outline() {
        return Optional.empty();
    }

    @Override
    public boolean isOutline() {
        return false;
    }

    @Override
    public boolean affectsCrumbling() {
        return false;
    }

    @Override
    public String toString() {
        return this.renderTypeName;
    }
}
