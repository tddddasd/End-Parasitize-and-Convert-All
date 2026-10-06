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
 *  uniform
 *
 * <h3></h3>
 *  RottenRuinsSplendiding  12
 * <b></b>{@code hall:shader/cosmic_0..11}
 *  12  UV  {@code mat2 cosmicuvs[12]}
 * " + "
 *
 * <p> 12
 * {@code assets/epca/textures/shader/cosmic_0..11.png} .mcmeta
 *  {@code assets/minecraft/atlases/blocks.json}
 * </p>
 *
 * <h3>uniform </h3>
 * {@code cosmicuvs}  JSON
 * {@code "type":"matrix2x2","count":48}Forge  {@code Uniform}
 * {@code glUniformMatrix2fv(location, transpose, FloatBuffer)}
 * <b> buffer </b>48 / 4 = 12  mat2
 *  count  {@code set(float[])}
 */
public final class SkyRuptureShaders {

    /**  {@code cosmiccount}  */
    public static final int SPRITE_COUNT = 12;

    public static ShaderInstance skyRuptureShader;

    public static Uniform uTime;
    public static Uniform uProgress;
    public static Uniform uBreakAmount;
    public static Uniform uFade;
    public static Uniform uSeed;
    /**  /  */
    public static Uniform uPatternOffset;
    public static Uniform uRimColor;
    public static Uniform uVoidColor;
    public static Uniform uFlashColor;
    public static Uniform uCosmicUvs;
    /**  +  */
    public static Uniform uSkyDarkProgress;
    public static Uniform uSkyDarkOpacity;

    /** rayRight / rayUp  tan(fov/2) */
    public static Uniform uRayForward;
    public static Uniform uRayRight;
    public static Uniform uRayUp;

    /** 12  sprite  UV  [u0, v0, u1, v1]  12  */
    public static final float[] COSMIC_UVS = new float[SPRITE_COUNT * 4];
    /** 12  sprite */
    public static final TextureAtlasSprite[] COSMIC_SPRITES = new TextureAtlasSprite[SPRITE_COUNT];

    static {
        //  1
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
     *  12  sprite  UV
     *  {@code CosmicShaders#onTextureAtlasStitched}
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

    //  Embeddium / Sodium

    //  sprite  shader uniform
    // Embeddium  animateOnlyVisibleTextures
    //  sprite
    private static volatile Method markSpriteActiveMethod;
    private static volatile boolean markSpriteActiveResolved;

    private static void resolveMarkSpriteActive() {
        if (!markSpriteActiveResolved) {
            try {
                markSpriteActiveMethod = Class.forName(
                                "me.jellysquid.mods.sodium.client.render.texture.SpriteUtil")
                        .getMethod("markSpriteActive", TextureAtlasSprite.class);
            } catch (Throwable ignored) {
                //  Embeddium/Sodium
            }
            markSpriteActiveResolved = true;
        }
    }

    /**
     *  12  sprite
     * Embeddium  tick
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
        }
    }
}

