package org.tdddd.epca.impl.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves an item layer's mask into a <b>bindable texture resource</b>, and reports how many
 * animation frames its strip contains.
 *
 * <h2>Why a mask cannot come from the block atlas</h2>
 * The first working revision of the item layer bound the block atlas as the layer texture and looked the
 * mask up as a {@code TextureAtlasSprite} inside it. That crashed the render thread with
 * {@code IllegalArgumentException: Invalid atlas id: minecraft:textures/atlas/blocks.png}, thrown from
 * {@code AtlasManager#getAtlasOrThrow(Identifier)}.
 *
 * <p>Verified with {@code javap -c} on the patched jar: {@code AtlasManager} keeps <b>two</b> maps,
 * {@code atlasById} and {@code atlasByTexture}, and {@code getAtlasOrThrow} looks up
 * <b>{@code atlasById}</b> - the atlas <em>id</em> ({@code minecraft:blocks}) - while
 * {@code TextureAtlas.LOCATION_BLOCKS} is the atlas <em>texture path</em>
 * ({@code minecraft:textures/atlas/blocks.png}, confirmed by disassembling the {@code TextureAtlas}
 * static initialiser). Passing the path asked for an atlas that does not exist under that key. Note the
 * distinction: the throw was from {@code getAtlasOrThrow}, <em>not</em> from
 * {@code RenderSetup#withTexture}.</p>
 *
 * <p>Rather than fix the lookup, this class removes the atlas from the item layer entirely: the mask is
 * bound as its own direct texture resource ({@code namespace:textures/...png}), which is the same
 * strategy {@code SkyRuptureShaders} uses for its 12 star strips and the same shape as the shipped
 * {@code GasCloudRenderType}. That also removes any dependence on the atlas layout, and it means the
 * mask is simply whatever PNG the binding names.</p>
 *
 * <h2>Frame bands</h2>
 * A directly bound texture is the raw strip, and a {@code .mcmeta} animation is <b>not</b> applied to it
 * (the same reason {@code sky_rupture.fsh} animates its strips itself). The shipped mask,
 * {@code ender_blade_scrap.png}, is 16x384, i.e. a <b>24-frame</b> strip - sampling it over the full
 * 0..1 V range would smear all 24 frames across the item. So the frame count is read from the PNG's own
 * IHDR header and the frame rate from the sibling {@code .mcmeta} if there is one, and the CPU maps the
 * quad's V into the band of the frame that is current for the game tick. Doing it on the CPU is what
 * keeps {@code corruption.fsh} completely untouched: the fragment stage still just samples
 * {@code texCoord0}, which now already points inside one frame.
 *
 * <p>The PNG frame count assumes square frames stacked vertically, which is the vanilla
 * {@code .mcmeta} convention ({@code frames = height / width}).</p>
 *
 * <p>Everything is cached per mask id, and the whole result is validated against the resource manager, so
 * a binding that names a texture which does not exist makes the layer skip instead of throwing.</p>
 */
public final class ItemMaskTexture {

    /** {@code frames = height / width} of the strip. */
    public record MaskInfo(Identifier texturePath, int frames, float framesPerTick) {

        /** True when the resource is missing or unusable, in which case the layer must not draw. */
        public boolean unusable() {
            return frames <= 0 || texturePath == null;
        }
    }

    /** Fallback frame rate when there is no readable {@code .mcmeta}: one frame per tick (vanilla default). */
    private static final float DEFAULT_FRAMETIME_TICKS = 1.0f;

    /** {@code "frametime": 1.25} out of a .mcmeta, without pulling in a JSON parser for one number. */
    private static final Pattern FRAMETIME = Pattern.compile("\"frametime\"\\s*:\\s*([0-9]*\\.?[0-9]+)");

    private static final Map<Identifier, MaskInfo> CACHE = new ConcurrentHashMap<>();

    private ItemMaskTexture() {
    }

    /**
     * Turns a binding's mask id into a bindable texture path.
     *
     * <p>Accepted forms, in order: a full texture path already
     * ({@code epca:textures/item/foo.png}), a path under {@code textures/} without the extension
     * ({@code epca:textures/item/foo}), or the item-style short form
     * ({@code epca:item/foo}, which is what
     * {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry#defaultMaskFor} produces).</p>
     */
    public static Identifier toTexturePath(Identifier maskId) {
        if (maskId == null) {
            return null;
        }
        String path = maskId.getPath();
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return Identifier.fromNamespaceAndPath(maskId.getNamespace(), path);
    }

    /**
     * Resolves (and caches) the bindable texture and its frame count for one mask id.
     *
     * <p>Returns a {@link MaskInfo#unusable()} result rather than throwing when the resource is absent,
     * so a misconfigured {@code mask(...)} degrades to "no effect" instead of a render-thread crash.</p>
     */
    public static MaskInfo resolve(Identifier maskId) {
        if (maskId == null) {
            return new MaskInfo(null, 0, 1.0f);
        }
        MaskInfo cached = CACHE.get(maskId);
        if (cached != null) {
            return cached;
        }
        MaskInfo info = load(maskId);
        CACHE.put(maskId, info);
        return info;
    }

    /** Drops the cache (resource reload / debugging). */
    public static void invalidate() {
        CACHE.clear();
    }

    private static MaskInfo load(Identifier maskId) {
        Identifier texturePath = toTexturePath(maskId);
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || texturePath == null) {
            return new MaskInfo(texturePath, 0, 1.0f);
        }
        try {
            var resource = mc.getResourceManager().getResource(texturePath);
            if (resource.isEmpty()) {
                epca.LOGGER.warn("[epca-render] item layer mask texture not found, layer disabled: {}",
                        texturePath);
                return new MaskInfo(texturePath, 0, 1.0f);
            }
            int frames = readFrameCount(resource.get());
            float framesPerTick = 1.0f / Math.max(readFrametimeTicks(texturePath), 0.01f);
            return new MaskInfo(texturePath, frames, framesPerTick);
        } catch (Throwable t) {
            epca.LOGGER.warn("[epca-render] could not inspect item layer mask texture {}", texturePath, t);
            return new MaskInfo(texturePath, 0, 1.0f);
        }
    }

    /** Reads the PNG IHDR so the strip's frame count is {@code height / width}. */
    private static int readFrameCount(net.minecraft.server.packs.resources.Resource resource)
            throws IOException {
        try (InputStream in = resource.open()) {
            byte[] header = new byte[24];
            int read = 0;
            while (read < header.length) {
                int n = in.read(header, read, header.length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
            if (read < 24 || (header[0] & 0xFF) != 0x89 || header[1] != 'P') {
                return 0; // not a PNG
            }
            int width = readInt(header, 16);
            int height = readInt(header, 20);
            if (width <= 0 || height <= 0 || height % width != 0) {
                return 1; // not a frame strip: treat the whole image as one frame
            }
            return height / width;
        }
    }

    /** Big-endian PNG integer at the given offset. */
    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    /**
     * {@code frametime} (in ticks) out of the sibling {@code .mcmeta}, or
     * {@link #DEFAULT_FRAMETIME_TICKS}. Absence is normal: a non-animated mask has no {@code .mcmeta} and
     * simply has one frame.
     */
    private static float readFrametimeTicks(Identifier texturePath) {
        Identifier metaPath = Identifier.fromNamespaceAndPath(texturePath.getNamespace(),
                texturePath.getPath() + ".mcmeta");
        Minecraft mc = Minecraft.getInstance();
        try {
            var resource = mc.getResourceManager().getResource(metaPath);
            if (resource.isEmpty()) {
                return DEFAULT_FRAMETIME_TICKS;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null && sb.length() < 4096) {
                    sb.append(line);
                }
                Matcher m = FRAMETIME.matcher(sb);
                if (m.find()) {
                    float value = Float.parseFloat(m.group(1));
                    return value > 0.0f ? value : DEFAULT_FRAMETIME_TICKS;
                }
            }
        } catch (Throwable ignored) {
            // A malformed or unreadable mcmeta must not break rendering; fall back to the vanilla rate.
        }
        return DEFAULT_FRAMETIME_TICKS;
    }

    /** The animation frame to show for one mask at the given game time, or 0 when unusable. */
    public static int currentFrame(MaskInfo info, long gameTime) {
        if (info == null || info.unusable() || info.frames() <= 1) {
            return 0;
        }
        long frame = (long) (gameTime * info.framesPerTick());
        int wrapped = (int) Math.floorMod(frame, (long) info.frames());
        return wrapped;
    }

    /** Diagnostics: how many mask ids are currently cached (and whether any failed). */
    public static Map<Identifier, MaskInfo> snapshot() {
        return Collections.unmodifiableMap(CACHE);
    }
}
