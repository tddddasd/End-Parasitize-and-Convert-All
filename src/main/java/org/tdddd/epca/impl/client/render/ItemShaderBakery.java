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
 *  {@link TextureAtlasSprite}  {@link BakedQuad}
 *  {@code ItemRenderer.renderQuadList()}
 *
 * <p> RottenRuinsSplendiding  {@code CosmicRenderUtils}
 *  {@link WeakHashMap}
 *  sprite </p>
 */
public final class ItemShaderBakery {

    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();

    /**
     *  ModelState UV
     *
     * <p> Forge  {@code SimpleModelState} Forge API
     * {@link ModelState} </p>
     */
    private static final ModelState IDENTITY_STATE = new ModelState() {
    };

    /**
     *  sprite  sprite
     *
     */
    private static final Map<TextureAtlasSprite, List<BakedQuad>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ItemShaderBakery() {
    }

    /**
     *  sprite  quad
     *
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

    /**  */
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
            //  /
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
                }
            }
        }
        return quads;
    }

    /**  */
    public static Transformation identity() {
        return Transformation.identity();
    }
}

