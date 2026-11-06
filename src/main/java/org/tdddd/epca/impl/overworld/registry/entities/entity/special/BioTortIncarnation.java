package org.tdddd.epca.impl.overworld.registry.entities.entity.special;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.events.BioTortMarkHandler;
import org.tdddd.epca.impl.events.PlayerPositionHistory;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.ScreenCorruptionPacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncHotbarMarkPacket;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BioTortIncarnation extends PathfinderMob  {

    public static final float FIXED_HEALTH = 20.0F;

    public static final float MAGIC_DAMAGE = 44.0F;

    private static final EntityDataAccessor<Boolean> DATA_HAS_REVENGE =
            SynchedEntityData.defineId(BioTortIncarnation.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_SHOWN_JOIN_MESSAGE =
            SynchedEntityData.defineId(BioTortIncarnation.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO =
            SynchedEntityData.defineId(BioTortIncarnation.class, EntityDataSerializers.BOOLEAN);

    private UUID lastAttacker;
    private int revengeCooldown = 0;
    private int attackStrengthTicker;

    private final Map<UUID, Integer> attackCounts = new HashMap<>();

    private final Map<UUID, Integer> phaseTwoAttackCounts = new HashMap<>();

    private final Map<UUID, HotbarMark> hotbarMarks = new HashMap<>();

    private final List<CorruptionEffect> corruptionEffects = new ArrayList<>();

    private int markCycleTicker;

    private static final class HotbarMark {

        private final int slotMask;

        private final long expireTick;

        private long nextDamageTick;

        private HotbarMark(int slotMask, long expireTick, long nextDamageTick) {
            this.slotMask = slotMask;
            this.expireTick = expireTick;
            this.nextDamageTick = nextDamageTick;
        }

        private boolean isSlotMarked(int slot) {
            return (this.slotMask & (1 << slot)) != 0;
        }
    }

    private static final class CorruptionEffect {

        private final ServerPlayer player;

        @Nullable
        private final PlayerPositionHistory.Sample teleportTarget;

        private int elapsedTicks;

        private boolean teleported;

        private CorruptionEffect(ServerPlayer player, @Nullable PlayerPositionHistory.Sample teleportTarget) {
            this.player = player;
            this.teleportTarget = teleportTarget;
        }
    }

    public BioTortIncarnation(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.navigation = new GroundPathNavigation(this, level);

        super.setHealth(FIXED_HEALTH);
    }

        public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HAS_REVENGE, false);
        this.entityData.define(DATA_HAS_SHOWN_JOIN_MESSAGE, false);
        this.entityData.define(DATA_PHASE_TWO, false);
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            this.lockMaxHealthAttribute();
        }

        if (!this.level().isClientSide) {
            this.tickBioTortSkills();
        }

        if (!this.level().isClientSide && !this.entityData.get(DATA_HAS_SHOWN_JOIN_MESSAGE)) {
            this.showJoinMessage();
            this.entityData.set(DATA_HAS_SHOWN_JOIN_MESSAGE, true);
        }

        if (this.attackStrengthTicker > 0) {
            --this.attackStrengthTicker;
        }

        if (revengeCooldown > 0) {
            revengeCooldown--;
            if (revengeCooldown <= 0) {
                this.entityData.set(DATA_HAS_REVENGE, false);
                this.lastAttacker = null;
            }
        }
    }

    private void showJoinMessage() {
        if (this.level() instanceof ServerLevel serverLevel) {

            Component joinMessage = Component.translatable("entity.epca.bio_tort_incarnation.join");

            serverLevel.getServer().getPlayerList().broadcastSystemMessage(joinMessage, false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return false;
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity && attacker != this && revengeCooldown <= 0) {
            this.lastAttacker = attacker.getUUID();
            this.entityData.set(DATA_HAS_REVENGE, true);
            this.revengeCooldown = 100;

            if (attacker instanceof LivingEntity livingAttacker) {
                this.setLastHurtByMob(livingAttacker);
            }
        }

        boolean registered = super.hurt(source, amount);

        if (registered && attacker instanceof ServerPlayer serverPlayer) {
            this.onPlayerAttack(serverPlayer);
        }

        return registered;
    }

    @Override
    public boolean doHurtTarget(Entity target) {

        this.playSound(SoundEvents.PLAYER_ATTACK_WEAK, 1.0F, 1.0F);

        boolean attacked = target.hurt(this.damageSources().indirectMagic(this, this), MAGIC_DAMAGE);

        if (attacked) {

            this.setLastHurtMob(target);
            this.attackStrengthTicker = 20;
        }

        return attacked;
    }

    private float getWeaponAttackDamage(ItemStack stack) {
        double attackDamage = stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE)
                .stream()
                .mapToDouble(AttributeModifier::getAmount)
                .sum();

        return (float) attackDamage;
    }

    public float getAttackDamage() {
        ItemStack mainHandItem = this.getMainHandItem();
        float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

        if (!mainHandItem.isEmpty()) {
            baseDamage += getWeaponAttackDamage(mainHandItem);
        }

        float cooldownPercent = this.getAttackStrengthScale(0.0F);
        return baseDamage * (0.2F + cooldownPercent * cooldownPercent * 0.8F);
    }

    public float getAttackStrengthScale(float adjustTicks) {
        float attackStrength = (float) this.attackStrengthTicker - adjustTicks;
        if (attackStrength < 0.0F) {
            attackStrength = 0.0F;
        }

        return attackStrength / 20.0F;
    }

    @Override
    public ItemStack getMainHandItem() {
        return this.getItemInHand(InteractionHand.MAIN_HAND);
    }

    @Override
    public ItemStack getOffhandItem() {
        return this.getItemInHand(InteractionHand.OFF_HAND);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PLAYER_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HasRevenge", this.entityData.get(DATA_HAS_REVENGE));
        compound.putBoolean("HasShownJoinMessage", this.entityData.get(DATA_HAS_SHOWN_JOIN_MESSAGE));
        compound.putInt("RevengeCooldown", this.revengeCooldown);
        compound.putInt("AttackStrengthTicker", this.attackStrengthTicker);
        if (this.lastAttacker != null) {
            compound.putUUID("LastAttacker", this.lastAttacker);
        }

        compound.putBoolean("PhaseTwo", this.entityData.get(DATA_PHASE_TWO));
        compound.putInt("MarkCycleTicker", this.markCycleTicker);
        compound.put("AttackCounts", writeCountMap(this.attackCounts));
        compound.put("PhaseTwoAttackCounts", writeCountMap(this.phaseTwoAttackCounts));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DATA_HAS_REVENGE, compound.getBoolean("HasRevenge"));
        this.entityData.set(DATA_HAS_SHOWN_JOIN_MESSAGE, compound.getBoolean("HasShownJoinMessage"));
        this.revengeCooldown = compound.getInt("RevengeCooldown");
        this.attackStrengthTicker = compound.getInt("AttackStrengthTicker");
        if (compound.hasUUID("LastAttacker")) {
            this.lastAttacker = compound.getUUID("LastAttacker");
        }

        this.entityData.set(DATA_PHASE_TWO, compound.getBoolean("PhaseTwo"));
        if (compound.contains("MarkCycleTicker")) {
            this.markCycleTicker = compound.getInt("MarkCycleTicker");
        }
        readCountMap(compound.getCompound("AttackCounts"), this.attackCounts);
        readCountMap(compound.getCompound("PhaseTwoAttackCounts"), this.phaseTwoAttackCounts);
    }

    private static CompoundTag writeCountMap(Map<UUID, Integer> counts) {
        CompoundTag tag = new CompoundTag();
        counts.forEach((uuid, value) -> tag.putInt(uuid.toString(), value));
        return tag;
    }

    private static void readCountMap(CompoundTag tag, Map<UUID, Integer> counts) {
        counts.clear();
        for (String key : tag.getAllKeys()) {
            try {
                counts.put(UUID.fromString(key), tag.getInt(key));
            } catch (IllegalArgumentException ignored) {

            }
        }
    }

    @Override
    public boolean canHoldItem(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canTakeItem(ItemStack itemstack) {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);

        if (!this.getMainHandItem().isEmpty()) {
            this.spawnAtLocation(this.getMainHandItem().copy());
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return true;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return true;
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    public boolean isPhaseTwo() {
        return this.entityData.get(DATA_PHASE_TWO);
    }

    private void tickBioTortSkills() {
        long now = this.level().getGameTime();
        this.tickCorruptionEffects();
        this.tickHotbarMarks(now);
    }

    private void tickHotbarMarks(long now) {

        if (++this.markCycleTicker >= BioTortSkillConstants.MARK_INTERVAL_TICKS) {
            this.markCycleTicker = 0;
            this.startNewMarkCycle(now);
            BioTortMarkHandler.pruneExpired(now);
        }

        Iterator<Map.Entry<UUID, HotbarMark>> markIterator = this.hotbarMarks.entrySet().iterator();
        while (markIterator.hasNext()) {
            Map.Entry<UUID, HotbarMark> entry = markIterator.next();
            ServerPlayer player = this.findServerPlayer(entry.getKey());
            if (player == null || !this.isValidMarkVictim(player)) {
                if (player != null) {
                    ModNetwork.sendToPlayer(player, SyncHotbarMarkPacket.clear());
                }
                markIterator.remove();
            }
        }

        for (Map.Entry<UUID, HotbarMark> entry : this.hotbarMarks.entrySet()) {
            ServerPlayer player = this.findServerPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            HotbarMark mark = entry.getValue();
            int selected = player.getInventory().selected;

            if (mark.isSlotMarked(selected) && now >= mark.nextDamageTick) {
                this.applyMinimumDamage(player, BioTortSkillConstants.SELECTED_SLOT_DAMAGE);
                mark.nextDamageTick = now + BioTortSkillConstants.SELECTED_SLOT_DAMAGE_INTERVAL_TICKS;
            }
        }
    }

    private void startNewMarkCycle(long now) {
        Map<UUID, HotbarMark> next = new HashMap<>();
        Set<UUID> markedPlayers = new HashSet<>();

        for (LivingEntity victim : this.collectMarkVictims()) {
            if (victim instanceof ServerPlayer player) {
                int mask = this.rollHotbarMask();
                next.put(player.getUUID(), new HotbarMark(mask, now + BioTortSkillConstants.MARK_DURATION_TICKS, now));
                markedPlayers.add(player.getUUID());

                ModNetwork.sendToPlayer(player, SyncHotbarMarkPacket.mark(mask,
                        BioTortSkillConstants.MARK_DURATION_TICKS));
            } else {

                BioTortMarkHandler.markCreature(victim.getUUID(), now,
                        BioTortSkillConstants.MARK_DURATION_TICKS);
            }
        }

        for (UUID previous : this.hotbarMarks.keySet()) {
            if (!markedPlayers.contains(previous)) {
                ServerPlayer player = this.findServerPlayer(previous);
                if (player != null) {
                    ModNetwork.sendToPlayer(player, SyncHotbarMarkPacket.clear());
                }
            }
        }

        this.hotbarMarks.clear();
        this.hotbarMarks.putAll(next);
    }

    private int rollHotbarMask() {
        int mask = 0;
        int wanted = Math.min(BioTortSkillConstants.MARKED_SLOT_COUNT,
                BioTortSkillConstants.MARKED_SLOT_MAX - BioTortSkillConstants.MARKED_SLOT_MIN + 1);
        while (Integer.bitCount(mask) < wanted) {
            int slot = BioTortSkillConstants.MARKED_SLOT_MIN
                    + this.getRandom().nextInt(BioTortSkillConstants.MARKED_SLOT_MAX
                    - BioTortSkillConstants.MARKED_SLOT_MIN + 1);
            mask |= 1 << slot;
        }
        return mask;
    }

    private List<LivingEntity> collectMarkVictims() {
        List<LivingEntity> victims = new ArrayList<>(2);

        LivingEntity target = this.getTarget();
        if (this.isValidMarkVictim(target)) {
            victims.add(target);
        }

        if (this.lastAttacker != null && this.entityData.get(DATA_HAS_REVENGE)
                && this.level() instanceof ServerLevel serverLevel) {
            Entity attacker = serverLevel.getEntity(this.lastAttacker);
            if (attacker instanceof LivingEntity living && !victims.contains(living)
                    && this.isValidMarkVictim(living)) {
                victims.add(living);
            }
        }

        return victims;
    }

    private boolean isValidMarkVictim(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (living == this || !living.isAlive() || living.isRemoved()) {
            return false;
        }
        if (living.level() != this.level()) {
            return false;
        }
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        double range = BioTortSkillConstants.VICTIM_RANGE;
        return this.distanceToSqr(living) <= range * range;
    }

    private void applyMinimumDamage(LivingEntity target, float amount) {
        Registry<DamageType> registry = this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder, this, this);
        target.hurt(minimumSource, amount);
    }

    @Nullable
    private ServerPlayer findServerPlayer(UUID playerId) {
        if (this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(playerId) instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private void onPlayerAttack(ServerPlayer player) {
        UUID playerId = player.getUUID();
        int total = this.attackCounts.merge(playerId, 1, Integer::sum);

        if (!this.isPhaseTwo()) {
            if (total >= BioTortSkillConstants.PHASE2_ATTACK_THRESHOLD) {
                this.enterPhaseTwo();
            }
            return;
        }

        if (this.hasCorruptionEffect(playerId)) {
            return;
        }

        int phaseTwoAttacks = this.phaseTwoAttackCounts.merge(playerId, 1, Integer::sum);
        if (phaseTwoAttacks % BioTortSkillConstants.PHASE2_ATTACKS_PER_EFFECT == 0) {
            this.startCorruptionEffect(player);
        }
    }

    private void enterPhaseTwo() {
        this.entityData.set(DATA_PHASE_TWO, true);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("entity.epca.bio_tort_incarnation.phase_two"), false);
        }
    }

    private boolean hasCorruptionEffect(UUID playerId) {
        for (CorruptionEffect effect : this.corruptionEffects) {
            if (effect.player.getUUID().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    private void startCorruptionEffect(ServerPlayer player) {
        long now = this.level().getGameTime();
        PlayerPositionHistory.Sample target = PlayerPositionHistory.sampleAt(player.getUUID(),
                now - BioTortSkillConstants.POSITION_LOOKBACK_TICKS);
        this.corruptionEffects.add(new CorruptionEffect(player, target));
        ModNetwork.sendToPlayer(player, ScreenCorruptionPacket.start(
                BioTortSkillConstants.CORRUPTION_TICKS, BioTortSkillConstants.CORRUPTION_DECAY_TICKS));

        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void tickCorruptionEffects() {
        if (this.corruptionEffects.isEmpty()) {
            return;
        }

        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        Iterator<CorruptionEffect> iterator = this.corruptionEffects.iterator();
        while (iterator.hasNext()) {
            CorruptionEffect effect = iterator.next();
            ServerPlayer player = effect.player;
            if (player.isRemoved() || player.isDeadOrDying()) {
                if (!player.isRemoved()) {
                    ModNetwork.sendToPlayer(player, ScreenCorruptionPacket.stop());
                }
                iterator.remove();
                continue;
            }

            effect.elapsedTicks++;

            if (!effect.teleported && effect.elapsedTicks >= BioTortSkillConstants.CORRUPTION_TELEPORT_TICKS) {
                effect.teleported = true;
                this.teleportBack(player, effect.teleportTarget);
            }

            if (effect.elapsedTicks >= BioTortSkillConstants.CORRUPTION_TOTAL_TICKS) {
                ModNetwork.sendToPlayer(player, ScreenCorruptionPacket.stop());
                iterator.remove();
            }
        }
    }

    private void teleportBack(ServerPlayer player, @Nullable PlayerPositionHistory.Sample target) {
        if (target == null) {
            return;
        }
        player.teleportTo(target.level, target.x, target.y, target.z, target.yRot, target.xRot);
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || !this.corruptionEffects.isEmpty();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide) {
            for (Map.Entry<UUID, HotbarMark> entry : this.hotbarMarks.entrySet()) {
                ServerPlayer player = this.findServerPlayer(entry.getKey());
                if (player != null) {
                    ModNetwork.sendToPlayer(player, SyncHotbarMarkPacket.clear());
                }
            }
            this.hotbarMarks.clear();
            for (CorruptionEffect effect : this.corruptionEffects) {
                if (!effect.player.isRemoved()) {
                    ModNetwork.sendToPlayer(effect.player, ScreenCorruptionPacket.stop());
                }
            }
            this.corruptionEffects.clear();
        }
        super.remove(reason);
    }

    @Override
    public void setHealth(float health) {
    }

    private void lockMaxHealthAttribute() {
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        if (maxHealth.getBaseValue() != FIXED_HEALTH) {
            maxHealth.setBaseValue(FIXED_HEALTH);
        }
        if (!maxHealth.getModifiers().isEmpty()) {
            maxHealth.removeModifiers();
        }
    }

    @Override
    public void die(DamageSource damageSource) {

    }

    @Override
    public boolean isDeadOrDying() {

        return false;
    }

    @Override
    public boolean isAlive() {

        return true;
    }

    @Override
    public void kill() {

    }

    public static boolean checkBioTortSpawnRules(
            EntityType<BioTortIncarnation> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {

        return level.getMaxLocalRawBrightness(pos) < 0;
    }
}
