package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.joml.Vector4f;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;

import javax.annotation.Nullable;

/**
 * Colour side of a like biome on the client.
 *
 * <p>Every getter returns {@code null} when the section has no like biome, when its colour toggle is off
 * or when that biome no longer exists in the client registry, so callers simply fall through to vanilla
 * behaviour.</p>
 */
public final class LikeBiomeClientEffects {

    private LikeBiomeClientEffects() {
    }

    /** The like biome in force at {@code pos} when it should drive colours, else {@code null}. */
    @Nullable
    public static Biome activeBiome(BlockPos pos) {
        if (pos == null || !LikeBiomeClientCache.overridesColors(pos)) {
            return null;
        }
        Identifier id = LikeBiomeClientCache.biomeId(pos);
        Minecraft minecraft = Minecraft.getInstance();
        if (id == null || minecraft.level == null) {
            return null;
        }
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        return minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME)
                .get(key).map(holder -> holder.value()).orElse(null);
    }

    @Nullable
    public static Integer waterColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getWaterColor();
    }

    @Nullable
    public static Integer grassColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getGrassColor(0.5D, 64.0D);
    }

    @Nullable
    public static Integer foliageColor(BlockPos pos) {
        Biome biome = activeBiome(pos);
        return biome == null ? null : biome.getFoliageColor();
    }

}
