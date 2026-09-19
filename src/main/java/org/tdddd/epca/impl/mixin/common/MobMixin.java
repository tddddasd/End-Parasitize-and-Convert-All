package org.tdddd.epca.impl.mixin.common;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.data.SafetyDaySavedData;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.events.NaturalSpawnProtection;

/**
 * 26.1.2 迁移记录（对 {@code minecraft-patched-26.1.2.76} 的 {@code Mob} 源码核对过）：
 * <ul>
 *   <li>{@code checkDespawn()} 不变，注入保留。</li>
 *   <li>{@code finalizeSpawn} 的签名由
 *       {@code (ServerLevelAccessor, DifficultyInstance, MobSpawnType, SpawnGroupData, CompoundTag)}
 *       变为 {@code (ServerLevelAccessor, DifficultyInstance, EntitySpawnReason, SpawnGroupData)}
 *       —— 枚举 {@code MobSpawnType} 改名为 {@link EntitySpawnReason}，且不再有 {@code CompoundTag} 参数。
 *       {@code NATURAL}/{@code CHUNK_GENERATION} 两个常量名在 26.1.2 中仍存在，判定语义不变。</li>
 * </ul>
 */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void onCheckDespawn(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        
        if (!(mob.level() instanceof ServerLevel level)) return;

        
        if (mob instanceof IParasite) {
            
            SafetyDaySavedData data = SafetyDaySavedData.get(level);
            long currentTick = level.getGameTime();
            if (data.isSafetyDayActive(currentTick)) {
                
                if (NaturalSpawnProtection.isNaturallySpawned(mob)) {
                    mob.discard();
                    ci.cancel();
                    return;
                }
            }
        }

        
        boolean canDespawn = NaturalSpawnProtection.canBeNaturallyDespawned(mob);
        if (!canDespawn) {
            mob.setNoActionTime(0);
            ci.cancel();
        }
    }

    @Inject(
            method = "finalizeSpawn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onFinalizeSpawn(
            ServerLevelAccessor levelAccessor,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnType,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        
        if (spawnType != EntitySpawnReason.NATURAL && spawnType != EntitySpawnReason.CHUNK_GENERATION) {
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
