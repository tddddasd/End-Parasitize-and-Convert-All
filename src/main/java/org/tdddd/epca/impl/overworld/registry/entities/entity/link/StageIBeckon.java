package org.tdddd.epca.impl.overworld.registry.entities.entity.link;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.data.EntityKillCountManager;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.ILink;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class StageIBeckon extends PathfinderMob implements GeoEntity, IParasite, ILink, Enemy {
    private boolean isFoolsBehavior = false;
    private boolean isAprilFoolsDay() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return now.getMonthValue() == 4 && now.getDayOfMonth() == 1;
    }
    private final BlockConversionManager conversionManager = BlockConversionManager.getInstance();
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        
        if (this.entityData.get(IS_RISING)) {
            startRiseEffect();
        }
    }

    
    private static final EntityDataAccessor<Integer> TICK_COUNT = SynchedEntityData.defineId(StageIBeckon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIMATION_STATE = SynchedEntityData.defineId(StageIBeckon.class, EntityDataSerializers.INT);
    
    private static final EntityDataAccessor<Boolean> IS_RISING = SynchedEntityData.defineId(StageIBeckon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RISE_TIMER = SynchedEntityData.defineId(StageIBeckon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(StageIBeckon.class, EntityDataSerializers.FLOAT);
    
    private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(StageIBeckon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(StageIBeckon.class, EntityDataSerializers.FLOAT);

    
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin().thenPlay("open");
    private static final RawAnimation IDLE_OPEN_ANIMATION = RawAnimation.begin().thenLoop("idle_open");
    private static final RawAnimation CLOSE_ANIMATION = RawAnimation.begin().thenPlay("close");

    
    private static final int ANIM_STATE_IDLE = 0;
    private static final int ANIM_STATE_OPEN = 1;
    private static final int ANIM_STATE_IDLE_OPEN = 2;
    private static final int ANIM_STATE_CLOSE = 3;
    private static final int ANIM_STATE_SPAWN = 4;

    private int groundConversionTimer = 0; 
    private int leavesConversionTimer = 0; 
    private BlockPos spawnTargetPos; 
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private int suffocationCooldown = 0; 
    
    private int attackCooldown = 0;      
    private int attackCount = 0;          
    private boolean hasTargets = false;   
    private boolean wasTargeting = false; 
    
    private int viralBombCooldown = 0;   

    private static final int OPEN_ANIMATION_DURATION = 15; 
    private static final int CLOSE_ANIMATION_DURATION = 15; 
    private int forcedAnimationTimer = 0;
    private int forcedAnimationState = ANIM_STATE_IDLE;
    
    private static final int PARTICLE_INTERVAL = 3; 
    private static final float PARTICLE_SPEED = 0.02F; 
    private static final double MAX_ANGLE_RADIANS = Math.toRadians(35); 
    private static final double SPAWN_HEIGHT_OFFSET = 1.0; 
    
    private int autoGrowthTimer = 0;
    private static final int GROWTH_INTERVAL = 300; 
    private static final int GROWTH_AMOUNT = 3;
    private static final int MAX_KILLS_STAGE_I = 64;
    private static final int BIOMASS_COST = 1;
    private static final int PLACE_CORE_COST = 15;
    private static final int PLACE_CORE_RADIUS_MIN = 31;
    private static final int PLACE_CORE_RADIUS_MAX = 32;
    private int placeCoreCooldown = 0;
    private int fastAttackCounter = 0; 
    private static final int AREA_CONVERSION_RADIUS = 9;

    public StageIBeckon(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 25;
        this.noPhysics = false; 

        
        this.setPersistenceRequired();

        
        if (isAprilFoolsDay() && this.random.nextInt(100) < 10) {
            this.isFoolsBehavior = true;
            
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2D);
            
            this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
            this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
            this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TICK_COUNT, 0);
        this.entityData.define(ANIMATION_STATE, ANIM_STATE_IDLE);
        
        this.entityData.define(IS_RISING, false);
        this.entityData.define(RISE_TIMER, 0);
        this.entityData.define(TARGET_Y, 0.0f);
        this.entityData.define(TARGET_X, 0.0f);
        this.entityData.define(TARGET_Z, 0.0f);
    }

    
    public void setRiseTarget(Vec3 targetPosition) {
        this.entityData.set(IS_RISING, true);
        this.entityData.set(RISE_TIMER, 0);
        this.entityData.set(TARGET_Y, (float) targetPosition.y);
        this.entityData.set(TARGET_X, (float) targetPosition.x);
        this.entityData.set(TARGET_Z, (float) targetPosition.z);
        this.spawnTargetPos = BlockPos.containing(targetPosition);

        
        this.setPos(targetPosition.x, targetPosition.y - 3.0, targetPosition.z);

        
        this.entityData.set(ANIMATION_STATE, ANIM_STATE_SPAWN);

        
        startRiseEffect();
    }

    
    private void startRiseEffect() {
        
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.noPhysics = true;

        
        this.entityData.set(RISE_TIMER, 0);
    }

    
    private void finishRiseEffect() {
        
        this.setInvulnerable(false);
        this.setNoGravity(false);
        this.noPhysics = false;

        
        double targetX = this.entityData.get(TARGET_X);
        double targetY = this.entityData.get(TARGET_Y);
        double targetZ = this.entityData.get(TARGET_Z);
        this.setPos(targetX, targetY, targetZ);

        
        this.entityData.set(ANIMATION_STATE, ANIM_STATE_IDLE);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D) 
                .add(Attributes.MOVEMENT_SPEED, 0.0D) 
                .add(Attributes.ATTACK_DAMAGE, 2.5D)
                .build();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (this.entityData.get(IS_RISING)) {
            return false;
        }

        
        if (source.getEntity() instanceof LivingEntity attacker) {
            
            if (shouldIgnoreDamageFrom(attacker)) {
                return false; 
            }
        }

        
        if (!this.level().isClientSide && viralBombCooldown <= 0) {
            spawnViralBombs();
            viralBombCooldown = 60; 
        }

        
        float adjustedAmount = ((IParasite) this).onHurt(source, amount);

        
        boolean result = super.hurt(source, adjustedAmount * 0.4f);

        return result;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true; 
    }

    @Override
    public boolean isPushedByFluid() {
        
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        
    }

    @Override
    public void travel(Vec3 travelVector) {
        
        if (this.entityData.get(IS_RISING)) {
            
            return;
        }

        super.travel(travelVector);
    }

    @Override
    public void tick() {
        
        int currentTick = this.entityData.get(TICK_COUNT);
        this.entityData.set(TICK_COUNT, currentTick + 1);

        
        if (this.entityData.get(IS_RISING)) {
            handleRiseAnimation();
            
            super.tick();
            return;
        }

        
        if (viralBombCooldown > 0) {
            viralBombCooldown--;
        }

        super.tick();

        
        this.hasTargets = hasValidTargets();

        
        updateAnimationState();

        
        if (this.level().isClientSide) {
            
            if (this.entityData.get(ANIMATION_STATE) == ANIM_STATE_IDLE_OPEN) {
                spawnIdleOpenParticles(currentTick);
            }
        }

        
        if (!this.level().isClientSide) {
            int stage = EvolutionManager.getStageForDimension(this.level());

            
            if (stage >= -2 && stage <= 2) {
                this.kill();
                return;
            }

            
            if (this.random.nextFloat() < 0.125f) {
                convertOneBlockByContact();
            }

            
            if (groundConversionTimer <= 0) {
                groundConversionTimer = 20; 
                convertGroundBlock();
            } else {
                groundConversionTimer--;
            }

            
            if (leavesConversionTimer <= 0) {
                leavesConversionTimer = 10; 
                convertLeavesAround();
            } else {
                leavesConversionTimer--;
            }

            
            if (attackCooldown > 0) {
                attackCooldown--;
            }

            
            if (this.isAlive()) {
                if (suffocationCooldown > 0) {
                    suffocationCooldown--;
                }

                
                BlockPos headPos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
                if (this.isInWall() || (!this.level().getFluidState(headPos).is(Fluids.EMPTY) && !this.level().getBlockState(headPos).isAir())) {
                    if (suffocationCooldown <= 0) {
                        
                        suffocationCooldown = 60;
                        
                        destroyBlocksAround(headPos);
                    }
                }
            }

            if (hasTargets && attackCooldown <= 0) {
                if (attackCount < 2) {
                    spawnBiomass();
                    attackCount++;
                } else {
                    attackCount = 0;
                    int currentKills = EntityKillCountManager.getCurrentKillCount(this);
                    boolean isFull = currentKills >= MAX_KILLS_STAGE_I;
                    if (isFull && fastAttackCounter < 10) {
                        attackCooldown = 60;   
                        fastAttackCounter++;
                    } else {
                        attackCooldown = 300;  
                        if (!isFull) {
                            fastAttackCounter = 0;
                        }
                    }
                }
            }
        }

        
        this.wasTargeting = this.hasTargets;

        


        if (--autoGrowthTimer <= 0) {
            autoGrowthTimer = GROWTH_INTERVAL;
            int current = EntityKillCountManager.getCurrentKillCount(this);
            if (current < MAX_KILLS_STAGE_I) {
                int newKills = Math.min(current + GROWTH_AMOUNT, MAX_KILLS_STAGE_I);
                EntityKillCountManager.setKillCount(this, newKills);
            }
        }


        if (EntityKillCountManager.getCurrentKillCount(this) >= PLACE_CORE_COST) {
            
            if (placeCoreCooldown > 0) {
                placeCoreCooldown--;
            } else {
                placeCoreCooldown = 20;
                if (tryPlaceCore()) {
                    
                    EntityKillCountManager.setKillCount(this,
                            EntityKillCountManager.getCurrentKillCount(this) - PLACE_CORE_COST);
                }
            }
        }
    }

    private boolean isBeckonEntityNearby(Level level, BlockPos center, int radius) {
        AABB area = new AABB(center).inflate(radius);
        List<StageIBeckon> stageIList = level.getEntitiesOfClass(StageIBeckon.class, area, Entity::isAlive);
        if (!stageIList.isEmpty()) return true;
        List<StageIIBeckon> stageIIList = level.getEntitiesOfClass(StageIIBeckon.class, area, Entity::isAlive);
        return !stageIIList.isEmpty();
    }

    private boolean tryPlaceCore() {
        Level level = this.level();
        if (!(level instanceof ServerLevel serverLevel)) return false;
        BlockPos center = this.blockPosition();
        int attempts = 100;
        RandomSource random = this.random;
        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = PLACE_CORE_RADIUS_MIN + random.nextDouble() * (PLACE_CORE_RADIUS_MAX - PLACE_CORE_RADIUS_MIN);
            int dx = (int) Math.round(Math.cos(angle) * dist);
            int dz = (int) Math.round(Math.sin(angle) * dist);
            int dy = random.nextInt(33) - 16;
            BlockPos pos = center.offset(dx, dy, dz);
            BlockState state = serverLevel.getBlockState(pos);
            if (isValidBlock(serverLevel, pos, state)) {
                if (!isBeckonCoreNearby(serverLevel, pos, 16) && !isBeckonEntityNearby(serverLevel, pos, 16)) {
                    serverLevel.setBlock(pos, ModBlocks.BECKON_CORE.get().defaultBlockState(), 3);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBeckonCoreNearby(Level level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getBlock() == ModBlocks.BECKON_CORE.get()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isValidBlock(Level level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > 4) return false;
        if (!state.isCollisionShapeFullBlock(level, pos)) return false;

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.isCollisionShapeFullBlock(level, below)) return false;

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.isCollisionShapeFullBlock(level, above)) return false;

        return true;
    }

    
    private void convertOneBlockByContact() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        BlockPos center = this.blockPosition();
        int radius = AREA_CONVERSION_RADIUS; 
        RandomSource random = this.random;

        
        int attempts = 20;
        for (int i = 0; i < attempts; i++) {
            
            int dx = random.nextInt(2 * radius + 1) - radius;
            int dy = random.nextInt(2 * radius + 1) - radius;
            int dz = random.nextInt(2 * radius + 1) - radius;
            BlockPos targetPos = center.offset(dx, dy, dz);

            
            if (serverLevel.getBlockState(targetPos).getBlock() instanceof InfestedBlockInterface) continue;

            
            boolean hasInfestedNeighbor = false;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = targetPos.relative(dir);
                if (serverLevel.getBlockState(neighbor).getBlock() instanceof InfestedBlockInterface) {
                    hasInfestedNeighbor = true;
                    break;
                }
            }
            if (!hasInfestedNeighbor) continue;

            
            boolean converted = conversionManager.convertBlockUsingStageIConfig(serverLevel, targetPos, serverLevel.getBlockState(targetPos));
            if (converted) {
                
                conversionManager.convertPlantsInRangeForStageI(serverLevel, targetPos);
            }
        }
    }

    
    private void spawnIdleOpenParticles(int currentTick) {
        
        if (currentTick % PARTICLE_INTERVAL == 0) {
            
            int particleCount = 1 + this.random.nextInt(2);

            for (int i = 0; i < particleCount; i++) {
                
                double angleYaw = this.random.nextDouble() * 2 * Math.PI; 
                double anglePitch = this.random.nextDouble() * MAX_ANGLE_RADIANS; 

                
                double speedX = Math.sin(angleYaw) * Math.cos(anglePitch) * PARTICLE_SPEED;
                double speedY = Math.sin(anglePitch) * PARTICLE_SPEED;
                double speedZ = Math.cos(angleYaw) * Math.cos(anglePitch) * PARTICLE_SPEED;

                
                double spawnX = this.getX();
                double spawnY = this.getY() - 0.2 + this.getBbHeight() * SPAWN_HEIGHT_OFFSET;
                double spawnZ = this.getZ();

                
                double offsetX = (this.random.nextDouble() - 0.5) * this.getBbWidth() * 0.15;
                double offsetZ = (this.random.nextDouble() - 0.5) * this.getBbWidth() * 0.15;

                
                this.level().addParticle(
                        ModParticles.BIOMASS.get(),
                        spawnX + offsetX,
                        spawnY,
                        spawnZ + offsetZ,
                        speedX,
                        speedY,
                        speedZ
                );
            }
        }
    }

    private void convertGroundBlock() {
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos groundPos = BlockPos.containing(this.getX(), this.getY() - 1, this.getZ());
            BlockState groundState = serverLevel.getBlockState(groundPos);
            conversionManager.convertBlockUsingStageIConfig(serverLevel, groundPos, groundState);
            conversionManager.convertPlantsInRangeForStageI(serverLevel, groundPos);
        }
    }

    private void convertLeavesAround() {
        if (this.level() instanceof ServerLevel serverLevel) {
            conversionManager.convertNearbyLeavesForStageI(serverLevel, this.blockPosition());
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        
        if (!this.level().isClientSide) {
            BlockPos groundPos = BlockPos.containing(this.getX(), this.getY() - 1, this.getZ());
        }

        super.die(damageSource);
        this.onDeath(damageSource); 
    }

    private void handleRiseAnimation() {
        int riseTimer = this.entityData.get(RISE_TIMER);
        riseTimer++;
        this.entityData.set(RISE_TIMER, riseTimer);

        
        if (riseTimer > 60) {
            
            this.entityData.set(IS_RISING, false);
            finishRiseEffect();
            return;
        }

        
        float progress = riseTimer / 60.0f;

        
        double targetY = this.entityData.get(TARGET_Y);
        double startY = targetY - 3.0;
        double currentY = startY + 3.0 * progress;

        
        this.setPos(this.getX(), currentY, this.getZ());

        
        if (this.level() instanceof ServerLevel serverLevel) {
            spawnRiseParticles(serverLevel, progress);
        }
    }

    
    private void spawnRiseParticles(ServerLevel level, float progress) {
        
        double targetX = this.entityData.get(TARGET_X);
        double targetY = this.entityData.get(TARGET_Y);
        double targetZ = this.entityData.get(TARGET_Z);

        BlockPos particleBasePos = BlockPos.containing(targetX, targetY - 1, targetZ);
        BlockState blockState = level.getBlockState(particleBasePos);

        
        if (blockState.isAir()) {
            return;
        }

        
        BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, blockState);

        
        int particleCount = 8;
        double radius = 0.5; 

        for (int i = 0; i < particleCount; i++) {
            
            double angle = this.random.nextDouble() * Math.PI * 2;
            double distance = this.random.nextDouble() * radius;

            double particleX = targetX + Math.cos(angle) * distance;
            double particleY = targetY; 
            double particleZ = targetZ + Math.sin(angle) * distance;

            
            double velocityX = (this.random.nextDouble() - 0.5) * 0.1;
            double velocityY = 0.1 + this.random.nextDouble() * 0.1; 
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.1;

            
            level.sendParticles(
                    particleOption,
                    particleX, particleY, particleZ, 
                    1, 
                    velocityX, velocityY, velocityZ, 
                    0.1 
            );
        }

        
        int currentRiseTimer = this.entityData.get(RISE_TIMER); 
        if (currentRiseTimer % 5 == 0) {
            for (int i = 0; i < 4; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double distance = this.random.nextDouble() * radius;

                double particleX = targetX + Math.cos(angle) * distance;
                double particleY = targetY - 0.9; 
                double particleZ = targetZ + Math.sin(angle) * distance;

                level.sendParticles(
                        particleOption,
                        particleX, particleY, particleZ,
                        1,
                        (this.random.nextDouble() - 0.5) * 0.2,
                        0.05 + this.random.nextDouble() * 0.1,
                        (this.random.nextDouble() - 0.5) * 0.2,
                        0.05
                );
            }
        }
    }

    
    private boolean hasValidTargets() {
        
        if (this.entityData.get(IS_RISING)) {
            return false;
        }

        double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(range))) {
            if (entity instanceof LivingEntity living) {
                
                if (living instanceof Creeper || IParasite.isParasiteByTagOrInterface(living)) {
                    continue;
                }

                
                if (living instanceof Player player) {
                    if (player.isCreative() || player.isSpectator()) {
                        continue;
                    }
                }

                
                if (living.isAlive()) {
                    return true;
                }
            }
        }
        return false;
    }

    
    private void spawnBiomass() {
        if (this.level() instanceof ServerLevelAccessor) {
            
            Vec3 eyePosition = this.getEyePosition();

            
            double angle = this.random.nextDouble() * Math.PI * 2;
            double offsetX = Math.cos(angle) * 0.5; 
            double offsetZ = Math.sin(angle) * 0.5;

            
            double x = eyePosition.x + offsetX;
            double y = eyePosition.y + 0.2; 
            double z = eyePosition.z + offsetZ;

            
            EntityType<?> biomassType = ModEntities.BIOMASS_SMALL.get();
            Entity biomass = biomassType.create(this.level());

            if (biomass != null) {
                
                int current = EntityKillCountManager.getCurrentKillCount(this);
                EntityKillCountManager.setKillCount(this, Math.max(0, current - BIOMASS_COST));
                biomass.setPos(x, y, z);
                this.level().addFreshEntity(biomass);
            }
        }
    }

    
    private void spawnViralBombs() {
        if (this.level() instanceof ServerLevelAccessor) {
            
            int count = 1 + this.random.nextInt(2);

            
            Vec3 centerPosition = this.position();

            for (int i = 0; i < count; i++) {
                
                double angle = this.random.nextDouble() * Math.PI * 2;
                double offsetX = Math.cos(angle) * 0.5; 
                double offsetZ = Math.sin(angle) * 0.5;

                
                double x = centerPosition.x + offsetX;
                double y = centerPosition.y + 0.5; 
                double z = centerPosition.z + offsetZ;

                
                EntityType<?> viralBombType = ModEntities.VIRAL_BOMB.get();
                Entity viralBomb = viralBombType.create(this.level());

                if (viralBomb != null) {
                    viralBomb.setPos(x, y, z);

                    
                    double speed = 0.3 + this.random.nextDouble() * 0.15;
                    viralBomb.setDeltaMovement(
                            Math.cos(angle) * speed,
                            0.1 + this.random.nextDouble() * 0.1, 
                            Math.sin(angle) * speed
                    );

                    this.level().addFreshEntity(viralBomb);
                }
            }
        }
    }

    private void destroyBlocksAround(BlockPos center) {
        int radius = 1; 
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos targetPos = center.offset(x, y, z);
                    BlockState state = level().getBlockState(targetPos);

                    
                    if (state.getDestroySpeed(level(), targetPos) <= 3.0f &&
                            state.getDestroySpeed(level(), targetPos) >= 0.0f &&
                            !state.isAir() &&
                            state.getFluidState().isEmpty()) {
                        
                        level().destroyBlock(targetPos, true, this);
                    }
                }
            }
        }
    }

    
    private void updateAnimationState() {
        
        if (this.entityData.get(IS_RISING)) {
            return;
        }

        int currentAnimState = this.entityData.get(ANIMATION_STATE);

        
        if (forcedAnimationTimer > 0) {
            forcedAnimationTimer--;
            
            this.entityData.set(ANIMATION_STATE, forcedAnimationState);
            return;
        }

        
        if (hasTargets && !wasTargeting) {
            startForcedAnimation(ANIM_STATE_OPEN, OPEN_ANIMATION_DURATION);
        }
        
        else if (hasTargets &&
                (currentAnimState == ANIM_STATE_OPEN)) {
            this.entityData.set(ANIMATION_STATE, ANIM_STATE_IDLE_OPEN);
        }
        
        else if (!hasTargets && wasTargeting) {
            startForcedAnimation(ANIM_STATE_CLOSE, CLOSE_ANIMATION_DURATION);
        }
        
        else if (!hasTargets &&
                (currentAnimState == ANIM_STATE_CLOSE)) {
            this.entityData.set(ANIMATION_STATE, ANIM_STATE_IDLE);
        }
    }

    
    private void startForcedAnimation(int animationState, int duration) {
        this.forcedAnimationState = animationState;
        this.forcedAnimationTimer = duration;
        this.entityData.set(ANIMATION_STATE, animationState);
    }

    
    public static boolean checkStageIBeckonSpawnRules(
            EntityType<StageIBeckon> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        
        return level.getMaxLocalRawBrightness(pos) < 0;
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    
    private PlayState predicate(AnimationState<StageIBeckon> event) {
        AnimationController<StageIBeckon> controller = event.getController();
        int animState = this.entityData.get(ANIMATION_STATE);

        
        switch (animState) {
            case ANIM_STATE_SPAWN:
                controller.setAnimation(SPAWN_ANIMATION);
                break;
            case ANIM_STATE_OPEN:
                controller.setAnimation(OPEN_ANIMATION);
                break;
            case ANIM_STATE_IDLE_OPEN:
                controller.setAnimation(IDLE_OPEN_ANIMATION);
                break;
            case ANIM_STATE_CLOSE:
                controller.setAnimation(CLOSE_ANIMATION);
                break;
            case ANIM_STATE_IDLE:
            default:
                controller.setAnimation(IDLE_ANIMATION);
                break;
        }

        return PlayState.CONTINUE;
    }

    
    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.contains("TickCount")) {
            this.entityData.set(TICK_COUNT, compoundTag.getInt("TickCount"));
        }
        if (compoundTag.contains("AnimationState")) {
            this.entityData.set(ANIMATION_STATE, compoundTag.getInt("AnimationState"));
        }
        
        if (compoundTag.contains("IsRising")) {
            this.entityData.set(IS_RISING, compoundTag.getBoolean("IsRising"));
        }
        if (compoundTag.contains("RiseTimer")) {
            this.entityData.set(RISE_TIMER, compoundTag.getInt("RiseTimer"));
        }
        if (compoundTag.contains("TargetY")) {
            this.entityData.set(TARGET_Y, compoundTag.getFloat("TargetY"));
        }
        if (compoundTag.contains("TargetX")) {
            this.entityData.set(TARGET_X, compoundTag.getFloat("TargetX"));
        }
        if (compoundTag.contains("TargetZ")) {
            this.entityData.set(TARGET_Z, compoundTag.getFloat("TargetZ"));
        }
        
        if (compoundTag.contains("ForcedAnimationTimer")) {
            this.forcedAnimationTimer = compoundTag.getInt("ForcedAnimationTimer");
        }
        if (compoundTag.contains("ForcedAnimationState")) {
            this.forcedAnimationState = compoundTag.getInt("ForcedAnimationState");
        }
        
        if (this.entityData.get(IS_RISING)) {
            startRiseEffect();
        }

        if (compoundTag.contains("FoolsBehavior")) {
            this.isFoolsBehavior = compoundTag.getBoolean("FoolsBehavior");
            
            if (this.isFoolsBehavior && !this.level().isClientSide) {
                
                this.goalSelector.removeGoal(new WaterAvoidingRandomStrollGoal(this, 0.8D));
                this.goalSelector.removeGoal(new LookAtPlayerGoal(this, Player.class, 8.0F));
                this.goalSelector.removeGoal(new RandomLookAroundGoal(this));
                this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
                this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
                this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
            }
        }
        if (compoundTag.contains("AutoGrowthTimer")) {
            this.autoGrowthTimer = compoundTag.getInt("AutoGrowthTimer");
        }
        if (compoundTag.contains("PlaceCoreCooldown")) {
            this.placeCoreCooldown = compoundTag.getInt("PlaceCoreCooldown");
        }
        if (compoundTag.contains("FastAttackCounter")) {
            this.fastAttackCounter = compoundTag.getInt("FastAttackCounter");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("TickCount", this.entityData.get(TICK_COUNT));
        compoundTag.putInt("AnimationState", this.entityData.get(ANIMATION_STATE));
        
        compoundTag.putBoolean("IsRising", this.entityData.get(IS_RISING));
        compoundTag.putInt("RiseTimer", this.entityData.get(RISE_TIMER));
        compoundTag.putFloat("TargetY", this.entityData.get(TARGET_Y));
        compoundTag.putFloat("TargetX", this.entityData.get(TARGET_X));
        compoundTag.putFloat("TargetZ", this.entityData.get(TARGET_Z));
        
        compoundTag.putInt("ForcedAnimationTimer", this.forcedAnimationTimer);
        compoundTag.putInt("ForcedAnimationState", this.forcedAnimationState);
        compoundTag.putBoolean("FoolsBehavior", this.isFoolsBehavior);
        compoundTag.putInt("AutoGrowthTimer", this.autoGrowthTimer);
        compoundTag.putInt("PlaceCoreCooldown", this.placeCoreCooldown);
        compoundTag.putInt("FastAttackCounter", this.fastAttackCounter);
    }

    
    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        
        if (vehicle instanceof Entity && (vehicle instanceof Boat || vehicle instanceof Minecart)) {
            return false;
        }
        return super.startRiding(vehicle, force);
    }

    
    @Override
    protected boolean canRide(Entity entity) {
        
        if (entity instanceof Entity && (entity instanceof Boat || entity instanceof Minecart)) {
            return false;
        }
        return super.canRide(entity);
    }

    
    @Override
    public boolean isPushable() {
        return false; 
    }

    @Override
    public boolean isPickable() {
        return !this.entityData.get(IS_RISING); 
    }

    
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; 
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true; 
    }

    @Override
    public boolean isPersistenceRequired() {
        return true; 
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        
        if (!this.level().isClientSide) {
            
            IParasite.super.onKillEntity(killedEntity);
        }
    }
}