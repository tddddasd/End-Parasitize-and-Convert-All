package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.SectionPos;
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

    /** Stores one like biome for the 4x4x4 section containing {@code pos}. */
    public static void set(ServerLevel level, BlockPos pos, LikeBiome likeBiome) {
        long key = SectionPos.asLong(pos);
        LikeBiomeSavedData.get(level).set(key, likeBiome);
        org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomesPacket.sendAll(level, LikeBiomeSavedData.get(level));
    }

    public static boolean remove(ServerLevel level, BlockPos pos) {
        boolean removed = LikeBiomeSavedData.get(level).remove(SectionPos.asLong(pos));
        org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomesPacket.sendAll(level, LikeBiomeSavedData.get(level));
        return removed;
    }

    public static int removeAll(ServerLevel level) {
        int count = LikeBiomeSavedData.get(level).removeAll();
        org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomesPacket.sendAll(level, LikeBiomeSavedData.get(level));
        return count;
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
