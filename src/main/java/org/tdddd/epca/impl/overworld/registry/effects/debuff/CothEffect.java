package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.Registries;
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
import org.tdddd.epca.impl.events.PendingConversionManager;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.ColorEffectPacket;

import java.util.Random;
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

    /** COTH II: 0.5s delay before the conversion actually happens. */
    private static final int CONVERSION_DELAY_COTH_II = 10;
    /** COTH III and above: 0.2s delay. */
    private static final int CONVERSION_DELAY_COTH_III = 4;
    /** 0.3s purple fade on the freshly converted entity (6 ticks). */
    private static final int CONVERSION_FADE_TICKS = 6;

    /** ClientColorEffect type for the purple fade-in during the delay. */
    public static final int COLOR_TYPE_CONVERSION = 2;
    /** ClientColorEffect type for the purple fade-out on the new entity. */
    public static final int COLOR_TYPE_CONVERSION_FADE = 3;

    public CothEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x990000);
    }

    // 26.1.2: Entity#saveWithoutId(ValueOutput); EntityConversionManager still matches its datapack rules
    // against a CompoundTag, so serialise through TagValueOutput with the entity's registry context.
    // The produced field set (including "Age") is the same one 1.20.1's saveWithoutId(CompoundTag) wrote.
    private static CompoundTag saveEntityTag(LivingEntity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    public static boolean canApplyInLevel(Level level) {
        return !(DifficultyEffects.isLegendary(level));
    }
    
    public static boolean applyCothEffect(LivingEntity target, int duration, int amplifier) {
        if (!canApplyInLevel(target.level())) return false;
        MobEffectInstance effect = new MobEffectInstance(
                ModEffects.COTH,
                duration,
                amplifier,
                false, false, true
        );

        return EffectApplicationInterceptor.applyEffectSafely(target, effect);
    }

    public static boolean canApplyEffect(LivingEntity target, int newAmplifier) {
        MobEffectInstance existingEffect = target.getEffect(ModEffects.COTH);

        
        if (existingEffect == null) {
            return true;
        }

        
        if (newAmplifier > existingEffect.getAmplifier()) {
            return true;
        }

        
        return false;
    }

    // 26.1.2: applyEffectTick(ServerLevel,LivingEntity,int):boolean. MobEffectInstance#tickServer is the
    // only caller, so the body is now always server side; the returned boolean replaces the old void
    // contract (true == keep ticking).
    
    
    
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        Level level = entity.level();

        if (level.isClientSide()) {
            // unreachable in 26.1.2 (kept so the surrounding structure still reads the same)
            return true;
        }

        
        if (level.getGameTime() % PARTICLE_SPAWN_INTERVAL == 0) {
            spawnCothParticles(entity);
        }

        if (entity instanceof Player || ParasiteHelper.isParasite(entity)) {
            executeOriginalLogic(entity, amplifier);
            return true;
        }

        if (DifficultyEffects.getEffectiveDifficulty(level) == DifficultyLevel.LEGENDARY) {
            tryConvertEntity(entity, amplifier, true);
        }

        executeOriginalLogic(entity, amplifier);
        return true;
    }

    private void executeOriginalLogic(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide()) return;

        if (ModConfig.isParasitePeaceful()) {
            if (!ModConfig.isInTargetWhitelist(entity)) {
                return;
            }
        }

        MobEffectInstance effect = entity.getEffect(ModEffects.COTH);
        if (effect == null) return;

        int duration = effect.getDuration();

        
        if (duration <= 1) {
            handleUpgrade(entity, amplifier);
            return;
        }

        
        if (amplifier >= 1 && duration % SPREAD_INTERVAL == 0) {
            spreadEffect(entity, amplifier);
        }

        
        if (amplifier >= 5 && !level.isClientSide()) {
            long gameTime = level.getGameTime();
            if (gameTime % LEVEL3_SPREAD_INTERVAL == 0) {
                spreadLevel3Effect(entity);
            }
        }

        
        if (amplifier >= 3) {
            CompoundTag tag = entity.getPersistentData();
            String key = "CothLevel4Triggered";
            if (!tag.getBoolean(key).orElse(false)) {
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

        // 26.1.2: Level#addParticle is client-only and applyEffectTick no longer runs on the client,
        // so the identical ClientboundLevelParticlesPacket is emitted from the server instead:
        // ServerLevel#sendParticles(p_type, x, y, z, count, xDist, yDist, zDist, speed) sends
        //   x + (rand-0.5)*2*xDist  etc.
        // 1.20.1 scattered each particle uniformly over +-PARTICLE_AREA_SIZE/2 (PARTICLE_AREA_SIZE = 5),
        // so xDist = PARTICLE_AREA_SIZE / 2 reproduces the same +-2.5 cube around the entity with the
        // same particle count. The in-box distribution is uniform rather than triangular - see
        // PORTING-NOTES-w2-effects.md.
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.COTH.get(),
                    centerX, centerY, centerZ,
                    particleCount,
                    PARTICLE_AREA_SIZE / 2.0, PARTICLE_AREA_SIZE / 2.0, PARTICLE_AREA_SIZE / 2.0,
                    0.0);
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
            
            CompoundTag forceNbt = saveEntityTag(entity);
            EntityConversionManager.EntityConversionRule forceRule = EntityConversionManager.getConversionRule(entity.getType(), forceNbt);
            boolean forceSmall = entity.getBbWidth() < SMALL_ENTITY_THRESHOLD || entity.getBbHeight() < SMALL_ENTITY_THRESHOLD;
            boolean forceLarge = entity.getBbWidth() > LARGE_ENTITY_THRESHOLD || entity.getBbHeight() > LARGE_ENTITY_THRESHOLD;
                ConversionPlan forcePlan = planConversion(entity, forceRule, forceNbt, forceSmall, forceLarge);
                scheduleConversion(entity, forcePlan,
                        isConfiguredConversion(forceRule, forcePlan) ? conversionDelayTicks(amplifier) : 0);
            return;
        }
        

        
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.getBoolean("BeingConvertedByGnat").orElse(false)) {
            return;
        }
        boolean killedByParasite = persistentData.getBoolean("KilledByParasite").orElse(false);
        boolean isSmallEntity = entity.getBbWidth() < SMALL_ENTITY_THRESHOLD ||
                entity.getBbHeight() < SMALL_ENTITY_THRESHOLD;
        boolean isLargeEntity = entity.getBbWidth() > LARGE_ENTITY_THRESHOLD ||
                entity.getBbHeight() > LARGE_ENTITY_THRESHOLD;

        
        CompoundTag nbt = saveEntityTag(entity);
        EntityType<?> entityType = entity.getType();
        EntityConversionManager.EntityConversionRule rule = EntityConversionManager.getConversionRule(entityType, nbt);

        boolean isBaby = nbt.getInt("Age").orElse(0) < 0;
        
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
            ConversionPlan plan = planConversion(entity, rule, nbt, isSmallEntity, isLargeEntity);
                
                int delay = isConfiguredConversion(rule, plan) ? conversionDelayTicks(amplifier) : 0;
            if (delay <= 0) {
                executePlan(entity, plan);
            } else {
                PendingConversionManager.schedule(entity, plan, delay);
            }
            persistentData.remove("KilledByParasite");
        }
    }

    /**
     * Delay in ticks between the moment the conversion conditions pass and the moment the
     * conversion is executed.
     *
     * <ul>
     *   <li>{@code amplifier <= 0} (COTH I / no amplification) - 0 ticks, immediate as before;</li>
     *   <li>{@code amplifier == 1} (COTH II) - 10 ticks (0.5s);</li>
     *   <li>{@code amplifier >= 2} (COTH III and above) - 4 ticks (0.2s).</li>
     * </ul>
     */
    
    private static void scheduleConversion(LivingEntity entity, ConversionPlan plan, int delayTicks) {
        if (delayTicks <= 0) {
            executePlan(entity, plan);
        } else {
            PendingConversionManager.schedule(entity, plan, delayTicks);
        }
    }
    public static int conversionDelayTicks(int amplifier) {
        if (amplifier <= 0) return 0;
        if (amplifier == 1) return CONVERSION_DELAY_COTH_II;
        return CONVERSION_DELAY_COTH_III;
    }

    /**
     * Resolves what {@link #performConversion(LivingEntity, EntityConversionManager.EntityConversionRule,
     * CompoundTag, boolean, boolean)} would do, without doing it.
     *
     * <p>Kept branch for branch identical to the original method, including the
     * {@code small_entity_priority} quirk where a generic conversion happens and the data pack
     * target is still resolved afterwards.</p>
     */
    private static ConversionPlan planConversion(LivingEntity entity, EntityConversionManager.EntityConversionRule rule,
                                                 CompoundTag nbt, boolean isSmallEntity, boolean isLargeEntity) {
        boolean doGeneric = false;
        String target = null;

        
        boolean isFinsConversion = hasFinsNearby(entity) && entity.hasEffect(ModEffects.COTH);
        if (isFinsConversion && rule != null && rule.fins_to != null && !rule.fins_to.isEmpty()) {
            return new ConversionPlan(true, rule.fins_to);
        }

        
        if (rule != null) {
            if (rule.small_entity_priority && isSmallEntity) {
                doGeneric = true;
            }
            target = EntityConversionManager.getConversionTarget(rule, nbt);
        } else {
            
            doGeneric = true;
        }

        return new ConversionPlan(doGeneric, target);
    }

    /**
     * Executes a plan produced by {@link #planConversion}. Called either immediately (delay 0) or
     * later by {@link PendingConversionManager}.
     */
    public static void executePlan(LivingEntity entity, ConversionPlan plan) {
        if (plan == null || plan.isEmpty()) {
            return;
        }

        if (plan.targetEntity != null && !plan.targetEntity.isEmpty()) {
            convertUsingDataPackRule(entity, plan.targetEntity);
        }

        if (plan.generic) {
            performGenericConversion(entity);
        }
    }

    /**
     * A deferred conversion: what to convert into, and whether the generic
     * (small/medium/large incomplete form) conversion still applies.
     *
     * <p>{@code targetEntity == null && !generic} means the original code did nothing for this
     * entity (unresolvable target), and must keep doing nothing.</p>
     */
    public static final class ConversionPlan {
        public static final ConversionPlan NONE = new ConversionPlan(false, null);

        final boolean generic;
        final String targetEntity;

        ConversionPlan(boolean generic, String targetEntity) {
            this.generic = generic;
            this.targetEntity = targetEntity;
        }

        boolean isEmpty() {
            return !generic && (targetEntity == null || targetEntity.isEmpty());
        }
    }

    
    
    
    public static boolean isConfiguredConversion(
            EntityConversionManager.EntityConversionRule rule, ConversionPlan plan) {
        return rule != null && !planIsEmpty(plan);
    }

    public static boolean planIsEmpty(ConversionPlan plan) {
        return plan == null || plan.isEmpty();
    }

    private static boolean hasNbtConditions(EntityConversionManager.EntityConversionRule rule) {
        return rule.nbt_conditions != null && !rule.nbt_conditions.isEmpty();
    }

    private static void convertUsingDataPackRule(LivingEntity entity, String targetEntity) {
        if (targetEntity == null || targetEntity.isEmpty()) {
            return;
        }

        Identifier target = Identifier.parse(targetEntity);
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(target);

        if (entityType != null && entity.level() instanceof ServerLevel serverLevel) {
            try {
                Entity newEntity = entityType.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
                if (newEntity != null) {
                    
                    newEntity.setPos(entity.getX(), entity.getY(), entity.getZ());
                    newEntity.setYRot(entity.getYRot());
                    newEntity.setXRot(entity.getXRot());

                    if (entity instanceof Wolf wolf && newEntity instanceof InfestedWolf infestedWolf) {
                        if (wolf.isTame()) {
                            int collarColor = wolf.getCollarColor().getId(); 
                            infestedWolf.setCollarColor(collarColor);
                        }
                        // 26.1.2: TamableAnimal#getOwnerUUID() is gone; the owner is stored as an
                        // EntityReference<LivingEntity>. InfestedWolf still carries a plain UUID.
                        EntityReference<LivingEntity> ownerReference = wolf.getOwnerReference();
                        if (ownerReference != null) {
                            infestedWolf.setOwnerUUID(ownerReference.getUUID());
                        }
                        
                    }
                    if (entity instanceof Fox fox && newEntity instanceof InfestedFox infestedFox) {
                        
                        Fox.Variant foxVariant = fox.getVariant();
                        if (foxVariant == Fox.Variant.SNOW) {
                            infestedFox.setVariant(InfestedFox.Variant.SNOW);
                        } else {
                            infestedFox.setVariant(InfestedFox.Variant.DEFAULT);
                        }
                    }
                    if (entity instanceof Fox fox && newEntity instanceof WalkingFoxHead walkingFoxHead) {
                        
                        Fox.Variant foxVariant = fox.getVariant();
                        if (foxVariant == Fox.Variant.SNOW) {
                            walkingFoxHead.setVariant(WalkingFoxHead.Variant.SNOW);
                        } else {
                            
                            walkingFoxHead.setVariant(WalkingFoxHead.Variant.DEFAULT);
                        }
                    }
                    
                    if (entity.getType() == EntityType.SKELETON &&
                            target.equals(Identifier.parse("epca:infested_skeleton")) &&
                            newEntity instanceof InfestedSkeleton infestedSkeleton) {

                        LivingEntity livingEntity = (LivingEntity) entity;
                        ItemStack bow = livingEntity.getItemInHand(InteractionHand.MAIN_HAND);
                        // 26.1.2: ItemStack#getEnchantmentLevel takes a Holder<Enchantment>; enchantments are
                        // a datapack registry, so Enchantments.FLAME (a ResourceKey) is resolved through the
                        // entity's registry access.
                        Holder<Enchantment> flame = entity.registryAccess()
                                .lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.FLAME);
                        boolean hasFlame = bow.getEnchantmentLevel(flame) > 0;
                        infestedSkeleton.setVariant(hasFlame ?
                                InfestedSkeleton.Variant.FIRED :
                                InfestedSkeleton.Variant.DEFAULT);
                    }

                    if (entity.hasCustomName()) {
                        newEntity.setCustomName(entity.getCustomName());
                        newEntity.setCustomNameVisible(entity.isCustomNameVisible());
                    }

                    playConversionEffects(entity);
                    entity.remove(Entity.RemovalReason.KILLED);
                    entity.teleportTo(1000000, -4000, 1000000);
                    
                    serverLevel.addFreshEntity(newEntity);

                    notifyConvertedEntity(newEntity);
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

    /** Recomputes the size classification; used when only the entity is known (delayed plans). */
    private static void performGenericConversion(LivingEntity entity) {
        boolean isSmallEntity = entity.getBbWidth() < SMALL_ENTITY_THRESHOLD || entity.getBbHeight() < SMALL_ENTITY_THRESHOLD;
        boolean isLargeEntity = entity.getBbWidth() > LARGE_ENTITY_THRESHOLD || entity.getBbHeight() > LARGE_ENTITY_THRESHOLD;
        performGenericConversion(entity, isSmallEntity, isLargeEntity);
    }

    
    private static void playConversionEffects(LivingEntity entity) {
        
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.0F, 1.0F);

        spawnConversionParticles(entity);
    }

    
    private static void spawnConversionParticles(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        CompoundTag nbt = saveEntityTag(entity);
        EntityConversionManager.EntityConversionRule rule =
                EntityConversionManager.getConversionRule(entity.getType(), nbt);
        if (rule != null && !rule.shouldSpawnMeatParticles()) {
            return;
        }

        Random random = ThreadLocalRandom.current();
        int count = 3 + random.nextInt(3);
        serverLevel.sendParticles(ModParticles.LIVING_FLESH.get(),
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                count, 0.5, 0.5, 0.5, 0.05);
    }

    /**
     * Tells tracking clients that a freshly converted entity should fade out its purple tint
     * over 0.3s. Non-living results (contaminated water) have no colour layer and are skipped.
     */
    public static void notifyConvertedEntity(Entity newEntity) {
        if (newEntity instanceof LivingEntity living) {
            ModNetwork.sendToAllTracking(living,
                    new ColorEffectPacket(living, COLOR_TYPE_CONVERSION_FADE, CONVERSION_FADE_TICKS));
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
        if (DifficultyEffects.isLegendary(level)) return;
        if (level.isClientSide()) return;

        AABB area = new AABB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> !e.hasEffect(ModEffects.COTH) &&
                        !isImmuneEntity(e) &&
                        !ParasiteHelper.isParasite(e) &&
                        (!ModConfig.isParasitePeaceful() || ModConfig.isInTargetWhitelist(e))
        )) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.COTH,
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

            if (!persistentData.getBoolean(tagKey).orElse(false)) {
                persistentData.putBoolean(tagKey, true);

                if (entity.level() instanceof ServerLevel serverLevel) {
                    EvolutionManager manager = EvolutionManager.forDimension(serverLevel);
                    manager.addPoints(6);
                }
            }
        }

        entity.removeEffect(ModEffects.COTH);
        entity.addEffect(new MobEffectInstance(
                ModEffects.COTH,
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
                        !e.hasEffect(ModEffects.COTH) &&
                        !isImmuneEntity(e) &&
                        !ParasiteHelper.isParasite(e) &&
                        (!ModConfig.isParasitePeaceful() || ModConfig.isInTargetWhitelist(e))
        )) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.COTH,
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
                    ModEffects.COTH,
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

        
        Entity newEntity = targetType.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
        if (newEntity != null) {
            
            newEntity.setPos(originalEntity.getX(), originalEntity.getY(), originalEntity.getZ());
            newEntity.setYRot(originalEntity.getYRot());
            newEntity.setXRot(originalEntity.getXRot());

            playConversionEffects(originalEntity);

            
            originalEntity.remove(Entity.RemovalReason.KILLED);
            originalEntity.teleportTo(1000000, -4000, 1000000);

            
            serverLevel.addFreshEntity(newEntity);

            notifyConvertedEntity(newEntity);
        }
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // 1.20.1 returned true unconditionally (the upgrade/spread/level3 cadences are computed inside the
    // body from the effect's own duration), so the effect must still be ticked every tick.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }
}
