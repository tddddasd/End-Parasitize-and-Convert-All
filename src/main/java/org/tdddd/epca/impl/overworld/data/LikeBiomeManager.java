package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * Applies like biomes on top of real ones. The real biome is never replaced: everything that wants an
 * overlaid property asks this class first.
 */
public final class LikeBiomeManager {

    private LikeBiomeManager() {
    }

    @Nullable
    public static ResourceKey<Biome> resolve(ServerLevel level, Identifier id) {
        if (id == null) {
            return null;
        }
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        return level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(key) ? key : null;
    }

    @Nullable
    public static LikeBiome activeAt(ServerLevel level, BlockPos pos) {
        LikeBiome stored = LikeBiomeSavedData.get(level).at(pos);
        if (stored == null || resolve(level, stored.biomeId()) == null) {
            return null;
        }
        return stored;
    }

    public static void set(ServerLevel level, BlockPos pos, LikeBiome likeBiome) {
        LikeBiomeSavedData.get(level).set(SectionPos.asLong(pos), likeBiome);
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
}
