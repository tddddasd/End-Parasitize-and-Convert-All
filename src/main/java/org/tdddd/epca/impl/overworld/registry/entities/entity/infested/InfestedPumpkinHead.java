package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class InfestedPumpkinHead extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float rollAngle = 0.0f;
    public float getRollAngle() {
        return rollAngle;
    }
    private int noTargetTimer = 0;
    private static final int NO_TARGET_DELAY = 100;

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.INFESTED_CARVED_PUMPKIN.get());
    }

    public InfestedPumpkinHead(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 9.0)
                .add(Attributes.ARMOR, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .build();
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            if (canAttackEntity(target)) {
                doHurtTarget(target);
            }

            setMoveSpeed(0.35);
            this.getNavigation().moveTo(target, 1.0);

            long gameTime = this.level().getGameTime();
            if (gameTime % 10 == 0) {
                AABB expandedBox = this.getBoundingBox().inflate(0.025);
                if (expandedBox.intersects(target.getBoundingBox())) {
                    if (!(target instanceof IParasite)) {
                        target.hurt(this.damageSources().inWall(), 2.0F);
                    }
                }
            }

            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }else {
            if (this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() != 0.0) {
                setMoveSpeed(0.0);
                this.getNavigation().stop();
            }
            noTargetTimer++;
            if (noTargetTimer >= NO_TARGET_DELAY) {
                tryTransformToBlock();
                return;
            }
        }

        if (this.level().isClientSide) {
            double dx = this.getX() - this.xo;
            double dz = this.getZ() - this.zo;
            double speed = Math.sqrt(dx * dx + dz * dz);
            final float ROLL_SPEED = -1.0f;        // 每移动1格旋转的弧度数
            final float DECAY = 0.92f;            // 静止时每帧衰减系数（0~1，越小归正越快）
            final float MIN_ANGLE = 0.001f;        // 角度小于此值直接归零，避免浮点余留

            if (speed > 0.001) {
                // 移动时累加角度
                this.rollAngle += (float) (speed * ROLL_SPEED);
                // 限制范围，防止溢出
                this.rollAngle %= (float) (2 * Math.PI);
            } else {
                // 静止时逐渐归正
                this.rollAngle *= DECAY;
                if (Math.abs(this.rollAngle) < MIN_ANGLE) {
                    this.rollAngle = 0.0f;
                }
            }
        }
    }

    private void tryTransformToBlock() {
        if (this.level().isClientSide) return;

        BlockPos center = this.blockPosition();
        java.util.List<BlockPos> validPositions = new java.util.ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(pos);
                    if (state.isAir() || state.canBeReplaced()) {
                        validPositions.add(pos);
                    }
                }
            }
        }

        if (!validPositions.isEmpty()) {
            BlockPos targetPos = validPositions.get(this.random.nextInt(validPositions.size()));
            net.minecraft.core.Direction[] directions = {
                    net.minecraft.core.Direction.NORTH,
                    net.minecraft.core.Direction.SOUTH,
                    net.minecraft.core.Direction.WEST,
                    net.minecraft.core.Direction.EAST
            };
            net.minecraft.core.Direction facing = directions[this.random.nextInt(directions.length)];

            // 使用您的方块注册引用
            net.minecraft.world.level.block.state.BlockState blockState =
                    org.tdddd.epca.impl.overworld.registry.ModBlocks.INFESTED_CARVED_PUMPKIN.get()
                            .defaultBlockState()
                            .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, facing)
                            .setValue(org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedCarvedPumpkin.NATURAL_SPAWN, true);

            this.level().setBlock(targetPos, blockState, 3);
            this.discard();
        }
    }

    private void setMoveSpeed(double speed) {
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
    }

    private boolean canAttackEntity(Entity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq > 1.6 * 1.6) return false;
        AABB self = this.getBoundingBox();
        AABB other = target.getBoundingBox();
        return self.minY < other.maxY && self.maxY > other.minY;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof IParasite) return false;
        if (!canAttackEntity(target)) return false;
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return target.hurt(this.damageSources().mobAttack(this), damage);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack mainHand = attacker.getMainHandItem();
            if (!(mainHand.getItem() instanceof AxeItem)) {
                amount *= 0.75F;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        boolean flag = super.causeFallDamage(fallDistance, damageMultiplier, source);

        if (fallDistance >= 2.0F && !this.level().isClientSide) {
            float damage;
            if (fallDistance <= 12.0F) {
                damage = 20.0F * (fallDistance - 2.0F) / (12.0F - 2.0F); // 0~20
            } else {
                damage = 20.0F;
            }

            AABB box = this.getBoundingBox();
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (entity == this) continue;
                if (entity.getBoundingBox().intersects(box) && entity.getBoundingBox().maxY <= this.getBoundingBox().minY + 0.1) {
                    if (!(entity instanceof IParasite)) {
                        entity.hurt(this.damageSources().fall(), damage);
                    }
                }
            }
        }
        return flag;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static boolean checkInfestedPumpkinHeadSpawnRules(
            EntityType<InfestedPumpkinHead> entityType,
            ServerLevelAccessor levelAccessor,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            if (stage < 2 || stage > 5) return false;

            BlockPos groundPos = pos.below();
            BlockState groundState = levelAccessor.getBlockState(groundPos);
            return groundState.is(Blocks.GRASS_BLOCK) || groundState.is(Blocks.DIRT);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void doPush(Entity entity) {
        if (entity != null && entity.isPushable()) {
            double dx = this.getX() - entity.getX();
            double dz = this.getZ() - entity.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.001) {
                double force = -0.2;
                entity.push(dx / dist * force, 0.0, dz / dist * force);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide && source.getEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            int silkLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, weapon);
            if (silkLevel > 0) {
                ItemStack drop = new ItemStack(ModItems.INFESTED_CARVED_PUMPKIN.get(), 1);
                this.spawnAtLocation(drop);
            }
        }
    }

    @Override
    protected void dropFromLootTable(DamageSource source, boolean causedByPlayer) {
        if (causedByPlayer && source.getEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, weapon) > 0) {
                return;
            }
        }
        super.dropFromLootTable(source, causedByPlayer);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.METAL_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.METAL_BREAK;
    }
}