package org.tdddd.epca.impl.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Builds the flat item plane an item shader layer is drawn as, and caches it per sprite.
 *
 * <h2>1.20.1 -&gt; 26.1.2: this is a rewrite, not a port</h2>
 * The 1.20.1 twin baked the plane into {@code BakedQuad}s with
 * {@code ItemModelGenerator#processFrames(0, "layer0", sprite.contents())} plus
 * {@code FaceBakery#bakeQuad(...)}, both in {@code net.minecraft.client.renderer.block.model}.
 * <b>Neither class exists in 26.1.2</b> (verified against the class list of the patched jar), so that code
 * cannot be ported.
 *
 * <p>An intermediate revision built {@code BakedQuad} records by hand and emitted them with
 * {@code VertexConsumer#putBakedQuad}. That was abandoned for a stronger reason than style:
 * {@code putBakedQuad} writes exactly six elements (POSITION, COLOR, UV0, UV1, UV2, NORMAL, per
 * {@code javap -c}), and a vertex format containing those sums to 35 bytes, which
 * {@code VertexFormat.Builder#build()} rejects because it requires a multiple of 4. So the geometry is
 * stored as plain floats here and written by {@link ItemCorruptionRenderer} with the writers the layout
 * actually declares.</p>
 *
 * <p>The plane is the item's unit quad in item-model space (1/16 block units): x 0..16 and y 0..16, a thin
 * slab around z = 8, plus a mirrored back face so the layer is visible from behind. That is the same box
 * {@code ItemModelGenerator} produced for a flat 2D item texture, and the UVs are the mask sprite's own
 * rectangle.</p>
 *
 * <p><b>Documented approximation:</b> because the generator is gone, a 3D/block item gets this flat
 * billboard plane rather than a shell around its real geometry. For the flat items this layer is used on
 * (the shipped binding is {@code epca:ender_blade_scrap}, a flat sprite) the result is identical; for a
 * block item the decay would cover its silhouette rather than wrap it.</p>
 *
 * <p>The cache is keyed weakly by sprite, so an atlas reload naturally drops the stale entries. The
 * returned array is shared and must not be mutated by callers.</p>
 */
public final class ItemShaderBakery {

    /** Floats per vertex: x, y, z, u, v. */
    public static final int VERTEX_STRIDE = 5;
    /** Vertices per quad. */
    public static final int VERTICES_PER_QUAD = 4;
    /** Quads per sprite: the front face and its mirror. */
    public static final int QUADS_PER_SPRITE = 2;
    /** Total floats in the array returned by {@link #geometry}. */
    public static final int GEOMETRY_LENGTH = QUADS_PER_SPRITE * VERTICES_PER_QUAD * VERTEX_STRIDE;

    /** Half-thickness of the plane in item-model units, so it does not z-fight with the item. */
    private static final float PLANE_HALF_DEPTH = 0.5f;
    /** Centre of the plane along z in item-model units (8 = the middle of the 16-unit item box). */
    private static final float PLANE_Z = 8.0f;

    private static final Map<TextureAtlasSprite, float[]> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ItemShaderBakery() {
    }

    /**
     * The (cached) geometry for one mask sprite: {@link #GEOMETRY_LENGTH} floats, laid out as
     * {@link #QUADS_PER_SPRITE} quads of {@link #VERTICES_PER_QUAD} vertices of {@link #VERTEX_STRIDE}
     * floats each.
     */
    public static float[] geometry(TextureAtlasSprite sprite) {
        if (sprite == null) {
            return new float[0];
        }
        float[] cached = CACHE.get(sprite);
        if (cached != null) {
            return cached;
        }
        float[] geometry = bake(sprite);
        CACHE.put(sprite, geometry);
        return geometry;
    }

    /** Clears the cache (atlas reload or debugging). */
    public static void invalidate() {
        CACHE.clear();
    }

    private static float[] bake(TextureAtlasSprite sprite) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();

        float z0 = PLANE_Z - PLANE_HALF_DEPTH;
        float z1 = PLANE_Z + PLANE_HALF_DEPTH;

        float[] out = new float[GEOMETRY_LENGTH];
        int i = 0;

        // Front face at z1: bottom-left, bottom-right, top-right, top-left.
        i = put(out, i, 0.0f, 0.0f, z1, u0, v1);
        i = put(out, i, 16.0f, 0.0f, z1, u1, v1);
        i = put(out, i, 16.0f, 16.0f, z1, u1, v0);
        i = put(out, i, 0.0f, 16.0f, z1, u0, v0);

        // Mirrored back face at z0. Culling is off for this pipeline, but emitting both faces removes a
        // whole class of "the effect vanished" reports if a driver keeps back-face culling anyway.
        i = put(out, i, 0.0f, 0.0f, z0, u0, v1);
        i = put(out, i, 0.0f, 16.0f, z0, u0, v0);
        i = put(out, i, 16.0f, 16.0f, z0, u1, v0);
        i = put(out, i, 16.0f, 0.0f, z0, u1, v1);

        return out;
    }

    private static int put(float[] out, int index, float x, float y, float z, float u, float v) {
        out[index] = x;
        out[index + 1] = y;
        out[index + 2] = z;
        out[index + 3] = u;
        out[index + 4] = v;
        return index + VERTEX_STRIDE;
    }
}
