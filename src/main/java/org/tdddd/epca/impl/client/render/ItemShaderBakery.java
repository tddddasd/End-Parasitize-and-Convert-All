package org.tdddd.epca.impl.client.render;

/**
 * The flat item plane an item shader layer is drawn as.
 *
 * <h2>1.20.1 -&gt; 26.1.2: this is a rewrite, not a port</h2>
 * The 1.20.1 twin baked the plane into {@code BakedQuad}s with
 * {@code ItemModelGenerator#processFrames(0, "layer0", sprite.contents())} plus
 * {@code FaceBakery#bakeQuad(...)}, both in {@code net.minecraft.client.renderer.block.model}.
 * <b>Neither class exists in 26.1.2</b> (verified against the class list of the patched jar), so that code
 * cannot be ported.
 *
 * <p>An intermediate revision built {@code BakedQuad} records by hand and emitted them with
 * {@code VertexConsumer#putBakedQuad}. That was abandoned for a hard reason: {@code putBakedQuad} writes
 * exactly six elements (POSITION, COLOR, UV0, UV1, UV2, NORMAL, per {@code javap -c}), and a format
 * containing those sums to 35 bytes, which {@code VertexFormat.Builder#build()} rejects because it requires
 * a multiple of 4. So the geometry is plain floats and is written by {@link ItemCorruptionRenderer} with
 * the writers the 32-byte layout declares.</p>
 *
 * <h2>UV space</h2>
 * The UVs here are the <b>normalised 0..1 square of whatever texture the render type binds</b>, not atlas
 * coordinates. The renderer scales V into the current animation frame band before emitting, so a
 * multi-frame strip shows exactly one frame; see {@link ItemMaskTexture} for why that matters (the shipped
 * mask is a 24-frame strip).
 *
 * <h2>Shape</h2>
 * The item's unit quad in item-model space (1/16 block units): x 0..16 and y 0..16, a thin slab around
 * z = 8, plus a mirrored back face so the layer is visible from behind.
 *
 * <p><b>Documented approximation:</b> because the generator is gone, a 3D/block item gets this flat
 * billboard plane rather than a shell around its real geometry. For the flat items this layer is used on
 * (the shipped binding is {@code epca:ender_blade_scrap}, a flat sprite) the result is identical; for a
 * block item the decay would cover its silhouette rather than wrap it.</p>
 *
 * <p>The array is a shared constant and must not be mutated by callers.</p>
 */
public final class ItemShaderBakery {

    /** Floats per vertex: x, y, z, u, v. */
    public static final int VERTEX_STRIDE = 5;
    /** Vertices per quad. */
    public static final int VERTICES_PER_QUAD = 4;
    /** Quads: the front face and its mirror. */
    public static final int QUADS = 2;
    /** Total floats in {@link #GEOMETRY}. */
    public static final int GEOMETRY_LENGTH = QUADS * VERTICES_PER_QUAD * VERTEX_STRIDE;

    /** Half-thickness of the plane in item-model units, so it does not z-fight with the item. */
    private static final float PLANE_HALF_DEPTH = 0.5f;
    /** Centre of the plane along z in item-model units (8 = the middle of the 16-unit item box). */
    private static final float PLANE_Z = 8.0f;

    /**
     * The geometry, in the bound texture's own 0..1 UV space.
     *
     * <p>Layout: {@link #QUADS} quads of {@link #VERTICES_PER_QUAD} vertices of {@link #VERTEX_STRIDE}
     * floats. There is exactly one of these for every item: the mask texture is chosen by the render type
     * and the frame band is applied by the renderer, so nothing here depends on the sprite.</p>
     */
    public static final float[] GEOMETRY = bake();

    private ItemShaderBakery() {
    }

    /** The geometry; a constant, so callers can cache the reference freely. */
    public static float[] geometry() {
        return GEOMETRY;
    }

    /** Kept for source compatibility with the earlier sprite-cached revision; now a no-op. */
    public static void invalidate() {
        // No per-sprite cache any more: the geometry is a constant and ItemMaskTexture owns the
        // per-mask cache.
    }

    private static float[] bake() {
        float z0 = PLANE_Z - PLANE_HALF_DEPTH;
        float z1 = PLANE_Z + PLANE_HALF_DEPTH;

        float[] out = new float[GEOMETRY_LENGTH];
        int i = 0;

        // Front face at z1: bottom-left, bottom-right, top-right, top-left.
        i = put(out, i, 0.0f, 0.0f, z1, 0.0f, 1.0f);
        i = put(out, i, 16.0f, 0.0f, z1, 1.0f, 1.0f);
        i = put(out, i, 16.0f, 16.0f, z1, 1.0f, 0.0f);
        i = put(out, i, 0.0f, 16.0f, z1, 0.0f, 0.0f);

        // Mirrored back face at z0. Culling is off for this pipeline, but emitting both faces removes a
        // whole class of "the effect vanished" reports if a driver keeps back-face culling anyway.
        i = put(out, i, 0.0f, 0.0f, z0, 0.0f, 1.0f);
        i = put(out, i, 0.0f, 16.0f, z0, 0.0f, 0.0f);
        i = put(out, i, 16.0f, 16.0f, z0, 1.0f, 0.0f);
        i = put(out, i, 16.0f, 0.0f, z0, 1.0f, 1.0f);

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
