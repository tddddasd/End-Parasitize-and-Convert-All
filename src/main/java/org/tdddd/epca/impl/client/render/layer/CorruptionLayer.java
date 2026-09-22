package org.tdddd.epca.impl.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.client.render.IItemShaderLayer;
import org.tdddd.epca.impl.client.render.ItemLayerConfig;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;
import org.tdddd.epca.impl.client.render.ItemShaderRenderTypes;
import org.tdddd.epca.impl.client.render.shader.EpcaShaders;

/**
 * 崩坏渲染层（移植自 RottenRuinsSplendiding 的 corruption 层）。
 *
 * <h3>视觉构成</h3>
 * <ol>
 *   <li>RGB 色散分离（随时间旋转方向的色差）</li>
 *   <li>染色偏移 + 冷色去饱和</li>
 *   <li>细/粗双层扫描线</li>
 *   <li>横向故障条带位移</li>
 *   <li>颗粒噪声</li>
 *   <li>随机坏点 + 高亮闪烁像素</li>
 *   <li>暗角</li>
 *   <li>低频脉动暗波</li>
 * </ol>
 * 再加上可选的几何鬼畜抖动（{@link ItemLayerConfig#twitch()}）。
 *
 * <h3>强度来源</h3>
 * {@link CorruptionPulse#intensity} —— 默认是周期爆发；物品 NBT 里写了
 * {@code epca_corruption} 则改为常驻强度。最终强度再乘
 * {@link ItemLayerConfig#strength()}。
 *
 * <h3>用法</h3>
 * <pre>{@code
 * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION);
 *
 * ItemRenderRegistry.attach(ModItems.CLUSTER, ItemShaderLayers.CORRUPTION,
 *         ItemLayerConfig.builder().strength(0.8f).twitch(true).build());
 * }</pre>
 */
public final class CorruptionLayer implements IItemShaderLayer {

    public static final String NAME = "corruption";

    public static final CorruptionLayer INSTANCE = new CorruptionLayer();

    /** 默认染色：品红紫（与 RottenRuinsSplendiding 一致）。 */
    private static float defaultTintR = 0.55f;
    private static float defaultTintG = 0.08f;
    private static float defaultTintB = 0.50f;

    /** 默认色散倍率。 */
    private static float defaultSplitStrength = 1.0f;

    private CorruptionLayer() {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ShaderInstance shader() {
        return EpcaShaders.corruptionShader;
    }

    @Override
    public ItemShaderRenderTypes renderTypes() {
        return EpcaShaders.CORRUPTION_TYPES;
    }

    @Override
    public ResourceLocation maskTexture(ItemStack stack, ItemLayerConfig config) {
        if (config.maskOverride() != null) {
            return config.maskOverride();
        }
        // 默认用物品自己的贴图 —— 天然 1:1 贴合物品轮廓
        return ItemRenderRegistry.defaultMaskFor(stack);
    }

    @Override
    public boolean prepare(ItemStack stack, ItemLayerConfig config, ItemDisplayContext ctx, boolean lateRender) {
        Minecraft mc = Minecraft.getInstance();
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;

        float intensity = CorruptionPulse.intensity(stack, gameTime) * config.strength();
        if (intensity < 0.005f) {
            return false; // 爆发窗口之外，直接不画（省一次 draw call）
        }

        if (EpcaShaders.corruptionTimeUniform != null) {
            EpcaShaders.corruptionTimeUniform.set((float) (gameTime % Integer.MAX_VALUE));
        }
        if (EpcaShaders.corruptionIntensityUniform != null) {
            EpcaShaders.corruptionIntensityUniform.set(intensity);
        }
        if (EpcaShaders.corruptionTintUniform != null) {
            EpcaShaders.corruptionTintUniform.set(defaultTintR, defaultTintG, defaultTintB);
        }
        if (EpcaShaders.corruptionSplitUniform != null) {
            EpcaShaders.corruptionSplitUniform.set(defaultSplitStrength);
        }
        return true;
    }

    @Override
    public void applyTwitch(PoseStack poseStack, ItemStack stack, ItemLayerConfig config, long gameTime) {
        CorruptionPulse.applyTwitch(poseStack, stack, gameTime, config.strength());
    }

    // ── 全局默认外观 ─────────────────────────────────────────────────

    /** 改崩坏默认染色（0–1 的 RGB）。 */
    public static void setDefaultTint(float r, float g, float b) {
        defaultTintR = r;
        defaultTintG = g;
        defaultTintB = b;
    }

    /** 改崩坏默认色散倍率。 */
    public static void setDefaultSplitStrength(float value) {
        defaultSplitStrength = value;
    }
}
