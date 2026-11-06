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

public final class ItemMaskTexture {

    public record MaskInfo(Identifier texturePath, int frames, float framesPerTick) {

        public boolean unusable() {
            return frames <= 0 || texturePath == null;
        }
    }

    private static final float DEFAULT_FRAMETIME_TICKS = 1.0f;

    private static final Pattern FRAMETIME = Pattern.compile("\"frametime\"\\s*:\\s*([0-9]*\\.?[0-9]+)");

    private static final Map<Identifier, MaskInfo> CACHE = new ConcurrentHashMap<>();

    private ItemMaskTexture() {
    }

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
                return 0;
            }
            int width = readInt(header, 16);
            int height = readInt(header, 20);
            if (width <= 0 || height <= 0 || height % width != 0) {
                return 1;
            }
            return height / width;
        }
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

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

        }
        return DEFAULT_FRAMETIME_TICKS;
    }

    public static int currentFrame(MaskInfo info, long gameTime) {
        if (info == null || info.unusable() || info.frames() <= 1) {
            return 0;
        }
        long frame = (long) (gameTime * info.framesPerTick());
        int wrapped = (int) Math.floorMod(frame, (long) info.frames());
        return wrapped;
    }

    public static Map<Identifier, MaskInfo> snapshot() {
        return Collections.unmodifiableMap(CACHE);
    }
}
