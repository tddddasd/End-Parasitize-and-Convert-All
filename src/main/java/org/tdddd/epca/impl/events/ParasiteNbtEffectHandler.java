package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.epca;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationManager;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.EventPriority;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GenericPriorityTargetGoal;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ParasiteNbtEffectHandler {

    private static final String LAST_RAGE_TRIGGER_KEY = "lastRageTrigger";
    private static final String FORCED_TARGET_UUID_KEY = "forcedTargetUuid";
    private static final String LAST_FORCED_SWITCH_TICK_KEY = "lastForcedSwitchTick";
    private static final int FORCED_TARGET_COOLDOWN_SECONDS = 12;

    

    @SubscribeEvent
    public static void onParasiteAttack(LivingAttackEvent event) {
        
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        
        LivingEntity victim = event.getEntity();

        
        if (IParasite.isParasiteByTagOrInterface(attacker) && IParasite.isParasiteByTagOrInterface(victim)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onParasiteTargetChange(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (!IParasite.isParasiteByTagOrInterface(entity)) return;

        LivingEntity newTarget = event.getNewTarget();
        if (newTarget != null && IParasite.isParasiteByTagOrInterface(newTarget)) {
            event.setCanceled(true);  
        }
    }



    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!IParasite.isParasiteByTagOrInterface(entity)) return;

        
        if (entity instanceof IParasite) {
            entity.getPersistentData().putBoolean("Parasite", true);
            
            
            if (entity instanceof Mob mob) {
                
                
                boolean hasPriorityGoal = mob.targetSelector.getAvailableGoals().stream()
                        .anyMatch(g -> g.getGoal() instanceof PriorityTargetGoal);
                if (!hasPriorityGoal) {
                    mob.targetSelector.addGoal(0, new PriorityTargetGoal(mob, 16.0));
                }
            }
        }
        
        else {
            
            float multiplier = DifficultyEffects.getParasiteStatMultiplier(entity.level());
            double newMaxHealth = entity.getMaxHealth() * multiplier;
            entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
            entity.setHealth((float) newMaxHealth);
            double newArmor = entity.getArmorValue() * multiplier;
            entity.getAttribute(Attributes.ARMOR).setBaseValue(newArmor);

            
            if (entity instanceof Mob mob) {
                
                
                mob.targetSelector.getAvailableGoals().removeIf(
                        goal -> goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
                );
                
                mob.targetSelector.addGoal(0, new GenericPriorityTargetGoal(mob, 16.0));
            }
        }
    }

    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!IParasite.isParasiteByTagOrInterface(entity)) return;
        if (entity instanceof IParasite) return; 

        DamageSource source = event.getSource();
        float amount = event.getAmount();

        
        if (isDamageAdaptationInvulnerable(entity)) {
            event.setCanceled(true);
            return;
        }

        
        if (source.getEntity() instanceof LivingEntity attacker && IParasite.isParasiteByTagOrInterface(attacker)) {
            event.setCanceled(true);
            return;
        }

        
        if (source.is(DamageTypeTags.IS_FIRE)) {
            Level level = entity.level();
            int currentTick = (int) level.getGameTime();
            int lastTrigger = entity.getPersistentData().getInt(LAST_RAGE_TRIGGER_KEY);
            if (currentTick - lastTrigger >= 10 && level.random.nextFloat() < 0.1f) {
                MobEffectInstance currentRage = entity.getEffect(ModEffects.RAGE.get());
                int amplifier = (currentRage != null) ? Math.min(currentRage.getAmplifier() + 1, 49) : 0;
                entity.addEffect(new MobEffectInstance(ModEffects.RAGE.get(), 300, amplifier));
                entity.getPersistentData().putInt(LAST_RAGE_TRIGGER_KEY, currentTick);
            }
        }
    }

    
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity killer)) return;
        if (!IParasite.isParasiteByTagOrInterface(killer)) return;
        if (killer instanceof IParasite) return;

        Level level = killer.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        EvolutionManager evolutionManager = EvolutionManager.forDimension(serverLevel);
        evolutionManager.addPoints(1);
    }

    
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (!IParasite.isParasiteByTagOrInterface(target)) return;
        if (target instanceof IParasite) return;

        if (event.getSource().getEntity() instanceof LivingEntity attacker && !IParasite.isParasiteByTagOrInterface(attacker)) {
            trySwitchForcedTargetOnAttacked(target, attacker);
        }
    }

    private static void trySwitchForcedTargetOnAttacked(LivingEntity self, LivingEntity attacker) {
        Level level = self.level();
        if (level.isClientSide) return;
        if (attacker != null && !IParasite.isParasiteByTagOrInterface(attacker)) {
            
            long lastSwitch = self.getPersistentData().getLong(LAST_FORCED_SWITCH_TICK_KEY);
            long now = level.getGameTime();
            if (now - lastSwitch < FORCED_TARGET_COOLDOWN_SECONDS * 20L) return;
            double distSq = self.distanceToSqr(attacker);
            if (distSq > 16 * 16) return;
            
            self.getPersistentData().putString(FORCED_TARGET_UUID_KEY, attacker.getUUID().toString());
            self.getPersistentData().putLong(LAST_FORCED_SWITCH_TICK_KEY, now);
        }
    }

    
    private static boolean isDamageAdaptationInvulnerable(LivingEntity entity) {
        var entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        var config = DamageAdaptationManager.getConfig(entityId);
        if (config == null) return false;
        return DamageAdaptation.isInvulnerable(entity);
    }

    private static BlockPos findValidSpawnPosition(ServerLevel level, BlockPos startPos) {
        int radius = 6;
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy >= -1; dy--) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    testPos.set(startPos.getX() + dx, startPos.getY() + dy, startPos.getZ() + dz);
                    if (isValidSpawnPosition(level, testPos)) return testPos.immutable();
                }
            }
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                testPos.set(startPos.getX() + dx, startPos.getY() + 1, startPos.getZ() + dz);
                if (isValidSpawnPosition(level, testPos)) return testPos.immutable();
            }
        }
        for (int dy = -2; dy >= -4; dy--) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    testPos.set(startPos.getX() + dx, startPos.getY() + dy, startPos.getZ() + dz);
                    if (isValidSpawnPosition(level, testPos)) return testPos.immutable();
                }
            }
        }
        return null;
    }

    private static boolean isValidSpawnPosition(ServerLevel level, BlockPos pos) {
        BlockState groundState = level.getBlockState(pos);
        BlockState spawnPosState = level.getBlockState(pos.above());
        BlockState aboveSpawnPosState = level.getBlockState(pos.above(2));
        return groundState.isCollisionShapeFullBlock(level, pos)
                && spawnPosState.isAir()
                && aboveSpawnPosState.isAir();
    }
}