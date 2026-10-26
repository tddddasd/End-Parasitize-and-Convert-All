package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.util.random.WeightedRandomList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;

/**
 * Applies the like biome spawn mode.
 *
 * <p>Verified signature (javap on the 1.20.1 mapped jar):
 * {@code private static WeightedRandomList<MobSpawnSettings.SpawnerData> mobsAt(ServerLevel,
 * StructureManager, ChunkGenerator, MobCategory, BlockPos, Holder<Biome>)}. Overriding its result is the
 * narrowest way to swap the spawn table, because the real biome is handed in as the last argument.</p>
 */
@Mixin(net.minecraft.world.level.NaturalSpawner.class)
public abstract class NaturalSpawnerLikeBiomeMixin {

    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true, require = 0)
    private static void epca$likeBiomeMobs(ServerLevel level, StructureManager structureManager,
                                           ChunkGenerator generator, MobCategory category, BlockPos pos,
                                           Holder<Biome> realBiome,
                                           CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
        LikeBiome like = LikeBiomeManager.activeAt(level, pos);
        if (like == null || like.spawnMode() == LikeBiome.SPAWN_NONE) {
            return;
        }
        // coexist keeps the real table half of the time
        if (like.spawnMode() == LikeBiome.SPAWN_COEXIST && level.getRandom().nextBoolean()) {
            return;
        }
        var key = LikeBiomeManager.resolve(level, like.biomeId());
        if (key == null) {
            return;
        }
        var biome = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME).get(key);
        if (biome == null) {
            return;
        }
        cir.setReturnValue(biome.getMobSettings().getMobs(category));
    }
}
