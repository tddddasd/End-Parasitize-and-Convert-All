package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Client copy of the like biome table, filled by {@code SyncLikeBiomesPacket}. */
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

    @Nullable
    public static LikeBiome at(BlockPos pos) {
        return BY_SECTION.get(SectionPos.asLong(pos));
    }

    public static boolean overridesColors(BlockPos pos) {
        LikeBiome like = at(pos);
        return like != null && like.overrideColors();
    }

    @Nullable
    public static Identifier biomeId(BlockPos pos) {
        LikeBiome like = at(pos);
        return like == null ? null : like.biomeId();
    }
}
