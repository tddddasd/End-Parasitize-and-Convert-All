package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;

/**
 * Applies the like biome spawn mode.
 *
 * <p>Verified against the 26.1.2 server jar: {@code NaturalSpawner.mobsAt(ServerLevel, StructureManager,
 * ChunkGenerator, MobCategory, BlockPos, Holder&lt;Biome&gt;)} returns a {@code WeightedList} of spawner
 * entries, and the real biome is the last argument, so swapping its result is the narrowest hook.</p>
 */
@Mixin(net.minecraft.world.level.NaturalSpawner.class)
public abstract class NaturalSpawnerLikeBiomeMixin {

    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true, require = 0)
    private static void epca$likeBiomeMobs(ServerLevel level, StructureManager structureManager,
                                           ChunkGenerator generator, MobCategory category, BlockPos pos,
                                           Holder<Biome> realBiome,
                                           CallbackInfoReturnable<WeightedList<MobSpawnSettings.SpawnerData>> cir) {
        LikeBiome like = LikeBiomeManager.activeAt(level, pos);
        if (like == null || like.spawnMode() == LikeBiome.SPAWN_NONE) {
            return;
        }
        if (like.spawnMode() == LikeBiome.SPAWN_COEXIST && level.getRandom().nextBoolean()) {
            return;
        }
        var key = LikeBiomeManager.resolve(level, like.biomeId());
        if (key == null) {
            return;
        }
        var biome = level.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).get(key);
        if (biome.isEmpty()) {
            return;
        }
        cir.setReturnValue(biome.get().value().getMobSettings().getMobs(category));
    }
}
