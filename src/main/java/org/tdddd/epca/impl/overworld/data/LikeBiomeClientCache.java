package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Client copy of the like biome table, filled by {@code SyncLikeBiomesPacket}.
 *
 * <p>Only the overlay properties are cached; resolving them into actual colours happens on the client
 * against its own biome registry, so a biome that disappears client side stops the overlay as well.</p>
 */
public final class LikeBiomeClientCache {

    private static final Map<Long, LikeBiome> BY_SECTION = new HashMap<>();

    private LikeBiomeClientCache() {
    }

    public static void replaceAll(Map<Long, LikeBiome> entries) {
        BY_SECTION.clear();
        BY_SECTION.putAll(entries);
    }

    public static void clear() {
        BY_SECTION.clear();
    }

    /** The like biome covering {@code pos}, or {@code null}. */
    @Nullable
    public static LikeBiome at(BlockPos pos) {
        return BY_SECTION.get(SectionPos.asLong(pos));
    }

    /** True when the like biome at {@code pos} should drive the water, fog, grass and sky colours. */
    public static boolean overridesColors(BlockPos pos) {
        LikeBiome like = at(pos);
        return like != null && like.overrideColors();
    }

    @Nullable
    public static ResourceLocation biomeId(BlockPos pos) {
        LikeBiome like = at(pos);
        return like == null ? null : like.biomeId();
    }
}
