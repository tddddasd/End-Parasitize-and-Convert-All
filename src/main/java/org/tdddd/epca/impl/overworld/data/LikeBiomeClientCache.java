package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

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
    public static ResourceLocation biomeId(BlockPos pos) {
        LikeBiome like = at(pos);
        return like == null ? null : like.biomeId();
    }
}
