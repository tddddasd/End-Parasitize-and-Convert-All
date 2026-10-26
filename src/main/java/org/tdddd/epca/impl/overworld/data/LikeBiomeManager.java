package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;

/**
 * Applies like biomes on top of real ones.
 *
 * <p>A like biome never replaces the real biome: the real one stays in the chunk, and everything that
 * wants an overlaid property asks this class first. When the like biome id no longer resolves (a data pack
 * removed it), the overlay is ignored and the real biome is used, which is the "cancel the overlay"
 * behaviour.</p>
 */
public final class LikeBiomeManager {

    private LikeBiomeManager() {
    }

    /** Resolves a biome id, or {@code null} when that biome does not exist any more. */
    @Nullable
    public static ResourceKey<Biome> resolve(ServerLevel level, ResourceLocation id) {
        if (id == null) {
            return null;
        }
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        return level.registryAccess().registryOrThrow(Registries.BIOME).containsKey(key) ? key : null;
    }

    /**
     * The like biome actually in force at {@code pos}: the stored entry, but only when its biome still
     * exists.
     */
    @Nullable
    public static LikeBiome activeAt(ServerLevel level, BlockPos pos) {
        LikeBiome stored = LikeBiomeSavedData.get(level).at(pos);
        if (stored == null || resolve(level, stored.biomeId()) == null) {
            return null;
        }
        return stored;
    }

    /** Stores one like biome for the chunk containing {@code pos}. */
    public static void set(ServerLevel level, BlockPos pos, LikeBiome likeBiome) {
        LikeBiomeSavedData.get(level).set(new ChunkPos(pos), likeBiome);
    }

    public static boolean remove(ServerLevel level, BlockPos pos) {
        return LikeBiomeSavedData.get(level).remove(new ChunkPos(pos));
    }

    public static int removeAll(ServerLevel level) {
        return LikeBiomeSavedData.get(level).removeAll();
    }

    /** True when {@code pos} sits in a chunk whose like biome overrides colours. */
    public static boolean overridesColors(ServerLevel level, BlockPos pos) {
        LikeBiome like = activeAt(level, pos);
        return like != null && like.overrideColors();
    }

    /** True when {@code pos} sits in a chunk whose like biome overrides rain. */
    public static boolean overridesRain(ServerLevel level, BlockPos pos) {
        LikeBiome like = activeAt(level, pos);
        return like != null && like.overrideRain();
    }
}
