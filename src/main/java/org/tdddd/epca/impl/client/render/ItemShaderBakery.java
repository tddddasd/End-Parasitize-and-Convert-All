package org.tdddd.epca.impl.client.render;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 把一张 {@link TextureAtlasSprite} 烘焙成物品平面用的 {@link BakedQuad} 列表，
 * 以便用 {@code ItemRenderer.renderQuadList()} 画出与物品完全同形的附加层。
 *
 * <p>移植自 RottenRuinsSplendiding 的 {@code CosmicRenderUtils}，
 * 并加了一层 {@link WeakHashMap} 缓存 —— 参考项目每帧都重新烘焙，
 * 这里按 sprite 缓存，物品在屏幕上时基本零开销。</p>
 */
public final class ItemShaderBakery {

    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();

    /**
     * 恒等 ModelState（不旋转、不锁 UV）。
     *
     * <p>不用 Forge 的 {@code SimpleModelState}，避免依赖可变的 Forge API：
     * {@link ModelState} 的默认实现本身就是恒等变换。</p>
     */
    private static final ModelState IDENTITY_STATE = new ModelState() {
    };

    /**
     * 缓存键是 sprite 实例。图集重载会换出新 sprite，旧条目因为弱引用自然回收，
     * 因此不需要在重载时手动清空。
     */
    private static final Map<TextureAtlasSprite, List<BakedQuad>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ItemShaderBakery() {
    }

    /**
     * 取（或首次烘焙）某张 sprite 的物品 quad 列表。
     * 返回的是只读列表，可以安全地在渲染线程复用。
     */
    public static List<BakedQuad> quads(TextureAtlasSprite sprite) {
        if (sprite == null) {
            return List.of();
        }
        List<BakedQuad> cached = CACHE.get(sprite);
        if (cached != null) {
            return cached;
        }
        List<BakedQuad> baked = Collections.unmodifiableList(bake(sprite));
        CACHE.put(sprite, baked);
        return baked;
    }

    /** 清空缓存（图集重载或调试时可调用）。 */
    public static void invalidate() {
        CACHE.clear();
    }

    private static List<BakedQuad> bake(TextureAtlasSprite sprite) {
        List<BakedQuad> quads = new LinkedList<>();
        ResourceLocation dummyLoc = new ResourceLocation(epca.MODID, "item_layer_bakery");

        List<BlockElement> unbaked;
        try {
            unbaked = ITEM_MODEL_GENERATOR.processFrames(0, "layer0", sprite.contents());
        } catch (Exception e) {
            // 贴图损坏 / 内容异常时不要让渲染线程崩掉，退化为不画。
            return quads;
        }

        for (BlockElement element : unbaked) {
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                try {
                    quads.add(FACE_BAKERY.bakeQuad(
                            element.from, element.to, entry.getValue(), sprite,
                            entry.getKey(), IDENTITY_STATE,
                            element.rotation, element.shade, dummyLoc));
                } catch (Exception ignored) {
                    // 单个面烘焙失败不影响其余面
                }
            }
        }
        return quads;
    }

    /** 恒等变换，供需要自己烘焙的层使用。 */
    public static Transformation identity() {
        return Transformation.identity();
    }
}
