package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ServerLevelAccessor;
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
 * <ul>
 *   <li>{@code override} - the like biome's spawn table replaces the real biome's.</li>
 *   <li>{@code coexist}  - half the attempts use the like biome's table, the rest keep the real one, so
 *       both biome's mobs appear.</li>
 *   <li>{@code none}     - the like biome contributes nothing, i.e. the real biome spawns untouched.</li>
 * </ul>
 */
@Mixin(net.minecraft.world.level.NaturalSpawner.class)
public abstract class NaturalSpawnerLikeBiomeMixin {

    @Inject(method = "getRandomSpawnMobAt", at = @At("HEAD"), cancellable = true, require = 0)
    private static void epca$likeBiomeSpawn(ServerLevel level, ChunkGenerator generator, MobCategory category,
                                            RandomSource random, BlockPos pos,
                                            CallbackInfoReturnable<MobSpawnSettings.SpawnerData> cir) {
        LikeBiome like = LikeBiomeManager.activeAt(level, pos);
        if (like == null || like.spawnMode() == LikeBiome.SPAWN_NONE) {
            return;
        }
        // coexist keeps the real table half of the time
        if (like.spawnMode() == LikeBiome.SPAWN_COEXIST && random.nextBoolean()) {
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
        // 1.20.1 returns an Optional here; an empty one means "nothing of this category spawns"
        java.util.Optional<MobSpawnSettings.SpawnerData> picked =
                biome.getMobSettings().getMobs(category).getRandom(random);
        cir.setReturnValue(picked.orElse(null));
    }
}
