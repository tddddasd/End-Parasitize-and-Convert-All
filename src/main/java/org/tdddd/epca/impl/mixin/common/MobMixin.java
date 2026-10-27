package org.tdddd.epca.impl.mixin.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(
            method = "finalizeSpawn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onFinalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            SpawnGroupData spawnGroupData,
            CompoundTag tag,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
            return;
        }

        int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
        if (stage != 13) {
            return;
        }

        Mob mob = (Mob) (Object) this;
        if (!(mob instanceof IParasite)) {
            mob.discard();
            cir.setReturnValue(null);
        }
    }
}