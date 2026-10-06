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
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *  / Bio-Tort Incarnation
 *
 * <p> {@link YawningNya} AI
 *  Yawning_Nya </p>
 * <ul>
 *   <li><b></b>{@link #hurt}  {@link #doHurtTarget}
 *
 *       {@code HurtByTargetGoal} + {@code MeleeAttackGoal}
 *       {@code DATA_HAS_REVENGE} / 100 tick  1
 *       </li>
 *   <li><b></b>{@link #setHealth(float)}
 *       {@link #FIXED_HEALTH}1.20.1  {@code LivingEntity#getMaxHealth()}
 *       {@code final} {@code MAX_HEALTH}  {@code setMaxHealth()}
 *        {@link #lockMaxHealthAttribute()}  tick
 *       {@code MAX_HEALTH}  +
 *       /</li>
 *   <li><b></b>44 {@link #MAGIC_DAMAGE}
 *       + 46  YawningNekoAPI  API  MinimumDamageEventHandler
 *       {@code data/yawning_neko_api/minimum_damage/epca_entities.json}
 *       {@code "epca:bio_tort_incarnation": {"min_damage": 46}} </li>
 * </ul>
 *
 * <h3> {@link BioTortSkillConstants}</h3>
 * <ol>
 *   <li><b> 1</b> 20  3
 *        {@code SyncHotbarMarkPacket}
 *       ""<b></b>
 *        2  5  {@code yawning_neko_api:minimum}
 *        V
 *       {@code epca:needler} {@link BioTortMarkHandler} </li>
 *   <li><b> 2 2 + 4 </b> 14  2
 *        + NBT  {@link #DATA_PHASE_TWO} 4
 *        {@code ScreenCorruptionPacket}
 *        3  5 </li>
 * </ol>
 */
public class BioTortIncarnation extends PathfinderMob implements IParasite {

    /**  setHealth  */
    public static final float FIXED_HEALTH = 20.0F;

    /** 46  YawningNekoAPI  minimum_damage  */
    public static final float MAGIC_DAMAGE = 44.0F;

    private static final EntityDataAccessor<Boolean> DATA_HAS_REVENGE =
            SynchedEntityData.defineId(BioTortIncarnation.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_SHOWN_JOIN_MESSAGE =
            SynchedEntityData.defineId(BioTortIncarnation.class, EntityDataSerializers.BOOLEAN);

    /**
     *  2
     *  NBT/
     */
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO =
            SynchedEntityData.defineId(BioTortIncarnation.class, EntityDataSerializers.BOOLEAN);

    private UUID lastAttacker;
    private int revengeCooldown = 0;
    private int attackStrengthTicker;

    //   1 /  2

    /**  2 {@code PHASE2_ATTACK_THRESHOLD}  2 */
    private final Map<UUID, Integer> attackCounts = new HashMap<>();

    /**  2  4  */
    private final Map<UUID, Integer> phaseTwoAttackCounts = new HashMap<>();

    /** UUID ->  /  /  */
    private final Map<UUID, HotbarMark> hotbarMarks = new HashMap<>();

    /**  = "" */
    private final List<CorruptionEffect> corruptionEffects = new ArrayList<>();

    /**  tick */
    private int markCycleTicker;

    /**  */
    private static final class HotbarMark {
        /** 9  */
        private final int slotMask;
        /**  */
        private final long expireTick;
        /** "" */
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

    /**  */
    private static final class CorruptionEffect {
        /**  */
        private final ServerPlayer player;
        /**  3  null */
        @Nullable
        private final PlayerPositionHistory.Sample teleportTarget;
        /**  tick  */
        private int elapsedTicks;
        /**  */
        private boolean teleported;

        private CorruptionEffect(ServerPlayer player, @Nullable PlayerPositionHistory.Sample teleportTarget) {
            this.player = player;
            this.teleportTarget = teleportTarget;
        }
    }

    public BioTortIncarnation(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        //  Yawning_Nya
        this.navigation = new GroundPathNavigation(this, level);

        //  setHealth
        // getHealth()  FIXED_HEALTH/
        super.setHealth(FIXED_HEALTH);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;  //  Yawning_Nya
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HAS_REVENGE, false);
        this.entityData.define(DATA_HAS_SHOWN_JOIN_MESSAGE, false);
        this.entityData.define(DATA_PHASE_TWO, false);
    }

    //  Yawning_Nya  20 0.3 1.0 16
    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_SPEED, 4.0D)  //  Yawning_Nya
                .build();
    }

    // AI  Yawning_Nya
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        //  Yawning_Nya
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        // 1.20.1  LivingEntity#getMaxHealth()  final
        // MAX_HEALTH  1.20.1  setMaxHealth()
        //  tick  super.tick()
        //  tick // MAX_HEALTH
        if (!this.level().isClientSide) {
            this.lockMaxHealthAttribute();
        }

        //  1 2
        // isImmobile aiStep  super.tick()
        // / tick  tick
        //  AI
        if (!this.level().isClientSide) {
            this.tickBioTortSkills();
        }

        //  Yawning_Nya
        if (!this.level().isClientSide && !this.entityData.get(DATA_HAS_SHOWN_JOIN_MESSAGE)) {
            this.showJoinMessage();
            this.entityData.set(DATA_HAS_SHOWN_JOIN_MESSAGE, true);
        }

        //  Yawning_Nya
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
            //  Yawning_Nya
            Component joinMessage = Component.translatable("entity.epca.bio_tort_incarnation.join");

            serverLevel.getServer().getPlayerList().broadcastSystemMessage(joinMessage, false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return false;
        }

        //  HurtByTargetGoal  MeleeAttackGoal
        // doHurtTarget Yawning_Nya
        // 44 + 46
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity && attacker != this && revengeCooldown <= 0) {
            this.lastAttacker = attacker.getUUID();
            this.entityData.set(DATA_HAS_REVENGE, true);
            this.revengeCooldown = 100;  //  Yawning_Nya  100 tick

            // "" doHurtTarget
            if (attacker instanceof LivingEntity livingAttacker) {
                this.setLastHurtByMob(livingAttacker);
            }
        }

        // / setHealth
        boolean registered = super.hurt(source, amount);

        //  2 ""super.hurt  true
        //  super.hurt
        // ""
        if (registered && attacker instanceof ServerPlayer serverPlayer) {
            this.onPlayerAttack(serverPlayer);
        }

        return registered;
    }

    /**
     * 44
     *
     * <p>46  YawningNekoAPI
     * {@code MinimumDamageEventHandler} LivingAttackEvent
     * {@code data/yawning_neko_api/minimum_damage/epca_entities.json}
     * {@code "epca:bio_tort_incarnation": {"min_damage": 46}} </p>
     *
     * <p> {@code indirectMagic(this, this)}
     *  {@code damageSources().magic()}API  {@code source.getEntity()}
     *  46
     * {@code EvokerFangs#doDamage} </p>
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        //  Yawning_Nya
        this.playSound(SoundEvents.PLAYER_ATTACK_WEAK, 1.0F, 1.0F);

        boolean attacked = target.hurt(this.damageSources().indirectMagic(this, this), MAGIC_DAMAGE);

        if (attacked) {
            //  Yawning_Nya
            this.setLastHurtMob(target);
            this.attackStrengthTicker = 20;
        }

        return attacked;
    }

    //  Yawning_Nya
    private float getWeaponAttackDamage(ItemStack stack) {
        double attackDamage = stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE)
                .stream()
                .mapToDouble(AttributeModifier::getAmount)
                .sum();

        return (float) attackDamage;
    }

    //  Yawning_Nya
    public float getAttackDamage() {
        ItemStack mainHandItem = this.getMainHandItem();
        float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

        if (!mainHandItem.isEmpty()) {
            baseDamage += getWeaponAttackDamage(mainHandItem);
        }

        float cooldownPercent = this.getAttackStrengthScale(0.0F);
        return baseDamage * (0.2F + cooldownPercent * cooldownPercent * 0.8F);
    }

    //  Yawning_Nya
    public float getAttackStrengthScale(float adjustTicks) {
        float attackStrength = (float) this.attackStrengthTicker - adjustTicks;
        if (attackStrength < 0.0F) {
            attackStrength = 0.0F;
        }

        return attackStrength / 20.0F;
    }

    //  Yawning_Nya
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

        //  2  2  +
        // /" 13 "
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

    /**  -> NBTkey  UUID  */
    private static CompoundTag writeCountMap(Map<UUID, Integer> counts) {
        CompoundTag tag = new CompoundTag();
        counts.forEach((uuid, value) -> tag.putInt(uuid.toString(), value));
        return tag;
    }

    /** NBT ->  key  */
    private static void readCountMap(CompoundTag tag, Map<UUID, Integer> counts) {
        counts.clear();
        for (String key : tag.getAllKeys()) {
            try {
                counts.put(UUID.fromString(key), tag.getInt(key));
            } catch (IllegalArgumentException ignored) {
                //  UUID
            }
        }
    }

    //  Yawning_Nya
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

        //  Yawning_Nya
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

    //  Yawning_Nya
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

    //   1
    //   2 2 + 4
    //   BioTortSkillConstants

    /**  2 Jade  */
    public boolean isPhaseTwo() {
        return this.entityData.get(DATA_PHASE_TWO);
    }

    /**
     *  tick
     *
     * <p>
     * /"2 "" 3 "</p>
     */
    private void tickBioTortSkills() {
        long now = this.level().getGameTime();
        this.tickCorruptionEffects();
        this.tickHotbarMarks(now);
    }

    //   1

    private void tickHotbarMarks(long now) {
        // 1)  20  3
        if (++this.markCycleTicker >= BioTortSkillConstants.MARK_INTERVAL_TICKS) {
            this.markCycleTicker = 0;
            this.startNewMarkCycle(now);
            BioTortMarkHandler.pruneExpired(now);
        }

        // 2)  64
        //    ""
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

        // 3)  ->  2  5
        for (Map.Entry<UUID, HotbarMark> entry : this.hotbarMarks.entrySet()) {
            ServerPlayer player = this.findServerPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            HotbarMark mark = entry.getValue();
            int selected = player.getInventory().selected;
            // "" 2
            if (mark.isSlotMarked(selected) && now >= mark.nextDamageTick) {
                this.applyMinimumDamage(player, BioTortSkillConstants.SELECTED_SLOT_DAMAGE);
                mark.nextDamageTick = now + BioTortSkillConstants.SELECTED_SLOT_DAMAGE_INTERVAL_TICKS;
            }
        }
    }

    /**  */
    private void startNewMarkCycle(long now) {
        Map<UUID, HotbarMark> next = new HashMap<>();
        Set<UUID> markedPlayers = new HashSet<>();

        for (LivingEntity victim : this.collectMarkVictims()) {
            if (victim instanceof ServerPlayer player) {
                int mask = this.rollHotbarMask();
                next.put(player.getUUID(), new HotbarMark(mask, now + BioTortSkillConstants.MARK_DURATION_TICKS, now));
                markedPlayers.add(player.getUUID());
                // /
                ModNetwork.sendToPlayer(player, SyncHotbarMarkPacket.mark(mask,
                        BioTortSkillConstants.MARK_DURATION_TICKS));
            } else {
                //  BioTortMarkHandler
                BioTortMarkHandler.markCreature(victim.getUUID(), now,
                        BioTortSkillConstants.MARK_DURATION_TICKS);
            }
        }

        //  /  /
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

    /**  3 0..8 9  */
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

    /**
     *  =
     *
     * <p> Yawning_Nya  {@code HurtByTargetGoal}
     *  {@link #getTarget()} {@link #lastAttacker}{@code DATA_HAS_REVENGE}
     * ""
     *  {@link BioTortSkillConstants#VICTIM_RANGE} /</p>
     */
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

    /**
     * 5 "" YawningNekoAPI
     *
     * <p> API  {@link ModDamageTypes#MINIMUM}
     * {@code yawning_neko_api:minimum} {@link DamageSource}
     *  {@code target.hurt(source, amount)}API  {@code LivingEntityMixin}
     * {@code hurt}  HEAD  {@code amount}
     * //"
     * 5 "</p>
     *
     * <p> {@code epca_entities.json}  46
     * "" 46" 5 "
     *  +  mixin </p>
     */
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

    //   2

    /**
     *
     *
     * <p>14  ->  2 + NBT 2  4
     *
     * " 3 "</p>
     *
     * <p><b></b> 14  2
     * " 4 "<b></b> spec
     * " 2"</p>
     */
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

    /**  2 NBT */
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

    /**
     *
     *
     * <p>"5 "{@link PlayerPositionHistory}
     * 3  5
     *  {@code teleportTarget}  null</p>
     */
    private void startCorruptionEffect(ServerPlayer player) {
        long now = this.level().getGameTime();
        PlayerPositionHistory.Sample target = PlayerPositionHistory.sampleAt(player.getUUID(),
                now - BioTortSkillConstants.POSITION_LOOKBACK_TICKS);
        this.corruptionEffects.add(new CorruptionEffect(player, target));
        ModNetwork.sendToPlayer(player, ScreenCorruptionPacket.start(
                BioTortSkillConstants.CORRUPTION_TICKS, BioTortSkillConstants.CORRUPTION_DECAY_TICKS));
        //  isImmobile
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
    }

    /**  3  4 + 1.5  */
    private void tickCorruptionEffects() {
        if (this.corruptionEffects.isEmpty()) {
            return;
        }
        // AI  isImmobile
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

    /**  */
    private void teleportBack(ServerPlayer player, @Nullable PlayerPositionHistory.Sample target) {
        if (target == null) {
            return;
        }
        player.teleportTo(target.level, target.x, target.y, target.z, target.yRot, target.xRot);
    }

    /**
     * ""
     *
     * <p> {@link LivingEntity#aiStep()}  {@code isImmobile()}
     *  {@code jumping/xxa/zza} <b> {@code serverAiStep()}</b>
     * "AI "
     *  goal  disable  false{@code corruptionEffects}
     * </p>
     */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || !this.corruptionEffects.isEmpty();
    }

    /**
     *  /  / discard
     */
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


    /**
     *  {@link #FIXED_HEALTH}
     *
     * <p> {@link LivingEntity#hurt} / {@code actuallyHurt}
     * {@code setHealth(getHealth() - amount)}
     *  {@code hurt}
     *  {@code super.setHealth(...)}
     *  {@link #getHealth()}
     * {@link #FIXED_HEALTH} 0/NaN hurt </p>
     */
    @Override
    public void setHealth(float health) {
    }

    /**
     *
     *
     * <p>1.20.1  {@code LivingEntity#getMaxHealth()}  {@code public final}
     *  {@code LivingEntity.java:1655} {@code getAttributeValue(MAX_HEALTH)}
     *  1.20.1  {@code LivingEntity}  {@code setMaxHealth()}
     *  {@code Attributes.MAX_HEALTH}
     * {@code /attribute} </p>
     *
     * <p> tick {@code MAX_HEALTH}
     * {@link #FIXED_HEALTH}  {@code getMaxHealth()}
     * final  {@code getMaxHealth()}
     *  tick  1 tick
     * </p>
     */
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
        //  Yawning_Nya
        // YawningNekoAPI  MinimumDamageEventHandler / LivingEntityMixin
        //  0  target.die(source)
    }

    @Override
    public boolean isDeadOrDying() {
        //  Yawning_Nya
        return false;
    }

    @Override
    public boolean isAlive() {
        //  Yawning_Nya
        return true;
    }

    @Override
    public void kill() {
        //  Yawning_Nya /kill
    }

    public static boolean checkBioTortSpawnRules(
            EntityType<BioTortIncarnation> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        //  Yawning_Nya
        return level.getMaxLocalRawBrightness(pos) < 0;
    }
}

