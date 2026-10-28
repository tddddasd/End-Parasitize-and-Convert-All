package org.tdddd.epca.impl.overworld.registry.blocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.HiveNode;

/** Stores the hive node's biomass and keeps the block state's texture band in sync. */
public class HiveNodeBlockEntity extends BlockEntity
        implements org.tdddd.epca.impl.overworld.data.BiomassStorage {

    /** Hard cap on stored biomass. */
    public static final int MAX_BIOMASS_COUNT = 64;

    private static final String BIOMASS_COUNT_KEY = "biomassCount";

    private int biomassCount = 0;

    /** True when the parasite ecology created this node rather than a player placing it. */
    private boolean natural = false;

    /** One biomass every 45 s. */
    private static final int BIOMASS_INTERVAL_TICKS = 45 * 20;
    /** One curbub every 60 s (costs 1 biomass). */
    private static final int CURBUG_INTERVAL_TICKS = 60 * 20;
    /** How often captive growth is advanced. */
    private static final int GROWTH_CHECK_TICKS = 20;
    /** Hard-coded growth window for hive-node curbugs: 15..30 seconds. */
    private static final int GROWTH_MIN_TICKS = 15 * 20;
    private static final int GROWTH_MAX_TICKS = 30 * 20;
    private static final String GROWTH_KEY = "epca:hive_growth";
    /** Last game tick at which any hive node advanced this bug's growth; limits the speed-up to one step per second. */
    private static final String GROWTH_ACCEL_KEY = "epca:hive_growth_accel";
    private static final double CAPTIVE_RADIUS = 8.0D;

    /** Infestation sphere radius around the node. */
    private static final int INFEST_RADIUS = 8;
    /** Infestation speed: one random block every 20..30 seconds. */
    /** A ripper may take this much biomass (biomassCount) to grow into a new hive node. */
    private static final int RIPPER_GROWTH_COST = 32;
    /** How often a ripper is looked for, and how far away it may be. */
    private static final int RIPPER_CHECK_TICKS = 20;
    private static final double RIPPER_RADIUS = 8.0D;

    /** Nodes link to each other within this distance; three or more linked nodes form a structure. */
    private static final int STRUCTURE_LINK_DISTANCE = 24;
    private static final int STRUCTURE_MIN_NODES = 3;
    /** Erosion points for establishing a hive structure, and for each extra linked node. */
    private static final int STRUCTURE_ESTABLISH_POINTS = 200;
    private static final int STRUCTURE_EXTRA_NODE_POINTS = 40;
    private static final int STRUCTURE_CHECK_TICKS = 100;

    /** Feeders are attracted from 32 blocks and feed from 4 blocks away. */
    private static final double FEED_ATTRACT_RADIUS = 32.0D;
    private static final double FEED_RADIUS = 4.0D;
    /** Where a fed parasite is sent afterwards. */
    private static final double LEAVE_DISTANCE = 20.0D;
    private static final int FEED_CHECK_TICKS = 40;
    private static final String FED_KEY = "epca:hive_fed_at";

    /** Link is refreshed on nearby parasites this often, and lasts this long. */
    private static final int LINK_CHECK_TICKS = 100;
    private static final int LINK_DURATION_TICKS = 200;
    private static final double LINK_RADIUS = 16.0D;

    private static final int INFEST_MIN_TICKS = 20 * 20;
    private static final int INFEST_MAX_TICKS = 30 * 20;

    private int biomassTimer = 0;
    private int spawnTimer = 0;
    private int growthTimer = 0;
    private int infestTimer = 0;
    private int infestTarget = 0;
    private int ripperTimer = 0;
    private int linkTimer = 0;
    private int feedTimer = 0;
    private int structureTimer = 0;
    /**
     * The structure scan walks a 49x49x49 cube (117,649 block lookups), so it only runs every
     * {@link #STRUCTURE_SCAN_EVERY}th structure check. The flag it recomputes changes over minutes, so
     * the coarser cadence is not observable in game but removes about 90% of the scan cost.
     */
    private int structureScanTimer = 0;
    private static final int STRUCTURE_SCAN_EVERY = 10;
    private boolean inStructure = false;
    private int knownStructureSize = 0;
    /** True once this node has paid out the structure bonus, so it is only granted once. */
    private boolean structureRewarded = false;

    public HiveNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIVE_NODE.get(), pos, state);
    }

    @Override
    public int getMaxBiomassCount() {
        return MAX_BIOMASS_COUNT;
    }

    @Override
    public int getBiomassCount() {
        return biomassCount;
    }

    @Override
    public void setBiomassCount(int value) {
        int clamped = Mth.clamp(value, 0, MAX_BIOMASS_COUNT);
        if (clamped == this.biomassCount) {
            return;
        }
        this.biomassCount = clamped;
        this.setChanged();
        updateStage();
    }

    public void addBiomassCount(int amount) {
        setBiomassCount(this.biomassCount + amount);
    }

    /** True when the node can still take biomass. */
    public boolean hasRoom() {
        return this.biomassCount < MAX_BIOMASS_COUNT;
    }

    /** True when this node appeared from the parasite ecology instead of a player placing it. */
    public boolean isNatural() {
        return this.natural;
    }

    /** Marks this node as naturally created; the flag is persisted with the block entity. */
    public void setNatural(boolean natural) {
        if (this.natural == natural) {
            return;
        }
        this.natural = natural;
        this.setChanged();
    }

    private void updateStage() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        BlockState state = this.getBlockState();
        int stage = HiveNode.stageFor(this.biomassCount);
        if (state.hasProperty(HiveNode.STAGE) && state.getValue(HiveNode.STAGE) != stage) {
            this.level.setBlock(this.worldPosition, state.setValue(HiveNode.STAGE, stage), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.biomassCount = Mth.clamp(tag.getInt(BIOMASS_COUNT_KEY), 0, MAX_BIOMASS_COUNT);
        this.biomassTimer = tag.getInt("biomassTimer");
        this.infestTimer = tag.getInt("infestTimer");
        this.infestTarget = tag.getInt("infestTarget");
        this.ripperTimer = tag.getInt("ripperTimer");
        this.linkTimer = tag.getInt("linkTimer");
        this.feedTimer = tag.getInt("feedTimer");
        this.inStructure = tag.getBoolean("inStructure");
        this.structureRewarded = tag.getBoolean("structureRewarded");
        this.knownStructureSize = tag.getInt("knownStructureSize");
        this.spawnTimer = tag.getInt("spawnTimer");
        this.natural = tag.getBoolean("natural");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(BIOMASS_COUNT_KEY, this.biomassCount);
        tag.putInt("biomassTimer", this.biomassTimer);
        tag.putInt("infestTimer", this.infestTimer);
        tag.putInt("infestTarget", this.infestTarget);
        tag.putInt("ripperTimer", this.ripperTimer);
        tag.putInt("linkTimer", this.linkTimer);
        tag.putInt("feedTimer", this.feedTimer);
        tag.putBoolean("inStructure", this.inStructure);
        tag.putBoolean("structureRewarded", this.structureRewarded);
        tag.putInt("knownStructureSize", this.knownStructureSize);
        tag.putInt("spawnTimer", this.spawnTimer);
        tag.putBoolean("natural", this.natural);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HiveNodeBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        // 1) biomass: +1 every 45 s, capped at MAX_BIOMASS_COUNT
        if (++be.biomassTimer >= BIOMASS_INTERVAL_TICKS) {
            be.biomassTimer = 0;
            if (be.hasRoom()) {
                be.addBiomassCount(1);
            }
        }
        // 2) one curbub every 60 s for 1 biomass
        if (++be.spawnTimer >= CURBUG_INTERVAL_TICKS) {
            be.spawnTimer = 0;
            if (be.biomassCount >= 1 && level instanceof net.minecraft.server.level.ServerLevel server) {
                if (be.spawnCurbug(server)) {
                    be.setBiomassCount(be.biomassCount - 1);
                }
            }
        }
        // 2.5) a ripper that touches a stocked node absorbs 32 biomass and becomes a new node
        if (++be.ripperTimer >= RIPPER_CHECK_TICKS) {
            be.ripperTimer = 0;
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                be.absorbRipper(server);
            }
        }
        // 2.6) Link: parasites near a node tend to patrol around it
        if (++be.linkTimer >= LINK_CHECK_TICKS) {
            be.linkTimer = 0;
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                be.applyLink(server);
            }
        }
        // 2.7) feeders: parasites are drawn in from 32 blocks; once fed they leave the colony
        if (++be.feedTimer >= FEED_CHECK_TICKS) {
            be.feedTimer = 0;
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                be.attractFeeders(server);
            }
        }
        // 2.8) hive structure: three or more nodes linked within 24 blocks
        if (++be.structureTimer >= STRUCTURE_CHECK_TICKS) {
            be.structureTimer = 0;
            if (++be.structureScanTimer >= STRUCTURE_SCAN_EVERY) {
                be.structureScanTimer = 0;
                if (level instanceof net.minecraft.server.level.ServerLevel server) {
                    be.updateStructure(server);
                }
            }
        }
        // 3) infest the surrounding sphere; which blocks are soft enough depends on the erosion stage
        if (++be.infestTimer >= (be.infestTarget <= 0 ? INFEST_MIN_TICKS : be.infestTarget)) {
            be.infestTimer = 0;
            be.infestTarget = INFEST_MIN_TICKS
                    + level.random.nextInt(INFEST_MAX_TICKS - INFEST_MIN_TICKS + 1);
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                be.infestNearby(server);
            }
        }
        // 4) advance the hard-coded growth of the curbugs this node spawned
        if (++be.growthTimer >= GROWTH_CHECK_TICKS) {
            be.growthTimer = 0;
            be.advanceCaptiveGrowth();
        }
    }

    private boolean spawnCurbug(net.minecraft.server.level.ServerLevel level) {
        net.minecraft.world.entity.Mob mob = org.tdddd.epca.impl.overworld.registry.ModEntities.CURBUG.get().create(level);
        if (mob == null) {
            return false;
        }
        double x = this.worldPosition.getX() + 0.5D;
        double y = this.worldPosition.getY() + 1.0D;
        double z = this.worldPosition.getZ() + 0.5D;
        mob.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        // Hard-coded: a hive-node curbub matures into a ripper after 15..30 seconds.
        mob.getPersistentData().putInt(GROWTH_KEY,
                GROWTH_MIN_TICKS + level.random.nextInt(GROWTH_MAX_TICKS - GROWTH_MIN_TICKS + 1));
        level.addFreshEntity(mob);
        spawnBiomassParticles(level);
        return true;
    }

    /** True once this node is part of a hive structure (three or more linked nodes). */
    public boolean isInStructure() {
        return this.inStructure;
    }

    /**
     * Rebuilds the structure flag: nodes within 24 blocks of each other are linked, and a connected
     * group of at least three nodes is a hive structure.
     */
    private void updateStructure(net.minecraft.server.level.ServerLevel level) {
        java.util.List<BlockPos> nodes = new java.util.ArrayList<>();
        int search = STRUCTURE_LINK_DISTANCE;
        for (BlockPos pos : BlockPos.betweenClosed(
                this.worldPosition.offset(-search, -search, -search),
                this.worldPosition.offset(search, search, search))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(org.tdddd.epca.impl.overworld.registry.ModBlocks.HIVE_NODE.get())) {
                nodes.add(pos.immutable());
            }
        }
        // flood fill from this node over the 24 block links
        java.util.Set<BlockPos> reached = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        reached.add(this.worldPosition);
        queue.add(this.worldPosition);
        while (!queue.isEmpty() && reached.size() < 128) {
            BlockPos current = queue.poll();
            for (BlockPos other : nodes) {
                if (reached.contains(other)) {
                    continue;
                }
                if (Math.abs(other.getX() - current.getX()) <= search
                        && Math.abs(other.getY() - current.getY()) <= search
                        && Math.abs(other.getZ() - current.getZ()) <= search
                        && current.distSqr(other) <= (double) (search * search)) {
                    reached.add(other);
                    queue.add(other);
                }
            }
        }
        boolean now = reached.size() >= STRUCTURE_MIN_NODES;
        // A second hive structure may not appear within eight chunks of an existing one. Nodes of
        // this group are excluded, because they are the structure that is being detected.
        if (now && !this.inStructure
                && org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeRules.hasHiveStructureWithinChunks(
                        level, this.worldPosition,
                        org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeRules.STRUCTURE_EXCLUSION_CHUNKS,
                        reached)) {
            now = false;
        }
        if (now != this.inStructure) {
            this.inStructure = now;
            this.setChanged();
            // Erosion points: +200 for establishing a structure, +40 for every extra linked node.
            var manager = org.tdddd.epca.impl.overworld.data.EvolutionManager.forDimension(level);
            if (manager != null) {
                if (now) {
                    if (!this.structureRewarded) {
                        this.structureRewarded = true;
                        manager.addPoints(STRUCTURE_ESTABLISH_POINTS);
                    }
                    this.knownStructureSize = reached.size();
                } else {
                    this.knownStructureSize = 0;
                }
            }
        } else if (now && reached.size() > this.knownStructureSize) {
            int grown = reached.size() - this.knownStructureSize;
            this.knownStructureSize = reached.size();
            var manager = org.tdddd.epca.impl.overworld.data.EvolutionManager.forDimension(level);
            if (manager != null) {
                manager.addPoints(STRUCTURE_EXTRA_NODE_POINTS * grown);
            }
        }
    }

    /**
     * Draws parasites in from 32 blocks and lets them feed the node.
     *
     * <p>A parasite that reaches the node hands over 1 biomass and is then sent away: everything except
     * the curbugs this node spawned itself walks off to {@link #LEAVE_DISTANCE} blocks and resumes its
     * normal wandering there.</p>
     */
    private void attractFeeders(net.minecraft.server.level.ServerLevel level) {
        net.minecraft.world.phys.AABB area =
                new net.minecraft.world.phys.AABB(this.worldPosition).inflate(FEED_ATTRACT_RADIUS);
        for (net.minecraft.world.entity.Mob mob
                : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area)) {
            if (!mob.isAlive()) {
                continue;                       // players are not Mob, so this only sees creatures
            }
            // Only parasites that carry biomass of their own bother approaching a hive node.
            int ownBiomass = org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager.getCurrentBiomassCount(mob);
            if (ownBiomass <= 0) {
                continue;
            }
            double distance = Math.sqrt(mob.blockPosition().distSqr(this.worldPosition));
            if (distance <= FEED_RADIUS) {
                // feed once per arrival
                if (mob.getPersistentData().getLong(FED_KEY) != 0L) {
                    continue;
                }
                mob.getPersistentData().putLong(FED_KEY, level.getGameTime());
                mob.getPersistentData().remove(GROWTH_KEY);        // a fed bug is no longer a node child
                // Feeding transfers the feeder's own biomass: a parasite with no kill count gives nothing.
                if (ownBiomass > 0 && this.hasRoom()) {
                    org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager.setBiomassCount(mob, ownBiomass - 1);
                    this.addBiomassCount(1);
                    spawnBiomassParticles(level);
                }
                // leave the colony: walk to a point ~20 blocks away
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                double tx = this.worldPosition.getX() + 0.5D + Math.cos(angle) * LEAVE_DISTANCE;
                double tz = this.worldPosition.getZ() + 0.5D + Math.sin(angle) * LEAVE_DISTANCE;
                mob.getNavigation().moveTo(tx, this.worldPosition.getY(), tz, 1.2D);
            } else if (distance <= FEED_ATTRACT_RADIUS) {
                // attracted: approach the node
                mob.getNavigation().moveTo(this.worldPosition.getX() + 0.5D, this.worldPosition.getY(),
                        this.worldPosition.getZ() + 0.5D, 1.0D);
            }
        }
    }

    /** Refreshes the Link effect on parasites within 16 blocks. */
    private void applyLink(net.minecraft.server.level.ServerLevel level) {
        net.minecraft.world.phys.AABB area =
                new net.minecraft.world.phys.AABB(this.worldPosition).inflate(LINK_RADIUS);
        for (net.minecraft.world.entity.LivingEntity living
                : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area)) {
            if (living instanceof net.minecraft.world.entity.player.Player) {
                continue;
            }
            // Only what this node spawned, or nearby onesent parasites, are linked.
            boolean nodeBorn = living.getPersistentData().contains(GROWTH_KEY);
            boolean onesent = living instanceof org.tdddd.epca.impl.overworld.registry.entities.IOnesent;
            if (!nodeBorn && !onesent) {
                continue;
            }
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    org.tdddd.epca.impl.overworld.registry.ModEffects.LINK.get(),
                    LINK_DURATION_TICKS, 0, true, false));
        }
    }

    /**
     * A nearby ripper drinks 32 biomass out of this node and turns itself into a new hive node.
     *
     * <p>This is absorption, not a donation: the node must hold at least 32, the amount is taken from
     * it, and the ripper is replaced by a fresh node that starts empty.</p>
     */
    private void absorbRipper(net.minecraft.server.level.ServerLevel level) {
        if (this.biomassCount < RIPPER_GROWTH_COST) {
            return;
        }
        net.minecraft.world.phys.AABB area =
                new net.minecraft.world.phys.AABB(this.worldPosition).inflate(RIPPER_RADIUS);
        for (org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper ripper
                : level.getEntitiesOfClass(org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper.class, area)) {
            if (!ripper.isAlive()) {
                continue;
            }
            BlockPos target = ripper.blockPosition();
            if (!org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeRules
                    .canCreateNaturalNode(level, target)) {
                return;                             // this chunk already holds its natural-node quota
            }
            this.setBiomassCount(this.biomassCount - RIPPER_GROWTH_COST);
            ripper.discard();
            level.setBlock(target, org.tdddd.epca.impl.overworld.registry.ModBlocks.HIVE_NODE.get().defaultBlockState(), 3);
            org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeRules.markNatural(level, target);
            spawnBiomassParticles(level);
            return;
        }
    }

    /**
     * Infests the surroundings gradually: the 8 block sphere is the MAXIMUM reach, and each pass
     * converts exactly one randomly chosen block inside it. Blocks are converted every 20..30 seconds
     * (a new random delay after each success), so the colony grows slowly and irregularly rather than
     * in visible shells.
     *
     * <p>Which blocks are soft enough depends on the erosion stage: stage II allows harder blocks than
     * stage I, and both limits live in BlockConversionManager's own configs.</p>
     */
    private void infestNearby(net.minecraft.server.level.ServerLevel level) {
        int stage = org.tdddd.epca.impl.overworld.data.EvolutionManager.getStageForDimension(level);
        org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager manager =
                org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager.getInstance();
        for (int attempt = 0; attempt < 12; attempt++) {
            double dx = (level.random.nextDouble() * 2.0D - 1.0D) * INFEST_RADIUS;
            double dy = (level.random.nextDouble() * 2.0D - 1.0D) * INFEST_RADIUS;
            double dz = (level.random.nextDouble() * 2.0D - 1.0D) * INFEST_RADIUS;
            if (dx * dx + dy * dy + dz * dz > (double) (INFEST_RADIUS * INFEST_RADIUS)) {
                continue;                               // outside the maximum sphere, retry
            }
            BlockPos target = this.worldPosition.offset((int) dx, (int) dy, (int) dz);
            if (target.equals(this.worldPosition) || !level.isLoaded(target)) {
                continue;
            }
            BlockState state = level.getBlockState(target);
            if (state.isAir() || state.getDestroySpeed(level, target) < 0.0F) {
                continue;
            }
            // Only spread from block to block: one of the six neighbours must already be infested.
            if (!hasInfestedNeighbour(level, target)) {
                continue;
            }
            boolean changed = stage >= 2
                    ? manager.convertBlockUsingStageIIConfig(level, target, state)
                    : manager.convertBlockUsingStageIConfig(level, target, state);
            if (changed) {
                return;                                 // one block per pass
            }
        }
    }

    /** True when any of the six direct neighbours is an infested (parasite) block. */
    private static boolean hasInfestedNeighbour(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            if (neighbour.getBlock() instanceof org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface) {
                return true;
            }
        }
        return false;
    }

    /** 2..3 biomass particles accompany a spawned curbub. */
    private void spawnBiomassParticles(net.minecraft.server.level.ServerLevel level) {
        int count = 2 + level.random.nextInt(2);
        level.sendParticles(org.tdddd.epca.impl.overworld.registry.ModParticles.BIOMASS.get(),
                this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D,
                count, 0.25D, 0.25D, 0.25D, 0.0D);
    }

    private void advanceCaptiveGrowth() {
        if (!(this.level instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(this.worldPosition).inflate(CAPTIVE_RADIUS);
        long now = level.getGameTime();
        for (org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Curbug bug
                : level.getEntitiesOfClass(org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Curbug.class, area)) {
            CompoundTag data = bug.getPersistentData();
            if (!data.contains(GROWTH_KEY)) {
                continue;
            }
            // Several nodes can cover the same bug. Only the first one in each growth step may
            // advance it, so overlapping nodes do not stack their acceleration.
            if (now - data.getLong(GROWTH_ACCEL_KEY) < GROWTH_CHECK_TICKS) {
                continue;
            }
            data.putLong(GROWTH_ACCEL_KEY, now);
            int left = data.getInt(GROWTH_KEY) - GROWTH_CHECK_TICKS;
            if (left > 0) {
                data.putInt(GROWTH_KEY, left);
                continue;
            }
            // mature into a ripper, in place
            double x = bug.getX();
            double y = bug.getY();
            double z = bug.getZ();
            float yaw = bug.getYRot();
            float pitch = bug.getXRot();
            SoundSource source = bug.getSoundSource();
            bug.discard();
            net.minecraft.world.entity.Mob ripper =
                    org.tdddd.epca.impl.overworld.registry.ModEntities.RIPPER.get().create(level);
            if (ripper != null) {
                ripper.moveTo(x, y, z, yaw, pitch);
                level.addFreshEntity(ripper);
            }
            // The same feedback as a natural curbub-to-ripper transformation, plus the explosion
            // particle burst used when two creatures fuse.
            level.playSound(null, x, y, z,
                    org.tdddd.epca.impl.overworld.registry.ModSoundEvents.CURBUG_EVOLVE.get(),
                    source, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.EXPLOSION, x, y + 1.0D, z, 3, 1.0D, 1.0D, 1.0D, 0.1D);
        }
    }
}
