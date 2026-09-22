package org.tdddd.epca.impl.client.render.sky;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import org.tdddd.epca.impl.epca;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 世界结界破损着色器的注册与 uniform 句柄。
 *
 * <h3>星点粒子来自哪里</h3>
 * 参考项目 RottenRuinsSplendiding 的宇宙渲染不做程序化星星，而是把 12 张
 * <b>带动画的星点贴图</b>（{@code hall:shader/cosmic_0..11}）放进方块图集，
 * 每帧把 12 个 UV 矩形作为 {@code mat2 cosmicuvs[12]} 传给着色器，
 * 着色器再按"多层球面壳 + 网格哈希"把这些星点铺到天空上。
 *
 * <p>本项目把那 12 张贴图原样复制到
 * {@code assets/epca/textures/shader/cosmic_0..11.png}（含 .mcmeta 动画），
 * 并通过 {@code assets/minecraft/atlases/blocks.json} 的目录源注册进图集，
 * 于是这一整套逻辑与参考项目完全一致。</p>
 *
 * <h3>uniform 声明里的坑</h3>
 * {@code cosmicuvs} 在 JSON 里必须写成
 * {@code "type":"matrix2x2","count":48}：Forge 的 {@code Uniform} 对矩阵类型走
 * {@code glUniformMatrix2fv(location, transpose, FloatBuffer)}，
 * <b>矩阵个数由 buffer 长度推导</b>（48 / 4 = 12 个 mat2）。
 * 写成别的 count 要么上传长度不足、要么被 {@code set(float[])} 的尺寸校验拒绝。
 */
public final class SkyRuptureShaders {

    /** 星点贴图数量，必须与着色器里的 {@code cosmiccount} 一致。 */
    public static final int SPRITE_COUNT = 12;

    public static ShaderInstance skyRuptureShader;

    public static Uniform uTime;
    public static Uniform uProgress;
    public static Uniform uBreakAmount;
    public static Uniform uFade;
    public static Uniform uSeed;
    /** 裂纹场 / 噪声场的随机偏移（碎片布局每次触发不同）。 */
    public static Uniform uPatternOffset;
    public static Uniform uRimColor;
    public static Uniform uVoidColor;
    public static Uniform uFlashColor;
    public static Uniform uCosmicUvs;
    /** 黑暗吞噬（天空部分）：前沿推进 + 不透明度。 */
    public static Uniform uSkyDarkProgress;
    public static Uniform uSkyDarkOpacity;

    /** 相机基向量：rayRight / rayUp 已乘好 tan(fov/2)，用于重建该像素的世界方向。 */
    public static Uniform uRayForward;
    public static Uniform uRayRight;
    public static Uniform uRayUp;

    /** 12 个星点 sprite 的 UV 矩形，按 [u0, v0, u1, v1] × 12 展开。 */
    public static final float[] COSMIC_UVS = new float[SPRITE_COUNT * 4];
    /** 12 个星点 sprite（图集重载后重新解析）。 */
    public static final TextureAtlasSprite[] COSMIC_SPRITES = new TextureAtlasSprite[SPRITE_COUNT];

    static {
        // 兜底：万一图集事件还没到，先用 1 个纹素的大小，避免采样到整张图集
        for (int i = 0; i < SPRITE_COUNT; i++) {
            COSMIC_UVS[i * 4 + 2] = 1.0f / 1024.0f;
            COSMIC_UVS[i * 4 + 3] = 1.0f / 1024.0f;
        }
    }

    private SkyRuptureShaders() {
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            new ResourceLocation(epca.MODID, "sky_rupture"),
                            DefaultVertexFormat.POSITION),
                    shader -> {
                        skyRuptureShader = shader;
                        uTime = shader.getUniform("time");
                        uProgress = shader.getUniform("progress");
                        uBreakAmount = shader.getUniform("breakAmount");
                        uFade = shader.getUniform("fade");
                        uSeed = shader.getUniform("seed");
                        uPatternOffset = shader.getUniform("patternOffset");
                        uRimColor = shader.getUniform("rimColor");
                        uVoidColor = shader.getUniform("voidColor");
                        uFlashColor = shader.getUniform("flashColor");
                        uCosmicUvs = shader.getUniform("cosmicuvs");
                        uSkyDarkProgress = shader.getUniform("skyDarkProgress");
                        uSkyDarkOpacity = shader.getUniform("skyDarkOpacity");
                        uRayForward = shader.getUniform("rayForward");
                        uRayRight = shader.getUniform("rayRight");
                        uRayUp = shader.getUniform("rayUp");
                        epca.LOGGER.info("[epca-render] 世界结界破损着色器已加载");
                    }
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "EPCA: 加载世界结界破损着色器失败，请检查 assets/" + epca.MODID + "/shaders/core/sky_rupture.*", e);
        }
    }

    /**
     * 方块图集缝合完成后取出 12 个星点 sprite 的 UV 矩形，
     * 与参考项目的 {@code CosmicShaders#onTextureAtlasStitched} 一致。
     */
    public static void onTextureAtlasStitched(TextureStitchEvent event) {
        if (!event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            return;
        }
        TextureAtlas atlas = event.getAtlas();
        for (int i = 0; i < SPRITE_COUNT; i++) {
            TextureAtlasSprite sprite = atlas.getSprite(
                    new ResourceLocation(epca.MODID, "shader/cosmic_" + i));
            COSMIC_SPRITES[i] = sprite;
            COSMIC_UVS[i * 4] = sprite.getU0();
            COSMIC_UVS[i * 4 + 1] = sprite.getV0();
            COSMIC_UVS[i * 4 + 2] = sprite.getU1();
            COSMIC_UVS[i * 4 + 3] = sprite.getV1();
        }
        epca.LOGGER.info("[epca-render] 已解析 {} 个结界星点 sprite", SPRITE_COUNT);
    }

    // ── Embeddium / Sodium 动画兼容 ──────────────────────────────────

    // 星点 sprite 通过 shader uniform 采样，不走标准顶点消费路径，
    // Embeddium 的 animateOnlyVisibleTextures 优化看不到它们 → 动画帧不推进。
    // 参考项目用反射每帧标记 sprite 活跃，这里照搬。
    private static volatile Method markSpriteActiveMethod;
    private static volatile boolean markSpriteActiveResolved;

    private static void resolveMarkSpriteActive() {
        if (!markSpriteActiveResolved) {
            try {
                markSpriteActiveMethod = Class.forName(
                                "me.jellysquid.mods.sodium.client.render.texture.SpriteUtil")
                        .getMethod("markSpriteActive", TextureAtlasSprite.class);
            } catch (Throwable ignored) {
                // 没装 Embeddium/Sodium：原版动画系统正常工作，不需要这个
            }
            markSpriteActiveResolved = true;
        }
    }

    /**
     * 每帧调用：把 12 个星点 sprite 标记为活跃，保证其动画帧推进。
     * Embeddium 在每个 tick 末尾会重置活跃标记，所以必须每帧调，不能只调一次。
     */
    public static void markSpritesActive() {
        resolveMarkSpriteActive();
        if (markSpriteActiveMethod == null) {
            return;
        }
        try {
            for (TextureAtlasSprite sprite : COSMIC_SPRITES) {
                if (sprite != null) {
                    markSpriteActiveMethod.invoke(null, sprite);
                }
            }
        } catch (Throwable ignored) {
            // 兼容性代码，失败不影响渲染
        }
    }
}
