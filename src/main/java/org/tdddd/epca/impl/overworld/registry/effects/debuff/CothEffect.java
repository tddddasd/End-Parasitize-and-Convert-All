package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.ModConfig;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedFox;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSkeleton;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedWolf;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingFoxHead;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;
import org.tdddd.epca.impl.overworld.data.EntityConversionManager;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.utils.EffectApplicationInterceptor;
import org.tdddd.epca.impl.utils.EntityConversionUtil;
import org.tdddd.epca.impl.utils.ParasiteHelper;

import java.util.concurrent.ThreadLocalRandom;

public class CothEffect extends MobEffect implements RemovableEffect {
    private static final int BASE_DURATION = 1200;
    private static final int SPREAD_INTERVAL = 10;
    private static final double SPREAD_RADIUS = 3.0;
    private static final double SMALL_ENTITY_THRESHOLD = 0.517; 
    private static final double LARGE_ENTITY_THRESHOLD = 2.48; 

    
    private static final int PARTICLE_SPAWN_INTERVAL = 10; 
    private static final int MIN_PARTICLES = 3; 
    private static final int MAX_PARTICLES = 5; 
    private static final double PARTICLE_AREA_SIZE = 5.0; 

    
    private static final int LEVEL3_SPREAD_INTERVAL = 20;    
    private static final double LEVEL3_SPREAD_RADIUS = 2.5;  
    private static final int LEVEL3_DURATION = 600;          
    private static final int LEVEL3_AMPLIFIER = 2;           

    public CothEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x990000);
    }

    
    public static boolean applyCothEffect(LivingEntity target, int duration, int amplifier) {
        MobEffectInstance effect = new MobEffectInstance(
                ModEffects.COTH.get(),
                duration,
                amplifier,
                false, false, true
        );

        return EffectApplicationInterceptor.applyEffectSafely(target, effect);
    }

    
    public static void applyCothEffectForce(LivingEntity target, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance(
                ModEffects.COTH.get(),
                duration,
                amplifier,
                false, false, true
        ));
    }

    
    public static boolean canApplyEffect(LivingEntity target, int newAmplifier) {
        MobEffectInstance existingEffect = target.getEffect(ModEffects.COTH.get());

        
        if (existingEffect == null) {
            return true;
        }

        
        if (newAmplifier > existingEffect.getAmplifier()) {
            return true;
        }

        
        return false;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();

        // 客户端：仅生成粒子
        if (level.isClientSide) {
            if (level.getGameTime() % PARTICLE_SPAWN_INTERVAL == 0) {
                spawnCothParticles(entity);
            }
            return;
        }

        // ===== 服务端逻辑 =====
        // 如果是玩家或寄生体，不进行强制转化，走原有逻辑（扩散/升级等）
        if (entity instanceof Player || ParasiteHelper.isParasite(entity)) {
            // 执行原有逻辑（升级、扩散等）
            executeOriginalLogic(entity, amplifier);
            return;
        }

        if (DifficultyEffects.getEffectiveDifficulty(level) == DifficultyLevel.LEGENDARY) {
            // 强制转化（forceConversion = true）
            tryConvertEntity(entity, amplifier, true);
        }

        // 非大师难度：继续走原有的转化规则（等级、血量等条件）
        executeOriginalLogic(entity, amplifier);
    }

    /**
     * 提取原有的逻辑（升级、扩散、等级4触发、普通转化），供非强制转化场景使用
     */
    private void executeOriginalLogic(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide) return;

        if (ModConfig.isParasitePeaceful()) {
            if (!ModConfig.isInTargetWhitelist(entity)) {
                return;
            }
        }

        MobEffectInstance effect = entity.getEffect(ModEffects.COTH.get());
        if (effect == null) return;

        int duration = effect.getDuration();

        // 升级处理
        if (duration <= 1) {
            handleUpgrade(entity, amplifier);
            return;
        }

        // 扩散（等级≥1）
        if (amplifier >= 1 && duration % SPREAD_INTERVAL == 0) {
            spreadEffect(entity, amplifier);
        }

        // 等级3特殊扩散
        if (amplifier >= 5 && !level.isClientSide) {
            long gameTime = level.getGameTime();
            if (gameTime % LEVEL3_SPREAD_INTERVAL == 0) {
                spreadLevel3Effect(entity);
            }
        }

        // 等级4触发（原等级3的逻辑）
        if (amplifier >= 3) {
            CompoundTag tag = entity.getPersistentData();
            String key = "CothLevel4Triggered";
            if (!tag.getBoolean(key)) {
                tag.putBoolean(key, true);

                double x = entity.getX();
                double y = entity.getY();
                double z = entity.getZ();

                if (entity.getBbWidth() < LARGE_ENTITY_THRESHOLD * 0.8 ||
                        entity.getBbHeight() < LARGE_ENTITY_THRESHOLD * 0.8) {
                    tryConvertEntity(entity, amplifier, false);
                }
                if (!(entity instanceof Player)) {
                    applyEffectInArea(entity.level(), x, y, z, 7, 1200, 0);
                }
            }
        }

        // 普通转化尝试（原有条件）
        if (amplifier > 0) {
            tryConvertEntity(entity, amplifier, false);
        }
    }

    
    private void spawnCothParticles(LivingEntity entity) {
        Level level = entity.level();
        double centerX = entity.getX();
        double centerY = entity.getY() + entity.getBbHeight() / 2;
        double centerZ = entity.getZ();

        
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int particleCount = MIN_PARTICLES + random.nextInt(MAX_PARTICLES - MIN_PARTICLES + 1);

        for (int i = 0; i < particleCount; i++) {
            double x = centerX + (random.nextDouble() - 0.5) * PARTICLE_AREA_SIZE;
            double y = centerY + (random.nextDouble() - 0.5) * PARTICLE_AREA_SIZE;
            double z = centerZ + (random.nextDouble() - 0.5) * PARTICLE_AREA_SIZE;
            level.addParticle(ModParticles.COTH.get(), x, y, z, 0, 0, 0);
        }
    }

    public static void tryConvertEntity(LivingEntity entity, int amplifier, boolean forceConversion) {
        if (ModConfig.isInConversionModImmunityWhitelist(entity)) {
            return; 
        }

        
        if ((entity instanceof Player) || ParasiteHelper.isParasite(entity)) {
            return;
        }

        
        if (amplifier >= 4 && entity.isAlive()) {
            
            performConversion(entity);
            return;
        }
        

        
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.getBoolean("BeingConvertedByGnat")) {
            return;
        }
        boolean killedByParasite = persistentData.getBoolean("KilledByParasite");
        boolean isSmallEntity = entity.getBbWidth() < SMALL_ENTITY_THRESHOLD ||
                entity.getBbHeight() < SMALL_ENTITY_THRESHOLD;
        boolean isLargeEntity = entity.getBbWidth() > LARGE_ENTITY_THRESHOLD ||
                entity.getBbHeight() > LARGE_ENTITY_THRESHOLD;

        
        CompoundTag nbt = entity.saveWithoutId(new CompoundTag());
        EntityType<?> entityType = entity.getType();
        EntityConversionManager.EntityConversionRule rule = EntityConversionManager.getConversionRule(entityType, nbt);

        boolean isBaby = nbt.getInt("Age") < 0;
        
        boolean canConvert = false;

        if (forceConversion) {
            
            canConvert = entity.isAlive();
        } else if (amplifier >= 3 && (entity.getBbWidth() < LARGE_ENTITY_THRESHOLD * 1.2 || entity.getBbHeight() < LARGE_ENTITY_THRESHOLD * 1.2)) {  
            
            canConvert = entity.isAlive();
        }
        else if (amplifier >= 2 && rule != null && hasNbtConditions(rule) && EntityConversionManager.checkNBTConditions(rule, nbt) && (entity.getBbWidth() <= LARGE_ENTITY_THRESHOLD || entity.getBbHeight() <= LARGE_ENTITY_THRESHOLD)) {
            
            canConvert = entity.isAlive();
        } else if (amplifier >= 1 && rule != null && hasNbtConditions(rule) && EntityConversionManager.checkNBTConditions(rule, nbt) && isBaby) {
            
            canConvert = entity.isAlive();
        }
        else {
            
            float healthThreshold = 0.3f;
            canConvert = (entity.getHealth() <= entity.getMaxHealth() * healthThreshold) ||
                    (killedByParasite && isSmallEntity && !entity.isAlive());
        }

        if (canConvert) {
            performConversion(entity, rule, nbt, isSmallEntity, isLargeEntity);
            persistentData.remove("KilledByParasite");
        }
    }

    
    private static void performConversion(LivingEntity entity) {
        
        CompoundTag nbt = entity.saveWithoutId(new CompoundTag());
        EntityType<?> entityType = entity.getType();
        EntityConversionManager.EntityConversionRule rule = EntityConversionManager.getConversionRule(entityType, nbt);
        boolean isSmallEntity = entity.getBbWidth() < SMALL_ENTITY_THRESHOLD || entity.getBbHeight() < SMALL_ENTITY_THRESHOLD;
        boolean isLargeEntity = entity.getBbWidth() > LARGE_ENTITY_THRESHOLD || entity.getBbHeight() > LARGE_ENTITY_THRESHOLD;
        performConversion(entity, rule, nbt, isSmallEntity, isLargeEntity);
    }

    private static void performConversion(LivingEntity entity, EntityConversionManager.EntityConversionRule rule, CompoundTag nbt, boolean isSmallEntity, boolean isLargeEntity) {
        
        boolean isFinsConversion = hasFinsNearby(entity) && entity.hasEffect(ModEffects.COTH.get());
        if (isFinsConversion && rule != null && rule.fins_to != null && !rule.fins_to.isEmpty()) {
            convertUsingDataPackRule(entity, rule.fins_to);
            return;
        }

        
        if (rule != null) {
            if (rule.small_entity_priority && isSmallEntity) {
                
                performGenericConversion(entity, isSmallEntity, isLargeEntity);
            }
            String target = EntityConversionManager.getConversionTarget(rule, nbt);
            if (target != null && !target.isEmpty()) {
                
                convertUsingDataPackRule(entity, target);
            } else if (target == null){
                return;
            }
            return;
        }

        
        performGenericConversion(entity, isSmallEntity, isLargeEntity);
    }

    private static boolean hasNbtConditions(EntityConversionManager.EntityConversionRule rule) {
        return rule.nbt_conditions != null && !rule.nbt_conditions.isEmpty();
    }

    private static void convertUsingDataPackRule(LivingEntity entity, String targetEntity) {
        if (targetEntity == null || targetEntity.isEmpty()) {
            return;
        }

        ResourceLocation target = new ResourceLocation(targetEntity);
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(target);

        if (entityType != null && entity.level() instanceof ServerLevel serverLevel) {
            try {
                Entity newEntity = entityType.create(serverLevel);
                if (newEntity != null) {
                    
                    newEntity.setPos(entity.getX(), entity.getY(), entity.getZ());
                    newEntity.setYRot(entity.getYRot());
                    newEntity.setXRot(entity.getXRot());

                    if (entity instanceof Wolf wolf && newEntity instanceof InfestedWolf infestedWolf) {
                        if (wolf.isTame()) {
                            int collarColor = wolf.getCollarColor().getId(); 
                            infestedWolf.setCollarColor(collarColor);
                        }
                        
                    }
                    if (entity instanceof Fox fox && newEntity instanceof InfestedFox infestedFox) {
                        
                        Fox.Type foxVariant = fox.getVariant();
                        if (foxVariant == Fox.Type.SNOW) {
                            infestedFox.setVariant(InfestedFox.Variant.SNOW);
                        } else {
                            
                            infestedFox.setVariant(InfestedFox.Variant.DEFAULT);
                        }
                    }
                    if (entity instanceof Fox fox && newEntity instanceof WalkingFoxHead walkingFoxHead) {
                        
                        Fox.Type foxVariant = fox.getVariant();
                        if (foxVariant == Fox.Type.SNOW) {
                            walkingFoxHead.setVariant(WalkingFoxHead.Variant.SNOW);
                        } else {
                            
                            walkingFoxHead.setVariant(WalkingFoxHead.Variant.DEFAULT);
                        }
                    }
                    
                    if (entity.getType() == EntityType.SKELETON &&
                            target.equals(new ResourceLocation("epca:infested_skeleton")) &&
                            newEntity instanceof InfestedSkeleton infestedSkeleton) {

                        LivingEntity livingEntity = (LivingEntity) entity;
                        ItemStack bow = livingEntity.getItemInHand(InteractionHand.MAIN_HAND);
                        boolean hasFlame = bow.getEnchantmentLevel(Enchantments.FLAMING_ARROWS) > 0;
                        infestedSkeleton.setVariant(hasFlame ?
                                InfestedSkeleton.Variant.FIRED :
                                InfestedSkeleton.Variant.DEFAULT);
                    }

                    
                    playConversionEffects(entity);
                    
                    entity.remove(Entity.RemovalReason.KILLED);
                    entity.teleportTo(1000000, -4000, 1000000);

                    
                    serverLevel.addFreshEntity(newEntity);
                }
            } catch (Exception e) {
            }
        }
    }

    
    private static void performGenericConversion(LivingEntity entity, boolean isSmallEntity, boolean isLargeEntity) {
        
        playConversionEffects(entity);

        
        if (isSmallEntity) {
            if (entity.isInWater()) {
                convertToContaminatedWater(entity);
            } else {
                EntityConversionUtil.convertTo(entity, ModEntities.SMALL_INCOMPLETE_FORM.get());
            }
        } else if (isLargeEntity) {
            EntityConversionUtil.convertTo(entity, ModEntities.LARGE_INCOMPLETE_FORM.get());
        } else {
            EntityConversionUtil.convertTo(entity, ModEntities.MEDIUM_INCOMPLETE_FORM.get());
        }
    }

    
    private static void playConversionEffects(LivingEntity entity) {
        
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.0F, 1.0F);

        spawnConversionParticles(entity);
    }

    private static void spawnConversionParticles(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    entity.getX(), entity.getY(), entity.getZ(),
                    5,
                    0.5, 0.5, 0.5,
                    0.1);
        }
    }

    
    public static void markKilledByParasite(LivingEntity entity) {
        entity.getPersistentData().putBoolean("KilledByParasite", true);
    }

    
    private static boolean hasFinsNearby(LivingEntity entity) {
        AABB area = new AABB(
                entity.getX() - 1.5, entity.getY() - 1.5, entity.getZ() - 1.5,
                entity.getX() + 1.5, entity.getY() + 1.5, entity.getZ() + 1.5
        );

        return !entity.level().getEntitiesOfClass(Fins.class, area).isEmpty();
    }

    private void applyEffectInArea(Level level, double x, double y, double z, double radius, int duration, int amplifier) {
        if (level.isClientSide) return;

        AABB area = new AABB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> !e.hasEffect(ModEffects.COTH.get()) &&
                        !isImmuneEntity(e) &&
                        !ParasiteHelper.isParasite(e) &&
                        (!ModConfig.isParasitePeaceful() || ModConfig.isInTargetWhitelist(e))
        )) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.COTH.get(),
                    duration,
                    amplifier,
                    false, false, true
            ));
        }
    }

    private void handleUpgrade(LivingEntity entity, int currentAmplifier) {
        if (entity instanceof Player) return;

        int newAmplifier = currentAmplifier;

        
        if (currentAmplifier < 5) {
            int nextAmplifier = currentAmplifier + 1;
            
            if (currentAmplifier == 2 && !ModConfig.isCothLevel4Allowed()) {
                nextAmplifier = 2; 
            }
            newAmplifier = nextAmplifier;
        }

        
        if (newAmplifier == 2 && !isImmuneEntity(entity) && !ParasiteHelper.isParasite(entity)) {
            CompoundTag persistentData = entity.getPersistentData();
            String tagKey = "CothLevel3Triggered";

            if (!persistentData.getBoolean(tagKey)) {
                persistentData.putBoolean(tagKey, true);

                if (entity.level() instanceof ServerLevel serverLevel) {
                    EvolutionManager manager = EvolutionManager.forDimension(serverLevel);
                    manager.addPoints(6);
                }
            }
        }

        entity.removeEffect(ModEffects.COTH.get());
        entity.addEffect(new MobEffectInstance(
                ModEffects.COTH.get(),
                BASE_DURATION,
                newAmplifier,
                false, false, true
        ));
    }

    
    private boolean isImmuneEntity(LivingEntity entity) {
        return ModConfig.isInConversionModImmunityWhitelist(entity);
    }

    private void spreadEffect(LivingEntity source, int amplifier) {
        if (source instanceof Player) return;
        if (amplifier < 1) return; 

        AABB area = new AABB(
                source.getX() - SPREAD_RADIUS,
                source.getY() - SPREAD_RADIUS,
                source.getZ() - SPREAD_RADIUS,
                source.getX() + SPREAD_RADIUS,
                source.getY() + SPREAD_RADIUS,
                source.getZ() + SPREAD_RADIUS
        );

        for (LivingEntity target : source.level().getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e != source &&
                        !e.hasEffect(ModEffects.COTH.get()) &&
                        !isImmuneEntity(e) &&
                        !ParasiteHelper.isParasite(e) &&
                        (!ModConfig.isParasitePeaceful() || ModConfig.isInTargetWhitelist(e))
        )) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.COTH.get(),
                    BASE_DURATION,
                    0, 
                    false, false, true
            ));
        }
    }

    
    private void spreadLevel3Effect(LivingEntity source) {
        if (source instanceof Player) return;
        AABB area = new AABB(
                source.getX() - LEVEL3_SPREAD_RADIUS,
                source.getY() - LEVEL3_SPREAD_RADIUS,
                source.getZ() - LEVEL3_SPREAD_RADIUS,
                source.getX() + LEVEL3_SPREAD_RADIUS,
                source.getY() + LEVEL3_SPREAD_RADIUS,
                source.getZ() + LEVEL3_SPREAD_RADIUS
        );

        for (LivingEntity target : source.level().getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e != source &&
                        !isImmuneEntity(e) &&
                        !ParasiteHelper.isParasite(e) &&
                        (!ModConfig.isParasitePeaceful() || ModConfig.isInTargetWhitelist(e))
        )) {
            
            target.addEffect(new MobEffectInstance(
                    ModEffects.COTH.get(),
                    LEVEL3_DURATION,
                    LEVEL3_AMPLIFIER,
                    false, false, true
            ));
        }
    }

    
    private static void convertToContaminatedWater(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel) {
            EntityType<?> entityType = ModEntities.CONTAMINATED_WATER.get();
            
            if (LivingEntity.class.isAssignableFrom(entityType.getBaseClass())) {
                
                EntityConversionUtil.convertTo(entity, (EntityType<? extends LivingEntity>) entityType);
            } else {
                
                convertToNonLivingEntity(entity, entityType);
            }
        }
    }

    
    private static void convertToNonLivingEntity(LivingEntity originalEntity, EntityType<?> targetType) {
        if (!(originalEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        
        Entity newEntity = targetType.create(serverLevel);
        if (newEntity != null) {
            
            newEntity.setPos(originalEntity.getX(), originalEntity.getY(), originalEntity.getZ());
            newEntity.setYRot(originalEntity.getYRot());
            newEntity.setXRot(originalEntity.getXRot());

            playConversionEffects(originalEntity);

            
            originalEntity.remove(Entity.RemovalReason.KILLED);
            originalEntity.teleportTo(1000000, -4000, 1000000);

            
            serverLevel.addFreshEntity(newEntity);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }
}